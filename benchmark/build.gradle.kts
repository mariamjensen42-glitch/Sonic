plugins {
    // com.android.test：这个模块不含产品代码，只有驱动 :app 的 instrumented 测试。
    // Kotlin 编译由 AGP 9 的内置 Kotlin 承担，和 :app / :data / :presentation 一样不需要
    // 再 apply org.jetbrains.kotlin.android。
    alias(libs.plugins.android.test)
}

android {
    namespace = "com.cycling.benchmark"
    // 与 :app 同一平台版本：benchmark 模块要链接被测 app 的清单与资源。
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        // 被测 app 的 minSdk 是 30，测试 APK 只能跑在同一批设备上。
        minSdk = 30
        targetSdk = 36

        // AndroidBenchmarkRunner 负责在测量前后做编译、锁频、关闭覆盖率等准备。
        testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
    }

    targetProjectPath = ":app"

    // com.android.test 模块按「同名变体」和被测工程配对，不会自动继承 :app 的构建类型：
    // 没有这个 build type 就只有 debug/release 变体，connectedBenchmarkAndroidTest 不存在。
    buildTypes {
        create("benchmark") {
            // 测试 APK 也要签名才能装：非 debug 构建类型默认没有签名配置。
            signingConfig = signingConfigs.getByName("debug")
            // benchmarkTestedApks 解析 :app → :presentation/:domain/:data 时，消费侧要自己
            // 声明回落，否则这几个库（只有 debug/release）匹配不上 benchmark 变体。
            matchingFallbacks += listOf("release")
        }
    }

    // 宏基准会在迭代之间反复杀掉被测进程，所以 instrumentation 不能挂在 app 进程里：
    // 打开 self-instrumenting 后测试 APK 自己就是 targetPackage，app 只由 shell 命令驱动。
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    // 宏基准本身（MacrobenchmarkRule / StartupTimingMetric / FrameTimingMetric / CompilationMode）
    implementation(libs.androidx.benchmark.macro.junit4)
    // AndroidBenchmarkRunner（清单里那个 testInstrumentationRunner）住在 benchmark-junit4 里。
    // benchmark 1.5.0 的 benchmark-macro-junit4 只把它写成版本约束、不传递依赖，少了它测试 APK
    // 里就没有 runner 类，instrumentation 起不来（ClassNotFoundException）。
    implementation(libs.androidx.benchmark.junit4)
    // @RunWith(AndroidJUnit4::class)
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.runner)
    // UiAutomator：宏基准只能通过无障碍节点操作 Compose 界面
    implementation(libs.androidx.test.uiautomator)
}
