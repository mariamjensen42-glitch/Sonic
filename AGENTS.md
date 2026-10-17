# Sonic — AGENTS.md

> **⛔ 最高优先级规则（覆盖本文件其余全部规则）：严禁执行 `./gradlew` / `.\gradlew.bat`、`gradle`，以及任何 assemble / build / compile / test / lint / install 等构建任务。构建太耗时，一律由用户自己执行；Agent 只能用 read / grep / glob 做静态检查，不得为了“验证”启动 Gradle，也不得后台起构建。**

现代化 Android 音乐播放器，Kotlin + Jetpack Compose，Clean Architecture + MVI，AI 驱动的音乐洞察。

> **⚠️ 每轮对话开始时，必须先完整阅读本文件（AGENTS.md），再开始任何工作。** 本文件定义了项目的技术栈、代码风格、项目结构与强制规则，是所有开发决策的依据。

## Project

- **语言**：Kotlin 2.3.0（强制，禁止 Java/其他 JVM 语言）
- **UI**：Jetpack Compose（BOM `2026.09.00`）+ Material 3 Expressive（`material3` 显式钉 `1.5.0-alpha29`，BOM 里的 1.4.0 缺 Expressive 组件集）
- **架构**：Clean Architecture（domain/data/presentation 三层）+ MVI（Intent/State/Effect 单向数据流）
- **DI**：Hilt 2.59.2 + KSP 2.3.8
- **本地存储**：Room 2.7.1、DataStore Preferences
- **播放**：Media3 ExoPlayer 1.9.0 + MediaSessionService（前台服务后台播放）
- **图片**：Coil 3.1.0
- **导航**：Navigation 3（`androidx.navigation3`）+ `@Serializable` 类型安全路由
- **其他**：kotlinx-coroutines/serialization、OkHttp、Timber（日志）、mp3agic（音频元数据）、palette-ktx（取色）
- **JDK**：17（`jvmToolchain(17)`）；**compileSdk**：37（+ `compileSdkMinor = 1`，因 material3 1.5.0-alpha29
  与 Compose 1.13.0-alpha01 的 AAR 要求 `minCompileSdk = 37`）；**targetSdk**：36；**minSdk**：30
- **AGP**：9.3.0
- **applicationId / namespace**：`com.cycling.sonic`（app 模块）
- **入口**：`app/src/main/java/com/cycling/sonic/MainActivity.kt`（单 Activity）、`SonicApplication.kt`（`@HiltAndroidApp`）
- **签名**：`key.properties`（不入库）+ `sonic-release.keystore`；DeepSeek API Key 通过 `local.properties` 的 `DEEPSEEK_API_KEY` 注入

## Commands

> **下面的命令只写给用户执行；Agent 禁止运行其中任何构建/编译/测试任务（见文件顶部最高优先级规则）。**

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
  - `api/` — 远程 API：DeepSeek（`DeepSeekApiService` 类 + `dto/`）、在线歌词（`api/lyrics/`，`OnlineLyricsProvider` 的 LRCLIB / NetEase 实现 + `dto/`）
  - `player/` — `PlayerManager`、`PlayerService`（Media3 MediaSessionService）
  - `repository/` — `XxxRepositoryImpl`（`@Singleton`，`@Inject constructor`）
  - `mapper/` — Entity ↔ Domain Model 映射（`Mappers.kt`）
  - `store/` — DataStore 封装（`ApiKeyStore`、`AiInfoCacheStore`）
  - `di/` — Hilt 模块（`RepositoryModule` 用 `@Binds`，`DatabaseModule`/`PlayerModule`/`AiModule` 用 `@Provides`）
- **presentation** (`com.cycling.presentation` + `com.cycling.core.ui`)：依赖 `:domain`，含 Compose UI。
  - `presentation/<feature>/` — 每个功能一个包，内含 `XxxContract.kt`（UiState/Intent/Effect）、`XxxViewModel.kt`、`XxxScreen.kt`
  - `presentation/navigation/` — `Screen`/`LibraryDestination`/`SettingsDestination`（`sealed interface : NavKey`）、`AppNavGraph`、`Navigator`
  - `presentation/lyrics/` — 卡拉OK 歌词 UI（`components/`、`utils/`）
  - `core/ui/theme/` — Material 3 Expressive 主题系统：`Theme.kt`（`SonicTheme`）、`Color.kt`、`Hct.kt`、`TonalPalette.kt`、`Typography.kt`、`Shape.kt`、`MotionTiming.kt`、`Spacing.kt`、`ArtColorExtractor.kt`
  - `core/ui/components/` — 仅放 Material 未提供的组合组件（当前只有 `MediaCards.kt`：`M3Artwork`、`M3MediaCard`、`M3ArtistCard`）。Material 已提供的组件一律直接用 `androidx.compose.material3`，不要再包一层

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

### 设计体系（Material 3 Expressive，强制）
全应用只使用 Material 3 Expressive，**不存在任何旧风格残留**。设计系统的唯一入口是
`core/ui/theme/`，页面不得自带样式常量：

