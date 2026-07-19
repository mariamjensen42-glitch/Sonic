# Sonic — AGENTS.md

现代化 Android 音乐播放器，Kotlin + Jetpack Compose，Clean Architecture + MVI，AI 驱动的音乐洞察。

> **⚠️ 每轮对话开始时，必须先完整阅读本文件（AGENTS.md），再开始任何工作。** 本文件定义了项目的技术栈、代码风格、项目结构与强制规则，是所有开发决策的依据。

## Project

- **语言**：Kotlin 2.3.0（强制，禁止 Java/其他 JVM 语言）
- **UI**：Jetpack Compose（BOM `2026.01.00`）+ Material 3
- **架构**：Clean Architecture（domain/data/presentation 三层）+ MVI（Intent/State/Effect 单向数据流）
- **DI**：Hilt 2.59.2 + KSP 2.3.8
- **本地存储**：Room 2.7.1、DataStore Preferences
- **播放**：Media3 ExoPlayer 1.9.0 + MediaSessionService（前台服务后台播放）
- **图片**：Coil 3.1.0
- **导航**：Navigation 3（`androidx.navigation3`）+ `@Serializable` 类型安全路由
- **其他**：kotlinx-coroutines/serialization、OkHttp、Timber（日志）、mp3agic（音频元数据）、palette-ktx（取色）
- **JDK**：17（`jvmToolchain(17)`）；**compileSdk/targetSdk**：36；**minSdk**：30
- **AGP**：9.3.0
- **applicationId / namespace**：`com.cycling.sonic`（app 模块）
- **入口**：`app/src/main/java/com/cycling/sonic/MainActivity.kt`（单 Activity）、`SonicApplication.kt`（`@HiltAndroidApp`）
- **签名**：`key.properties`（不入库）+ `sonic-release.keystore`；DeepSeek API Key 通过 `local.properties` 的 `DEEPSEEK_API_KEY` 注入

## Commands

```bash
# 构建（Windows 用 gradlew.bat，类 Unix 用 ./gradlew）
./gradlew build                  # 全量构建
./gradlew :app:assembleDebug     # 仅 Debug APK
./gradlew installDebug           # 构建并安装到设备/模拟器

# 测试
./gradlew test                   # 所有模块 JVM 单测
./gradlew :domain:test           # 单模块单测
./gradlew :data:test
./gradlew :presentation:test
./gradlew connectedAndroidTest   # 仪器测试（需连接设备）

# 代码规范检查
./gradlew lint                   # Android Lint
```

## Architecture

四个 Gradle 模块（`settings.gradle.kts` 中 `:app :domain :data :presentation`），依赖方向严格单向：

```
app → presentation → domain ← data
app → domain, app → data
```

- **app** (`com.cycling.sonic`)：`MainActivity`、`SonicApplication`、`MainViewModel`（主题）。仅装配，不含业务逻辑。
- **domain** (`com.cycling.domain`)：纯 Kotlin，无 Android 依赖（除 core-ktx）。
  - `model/` — 领域模型（`Song`、`Album`、`Artist`、`Lyrics`、`PlayerState` 等，`data class`）
  - `repository/` — 仓库**接口**（`interface XxxRepository`，返回 `Flow<T>` 或 `suspend`）
  - `usecase/` — 用例（`class XxxUseCase @Inject constructor`，`operator fun invoke`）
  - `lyrics/` — 歌词子系统：`parser/`（`ILyricsParser` 接口 + LRC/TTML/Syllable 等实现）、`model/`、`exporter/`、`utils/`
- **data** (`com.cycling.data`)：依赖 `:domain`，实现所有 repository。
  - `local/` — `database/`(`AppDatabase`)、`dao/`、`entity/`、`mediastore/`、`scanner/`、`lyrics/`、`metadata/`
  - `api/` — DeepSeek 远程 API（`DeepSeekApiService` 接口 + Impl + `dto/`）
  - `player/` — `PlayerManager`、`PlayerService`（Media3 MediaSessionService）
  - `repository/` — `XxxRepositoryImpl`（`@Singleton`，`@Inject constructor`）
  - `mapper/` — Entity ↔ Domain Model 映射（`Mappers.kt`）
  - `store/` — DataStore 封装（`ApiKeyStore`、`AiInfoCacheStore`）
  - `di/` — Hilt 模块（`RepositoryModule` 用 `@Binds`，`DatabaseModule`/`PlayerModule`/`AiModule` 用 `@Provides`）
