plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.jetbrains.kotlin.compose)
}
val signingStore = providers.environmentVariable("NAWA_SIGNING_STORE").orNull
android {
    namespace = "com.ahmed9461.nawa"
    compileSdk = 36
    defaultConfig {
        // The old package used nonpersistent debug keys. Keep it installed alongside this channel.
        applicationId = "com.ahmed9461.nawa.stable"
        minSdk = 33
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0"
        ndk { abiFilters += "arm64-v8a" }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }
    if (!signingStore.isNullOrBlank()) {
        signingConfigs.create("personal") {
            storeFile = file(signingStore)
            storePassword = providers.environmentVariable("NAWA_SIGNING_PASSWORD").get()
            keyAlias = providers.environmentVariable("NAWA_SIGNING_ALIAS").get()
            keyPassword = providers.environmentVariable("NAWA_SIGNING_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (signingStore.isNullOrBlank()) null else signingConfigs.getByName("personal")
            ndk { debugSymbolLevel = "none" }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    lint { abortOnError = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
dependencies {
    implementation(project(":engine"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.icons.core)
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.17")
}
