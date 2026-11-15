#!/usr/bin/env python3
"""Verify actual binary manifest, native ELF alignment and ZIP alignment, fail closed."""
import argparse
import hashlib
import struct
import subprocess
import zipfile
from pathlib import Path


def require(condition, message):
    if not condition:
        raise RuntimeError(str(message))


def manifest_nodes(data):
    """Read typed AXML attributes without depending on aapt's human-readable spacing."""
    require(len(data) >= 8 and struct.unpack_from('<H', data)[0] == 3, 'Not binary Android XML')
    strings = []
    offset = struct.unpack_from('<H', data, 2)[0]

    def string(index):
        return strings[index] if 0 <= index < len(strings) else None

    def length8(at):
        value = data[at]
        return (((value & 127) << 8) | data[at + 1], 2) if value & 128 else (value, 1)

    def length16(at):
        value = struct.unpack_from('<H', data, at)[0]
        return (((value & 32767) << 16) | struct.unpack_from('<H', data, at + 2)[0], 4) if value & 32768 else (value, 2)

    while offset < len(data):
        kind, header, size = struct.unpack_from('<HHI', data, offset)
        require(size >= header >= 8 and offset + size <= len(data), 'Invalid XML chunk')
        if kind == 1:
            count, _, flags, start, _ = struct.unpack_from('<IIIII', data, offset + 8)
            require(header + 4 * count <= size, 'Invalid string pool')
            for i in range(count):
                at = offset + start + struct.unpack_from('<I', data, offset + header + 4 * i)[0]
                if flags & 256:
                    _, width = length8(at)
                    at += width
                    length, width = length8(at)
                    at += width
                    strings.append(data[at:at + length].decode('utf-8'))
                else:
                    length, width = length16(at)
                    at += width
                    strings.append(data[at:at + 2 * length].decode('utf-16-le'))
        elif kind == 0x102:
            name = string(struct.unpack_from('<I', data, offset + 20)[0])
            start, stride, count = struct.unpack_from('<HHH', data, offset + 24)
            require(stride >= 20 and 16 + start + stride * count <= size, 'Invalid attributes')
            attributes = {}
            for i in range(count):
                at = offset + 16 + start + i * stride
                _, key, raw = struct.unpack_from('<III', data, at)
                value_type = data[at + 15]
                value = struct.unpack_from('<I', data, at + 16)[0]
                attributes[string(key)] = string(raw) if raw != 0xffffffff else string(value) if value_type == 3 else value
            yield name, attributes
        offset += size


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('apk', type=Path)
    parser.add_argument('--aapt2', required=True)
    parser.add_argument('--zipalign', required=True)
    parser.add_argument('--expected-min-sdk', type=int, default=33)
    args = parser.parse_args()
    with zipfile.ZipFile(args.apk) as archive:
        nodes = list(manifest_nodes(archive.read('AndroidManifest.xml')))
        root = next(attrs for name, attrs in nodes if name == 'manifest')
        sdk = next(attrs for name, attrs in nodes if name == 'uses-sdk')
        app = next(attrs for name, attrs in nodes if name == 'application')
        permissions = [attrs['name'] for name, attrs in nodes if name.startswith('uses-permission')]
        require(root.get('package') == 'com.ahmed9461.nawa.stable', root)
        require(int(root.get('versionCode', 0)) >= 2, root)
        require(int(sdk.get('minSdkVersion', 0)) == args.expected_min_sdk, sdk)
        require(int(sdk.get('targetSdkVersion', 0)) == 36, sdk)
        require(not app.get('debuggable', 0), 'Release APK is debuggable')
        require(not app.get('allowBackup', 1), 'OS backup is enabled')
        require(not app.get('usesCleartextTraffic', 1), 'Cleartext traffic is enabled')
        require('android.permission.INTERNET' not in permissions, permissions)
        print('Binary manifest:', root, sdk, 'permissions=', permissions, flush=True)
        libraries = [n for n in archive.namelist() if n.startswith('lib/') and n.endswith('.so')]
        require(libraries, 'APK has no native libraries')
        require({n.split('/')[1] for n in libraries} == {'arm64-v8a'}, libraries)
        require('lib/arm64-v8a/libnawa-engine.so' in libraries, 'Inference library is absent')
        for name in libraries:
            elf = archive.read(name)
            require(elf[:4] == b'\x7fELF' and elf[4] == 2 and elf[5] == 1, name)
            offset = struct.unpack_from('<Q', elf, 32)[0]
            entry_size, count = struct.unpack_from('<HH', elf, 54)
            for i in range(count):
                at = offset + i * entry_size
                if struct.unpack_from('<I', elf, at)[0] == 1:
                    alignment = struct.unpack_from('<Q', elf, at + 48)[0]
                    require(alignment >= 16384, (name, 'Not 16KB-page compatible', alignment))
            print(name, len(elf), 'bytes; ELF alignment >= 16KB', flush=True)
    print(subprocess.check_output([args.aapt2, 'dump', 'badging', str(args.apk)], text=True))
    subprocess.run([args.zipalign, '-c', '-P', '16', '4', str(args.apk)], check=True)
    print('APK size:', args.apk.stat().st_size)
    print('SHA256:', hashlib.sha256(args.apk.read_bytes()).hexdigest())
    print('APK packaging checks PASSED. This is not an Android runtime test.')


if __name__ == '__main__':
    main()