- **presentation** (`com.cycling.presentation` + `com.cycling.core.ui`)：依赖 `:domain`，含 Compose UI。
  - `presentation/<feature>/` — 每个功能一个包，内含 `XxxContract.kt`（UiState/Intent/Effect）、`XxxViewModel.kt`、`XxxScreen.kt`
  - `presentation/navigation/` — `Screen`/`LibraryDestination`/`SettingsDestination`（`sealed interface : NavKey`）、`AppNavGraph`、`Navigator`
  - `presentation/lyrics/` — 卡拉OK 歌词 UI（`components/`、`utils/`）
  - `core/ui/theme/` — M3 主题系统（`M3ThemeManager`、`M3DynamicColor`、`DesignTokens`、`Typography`）
  - `core/ui/components/` — 可复用组件（`M3Button`、`M3Card`、`M3ListItem`、`M3Navigation` 等，前缀 `M3`）

### MVI 数据流
```
用户操作 → XxxIntent → ViewModel.handleIntent() → UseCase/Repository → 数据源
                                ↓
                  _uiState.update{} → uiState(StateFlow) → Compose 渲染
                  _uiEffect.emit{} → uiEffect(SharedFlow) → 一次性事件（导航/Toast）
```

## Conventions

### 强制规则
- **语言**：仅 Kotlin。新增 Java 文件禁止。
- **UI**：全部 Jetpack Compose，禁止 XML View（`AndroidManifest`/资源文件除外）。
- **包名**：根包 `com.cycling`；模块按 `com.cycling.<module>`（app 为 `com.cycling.sonic`）。presentation 的主题/组件用 `com.cycling.core.ui.*`。
- **DI**：所有 ViewModel 用 `@HiltViewModel` + `@Inject constructor`；Repository 接口在 domain，实现在 data 并通过 Hilt `@Binds` 绑定；UseCase 用 `@Singleton class XxxUseCase @Inject constructor`。
- **依赖方向**：data/presentation 只依赖 domain，绝不反向。domain 不依赖 data/presentation/Android 框架（仅 core-ktx + coroutines）。
- **异步**：用 Kotlin Coroutines + Flow；Repository 查询返回 `Flow<T>`，写操作用 `suspend`；ViewModel 中用 `viewModelScope.launch`。
- **导航**：Navigation 3 + `@Serializable` 的 `sealed interface : NavKey` 路由（见 `Screen.kt`）。

### MVI 三件套（每个 feature 必备）
文件命名 `XxxContract.kt`，内含：
- `data class XxxUiState(...)` — 默认参数构造，`val` 属性，不可变
- `sealed interface XxxIntent` — `data object` / `data class` 表达用户意图
- `sealed interface XxxEffect` — 一次性副作用（导航、Toast）
- `XxxViewModel`：`_uiState: MutableStateFlow`（对外 `val uiState: StateFlow`）、`_uiEffect: MutableSharedFlow`（对外 `val uiEffect: SharedFlow`）、`fun handleIntent(intent: XxxIntent)` 用 `when` 分发。

### 命名
- 类/接口：PascalCase；方法/变量：camelCase；包名：全小写。
- Repository 接口 `XxxRepository`，实现 `XxxRepositoryImpl`。
- UseCase `XxxUseCase`，入口方法 `operator fun invoke(...)`。
- Compose 组件文件与 Composable 同名 PascalCase；可复用 M3 组件前缀 `M3`。
- Hilt Module：`RepositoryModule`（`@Binds`）、`XxxModule`（`@Provides`）。

### 代码风格
- `kotlin.code.style=official`（见 `gradle.properties`）。
- 4 空格缩进；无分号；字符串优先双引号。
- 日志统一用 `Timber.d/w/e`（Debug 构建自动种 `Timber.DebugTree`，见 `SonicApplication`）。
- Entity ↔ Domain Model 转换放 `data/mapper/Mappers.kt`，用扩展函数 `fun XxxEntity.toDomain(): Xxx`。
- Room：`@Entity` 在 `data/local/entity/`，`@Dao` 在 `data/local/dao/`，`@Database` 在 `AppDatabase.kt`，DAO 返回 `Flow<List<...>>`。
- 不可变数据：`data class` 全 `val`，集合默认 `emptyList()`/`emptyMap()`。
- `nonTransitiveRClass=true` —— 资源 R 类不跨模块传递。

### 测试
- 框架：JUnit 4 + MockK + `kotlinx-coroutines-test` + Turbine（Flow 断言）。
- ViewModel 测试用 `TestDispatcherRule`（`presentation/util/`）替换 `Dispatchers.Main`，`runTest { advanceUntilIdle() }`。
- 测试方法名用反引号 `` `init loads search history`() `` 风格。
- 测试目录镜像 main 包结构。

## Notes

<!-- 临时补充区，快速记录后续发现 -->
- `spotify-clone-jetpack-compose-main/` 为参考项目目录，不参与构建。
- Release 签名需 `key.properties`（含 `storeFile/storePassword/keyAlias/keyPassword`），缺失时回退默认 keystore。
