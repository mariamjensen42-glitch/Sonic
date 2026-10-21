plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.cycling.salt.audiotag"
    // material3 1.5.0-alpha29 与 Compose 1.13.0-alpha01 的 AAR 要求 minCompileSdk = 37，
    // 本模块不依赖 Compose，但同工程约定使用同一 compileSdk，避免产物元数据混用。
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        minSdk = 30
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

// 这是从本地 SaltAudioTag 工程的 `core` 模块整包搬进来的多格式标签库（LGPL-2.1，见本目录
// LICENSE 与每个源文件头）。它读写 MP3 / FLAC / OGG-Vorbis / MP4(M4A) 的标签，Sonic 的
// `:data` 通过 `AudioMetadataHelper` 使用它，补齐「标签编辑只支持 MP3」的短板。
dependencies {
    api(libs.kotlinx.io.core)
    api(libs.kotlinx.io.bytestring)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test.junit)
}
