plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.cycling.sonic"
    // material3 1.5.0-alpha29 与 Compose 1.13.0-alpha01 的 AAR 要求 minCompileSdk = 37。
    // 显式带上 minor：37 只有带点目录（android-37.0/37.1/37.2），
    // 写全版本号才能保证解析到已安装的平台。
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.cycling.sonic"
        minSdk = 30
        targetSdk = 36
        versionCode = 4
        versionName = "1.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            // 签名材料按这个优先级解析：
            //   1. 环境变量（CI：SONIC_STORE_FILE / _PASSWORD / _KEY_ALIAS / _KEY_PASSWORD）
            //   2. 仓库根的 key.properties（本地开发）
            // 两者都没有时这里留空，由 buildTypes.release 回落到 debug 签名 —— 否则
            // validateSigningRelease 会直接失败，连本地打一次包都做不到。
            val envStoreFile = System.getenv("SONIC_STORE_FILE")
            val propsFile = rootProject.file("key.properties")
            val props = mutableMapOf<String, String>()
            if (propsFile.exists()) {
                propsFile.readLines().forEach { raw ->
                    val line = raw.trim()
                    if (line.isNotEmpty() && !line.startsWith("#") && line.contains("=")) {
                        props[line.substringBefore("=").trim()] = line.substringAfter("=").trim()
                    }
                }
            }

            val storePath: String? = envStoreFile ?: props["storeFile"]
            if (storePath != null) {
                // key.properties 里的路径相对 :app（仓库根的 keystore 写作 ../sonic-release.keystore）
                storeFile = file(storePath)
                storePassword = System.getenv("SONIC_STORE_PASSWORD") ?: props["storePassword"] ?: ""
                keyAlias = System.getenv("SONIC_KEY_ALIAS") ?: props["keyAlias"] ?: ""
                keyPassword = System.getenv("SONIC_KEY_PASSWORD") ?: props["keyPassword"] ?: ""
            }
        }
    }

    buildTypes {
        release {
            // R8 全量优化：dex 体积与类数量的下降直接换来更短的类加载/验证时间，
            // 也就是冷启动里"代码不慢、装载慢"的那一段。压缩规则见 proguard-rules.pro。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 没有 release 密钥时用 debug 签名，保证本地能出包；但会留一条 warning，
            // 免得把 debug 签名的产物当成正式包发出去。
            val releaseSigning = signingConfigs.getByName("release")
            signingConfig = if (releaseSigning.storeFile != null) {
                releaseSigning
            } else {
                logger.warn(
                    "Sonic: 未找到 release 签名（环境变量或仓库根 key.properties），" +
                        "本次 release 构建使用 debug 签名，不能用于发布。"
                )
                signingConfigs.getByName("debug")
            }
        }
        // 宏基准（:benchmark）的目标变体：产物形态与 release 一致（不可调试、**混淆**），
        // 但用 debug 签名，这样 connectedBenchmarkAndroidTest 不依赖 release keystore 就能装。
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            // 其余模块没有 benchmark 变体，依赖解析回落到 release。
            matchingFallbacks += listOf("release")
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
        buildConfig = true
    }
}

dependencies {
    implementation(project(":presentation"))
    implementation(project(":domain"))
    implementation(project(":data"))
    
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.hilt.android)
    ksp(libs.androidx.hilt.compiler)
    
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Navigation 3
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigation3.runtime)

    implementation(libs.androidx.hilt.navigation.compose)

    //timber
    implementation(libs.timber)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
