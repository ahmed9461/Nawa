#!/usr/bin/env python3
"""Inspect the actual APK rather than inferring packaging from source settings."""
import argparse
import hashlib
import struct
import subprocess
import zipfile
from pathlib import Path

p = argparse.ArgumentParser()
p.add_argument("apk", type=Path)
p.add_argument("--aapt2", required=True)
p.add_argument("--zipalign", required=True)
a = p.parse_args()
with zipfile.ZipFile(a.apk) as z:
    libraries = [n for n in z.namelist() if n.startswith("lib/") and n.endswith(".so")]
    assert libraries, "APK has no native libraries"
    assert {n.split('/')[1] for n in libraries} == {"arm64-v8a"}, libraries
    assert "lib/arm64-v8a/libnawa-engine.so" in libraries
    for name in libraries:
        elf = z.read(name)
        assert elf[:4] == b'\x7fELF' and elf[4] == 2 and elf[5] == 1, name
        offset = struct.unpack_from('<Q', elf, 32)[0]
        entry_size, count = struct.unpack_from('<HH', elf, 54)
        for i in range(count):
            at = offset + i * entry_size
            if struct.unpack_from('<I', elf, at)[0] == 1:
                alignment = struct.unpack_from('<Q', elf, at + 48)[0]
                assert alignment >= 16384, (name, 'Not 16KB-page compatible', alignment)
        print(name, len(elf), 'bytes; ELF alignment >= 16KB')
permissions = subprocess.check_output([a.aapt2, 'dump', 'permissions', str(a.apk)], text=True)
assert 'android.permission.INTERNET' not in permissions, permissions
badging = subprocess.check_output([a.aapt2, 'dump', 'badging', str(a.apk)], text=True)
assert "name='com.ahmed9461.nawa.stable'" in badging
assert "sdkVersion:'33'" in badging
subprocess.run([a.zipalign, '-c', '-P', '16', '4', str(a.apk)], check=True)
print(badging)
print(permissions)
print('APK size:', a.apk.stat().st_size)
print('SHA256:', hashlib.sha256(a.apk.read_bytes()).hexdigest())
print('APK packaging checks PASSED. This is not an Android runtime test.')
