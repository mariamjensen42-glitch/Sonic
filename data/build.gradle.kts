plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cycling.data"
    // material3 1.5.0-alpha29 与 Compose 1.13.0-alpha01 的 AAR 要求 minCompileSdk = 37。
    // 显式带上 minor：37 只有带点目录（android-37.0/37.1/37.2），
    // 写全版本号才能保证解析到已安装的平台。
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        minSdk = 30

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

// Exported Room schemas are checked in: the 1→2 migration is the first upgrade path that has to
// preserve user data, so the generated JSON is the reference the migration SQL is verified against.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.hilt.android)
    ksp(libs.androidx.hilt.compiler)
    
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.datasource)

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)

    // 在线音源：内嵌 QuickJS 执行用户导入的 LX 音源脚本（AAR 自带 libquickjs.so）。
    // 它传递的 kotlin-stdlib 会顶到 2.4，而 Hilt 只认到 metadata 2.3 —— 版本统一钉回
    // 项目 Kotlin 的那一步在根 build.gradle.kts 的 resolutionStrategy 里。
    implementation(libs.quickjs.android)

    // 多格式标签读写（MP3/FLAC/OGG/MP4/WAV/AIFF/WMA/DSF）。字段名走 FieldKey，
    // 不再为每个容器各写一套 reader / writer。
    implementation(libs.jaudiotagger)

    implementation(libs.timber)

    implementation(libs.coil.network.okhttp)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
