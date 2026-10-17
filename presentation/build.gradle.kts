plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.cycling.presentation"
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
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":domain"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    implementation(libs.kotlinx.serialization.core)

    implementation(libs.androidx.hilt.android)
    ksp(libs.androidx.hilt.compiler)
    
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Hilt
    implementation(libs.androidx.hilt.navigation.compose)

    // Navigation 3
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // 桌面小组件。Glance 是独立于 Compose UI 的远端 UI 描述，只能复用主题色，
    // 不能复用 core/ui 里的 Composable，因此不违反“页面只用 M3 组件”的约束。
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    
    implementation(libs.androidx.palette)

    implementation(libs.compose.markdown)

    implementation(libs.timber)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
