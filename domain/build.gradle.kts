plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.cycling.domain"
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

dependencies {
    implementation(libs.androidx.core.ktx)
    
    implementation(libs.androidx.hilt.android)
    ksp(libs.androidx.hilt.compiler)
    
    implementation(libs.kotlinx.coroutines.core)
    
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