- **主题**：只能通过 `SonicTheme` 装配。配色来源优先级：显式 `colorScheme`（封面种子色，见
  `rememberArtworkTheme`）→ 系统动态取色 → 品牌种子色 `SonicSeed`。三条路径都收敛到
  `SonTonalScheme` 的 HCT 色调映射，不要绕开它直接拼颜色。
- **颜色**：只能取自 `MaterialTheme.colorScheme.*`；M3 角色无法表达时才用
  `LocalSonExtendedColors.current`（封面占位、封面之上内容、歌词高亮、波形）。
  **禁止**在页面里写 `Color(0x...)`/`Color.White`/`Color.Gray`（`Color.Transparent` 仅用于表达真透明）。
- **排版**：只能用 `MaterialTheme.typography.*`（含 `*Emphasized` 强调变体）。
  **禁止**在 `Text` 上写 `fontSize`/`fontWeight`。字体族见 `SonFontFamily`（Roboto Flex 静态子集
  regular/medium/bold，中文由系统 CJK 回退），**禁止**再出现 `FontFamily.Default`。
- **形状**：只能用 `MaterialTheme.shapes.*` 或 `CircleShape`。**禁止**裸写 `RoundedCornerShape(n.dp)`，
  也**禁止**给 Material 组件传 `shape` 去复刻它本就有的默认形状（卡片、列表项、文本框、芯片、
  对话框、抽屉都不需要传）。
- **间距与尺寸**：只能用 `SonSpacing` / `SonSizes` / `SonLayout`。
- **动效**：弹簧只从 `MaterialTheme.motionScheme` 取（`defaultSpatialSpec` / `fastSpatialSpec` /
  `slowSpatialSpec` 与对应 effects 三档），场景与档位的对应见 `docs/design/motion.md`。
  **禁止** `tween(300)`、`spring(dampingRatio = 0.8f, ...)` 这类裸参数；唯一例外是歌词的自动滚动与
  伴奏行进出（`SonMotionTiming` 的时序 token，见 `docs/design/motion.md`）。
  `MotionScheme.expressive()` 已由主题注入。
- **交互反馈**：**禁止** `indication = null`；直接 `Modifier.clickable(onClick = ...)` 即由 Material
  提供 ripple 与 state layer。Expressive 的按压反馈是**形状变形**，Material 组件（`Button`、
  `IconButton`、`ListItem`、`SegmentedButton`）内部已经实现，页面不要再手写形变或缩放。
- **图标**：统一用 Material Symbols Rounded + `M3SymbolIcon`（`filled = true` 强调/选中，
  `filled = false` 弱化/未选中；码位集中在 `SymbolIconName`，字体见 `SonSymbolFontFamily`）。
  **禁止**再用 `material-icons-extended` 的 `Icons.*`，也不要新增自绘 vector drawable 当图标；
  唯一保留的 drawable 是启动图标。新增图标时同步 `scripts/subset-fonts.py` 的码位清单并重跑脚本。
- **组件**：**直接使用 `androidx.compose.material3` 的原生组件**（`Button` / `Card` / `ListItem` /
  `TopAppBar` / `LoadingIndicator` / `OutlinedTextField` / `Chip` 等），让它们的官方默认值决定
  尺寸、形状、内容边距与排版。
  `core/ui/components/` 里**只允许**放 Material 未提供的组合，目前 4 个：
  `M3Artwork`（封面加载：Coil + 占位）、`M3MediaCard`、`M3ArtistCard`、`M3SymbolIcon`（图标字体渲染）；
  另有 `navigation/M3BottomNavigationBar`（标准 `NavigationBar` + 迷你播放器插槽；决策见 `docs/design/navigation-shell.md`）。
  **禁止**再新增对 Material 组件的转发壳（历史上那层 `M3Button` / `M3Card` / `M3ListItem` /
  `M3Indicator` / `M3TextField` / `M3Chip` / `M3Navigation` / `M3Interaction` 已全部删除）。
- **底部留白**：底部导航与迷你播放器是浮层，可滚动内容必须预留
  `SonLayout.bottomBarClearance`（迷你播放器可见时再加 `SonLayout.miniPlayerClearance`）。
- **`ListItem` 两个重载的主内容槽名不同**（易踩）：非交互重载用 `headlineContent`；
  带 `onClick` / `selected` / `checked` 的可交互重载主内容槽叫 **`content`**
  （同时它还有 `shapes` / `elevation` / `contentPadding`）。写可点击列表行时用
  `ListItem(onClick = ..., content = { ... }, supportingContent = ...)`，不要写成 `headlineContent`。
- **Expressive 方向**：以官方 M3 Expressive 规范与示例为准，每屏 1–2 个 expressive moment + 安静阅读面；
  取舍、试点顺序与六条验收标记见 `docs/adr/0001-expressive-ui-direction.md` 与
  `docs/design/expressive-audit.md`。词义见根目录 `CONTEXT.md`。

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

## Agent skills

### Issue tracker

Issues live in GitHub Issues on `mariamjensen42-glitch/Sonic`, managed with the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role triage vocabulary — `needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix` — plus the `wayfinder:*` labels. See `docs/agents/triage-labels.md`.

### Domain docs

Layout: single-context — one `CONTEXT.md` at the repo root, architecture decisions in `docs/adr/`. See `docs/agents/domain.md`.
