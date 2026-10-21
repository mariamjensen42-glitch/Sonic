# 性能基准（`:benchmark`）

`:benchmark` 是一个 `com.android.test` 模块，用 AndroidX Macrobenchmark 测量**打包后的 app**
在真机/模拟器上的启动耗时和滚动帧时间。它不含产品代码，也不参与 `:app → :presentation →
:domain ← :data` 的依赖链 —— 唯一的联系是 `targetProjectPath = ":app"`。

## 跑之前

- 需要连接一台设备或启动模拟器（`minSdk 30`）。
- 被测变体是 `:app` 的 `benchmark` 构建类型（见 `app/build.gradle.kts`）：产物形态与 release
  一致（不可调试、不混淆），但用 debug 签名，所以不需要 release keystore。
- `app/src/benchmark/AndroidManifest.xml` 只给这个变体加上 `profileable`，让 shell 能做 AOT
  编译和抓 trace；正式包没有这个能力。

## 命令（由本人执行，Agent 不跑构建）

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest
```

只跑一条：

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.cycling.benchmark.StartupBenchmark
```

在模拟器上跑必须显式压掉那条 ``ERROR: Running on Emulator``，否则基准库会直接判失败
（模拟器的数字仍然没有代表性）：

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR,LOW-BATTERY
```

结果在 `benchmark/build/outputs/connected_android_test_additional_output/` 下的
`benchmarkData.json` 与 `*-benchmarkData.json`，Android Studio 会把它渲染成表格。

## 测了什么

| 测试 | 指标 | 说明 |
| --- | --- | --- |
| `StartupBenchmark.coldStartup` | `StartupTimingMetric` | 冷启动，基准库默认的编译模式（预热 + speed-profile），接近「用了一阵子」的真实情况 |
| `StartupBenchmark.coldStartupFullyCompiled` | `StartupTimingMetric` | 冷启动，`CompilationMode.Full()`，全部 AOT 之后的上限 |
| `SettingsScrollBenchmark.scrollSettings` | `FrameTimingMetric` | 设置页来回甩动时的帧耗时与错过 VSYNC 的比例 |

几条实现约定：

- **权限**：`MainActivity` 在第一帧之后就会申请媒体权限，不授权的话每轮都会盖一层系统
  对话框，首页根本出不来。`BenchmarkScope.grantMediaReadPermission()` 用 `pm grant` 直接
  授权（Android 13+ 是 `READ_MEDIA_AUDIO`，之前是 `READ_EXTERNAL_STORAGE`）。
- **为什么滚设置页**：曲库来自 MediaStore，宏基准没法注入样本音频，空库没有可滚动的列表。
  设置页（`Column(Modifier.verticalScroll(...))`）是唯一不依赖用户媒体库的长列表，而且就是
  普通 Material 组件树，改 `ListItem` / `Switch` / 主题最容易在这里看到回归。
- **定位方式**：UiAutomator 只能通过无障碍节点找 Compose 组件。首页的「设置」入口靠
  `By.desc("设置")`（即 `presentation` 的 `home_settings_desc`），**改这个文案要同步改**
  `BenchmarkScope.kt`。
- **迭代方向**：设置页在顶/底之间来回甩（偶数轮向下、奇数轮向上），避免甩到底之后空转，
  也不用在测量窗口里花时间滚回顶部。

## 读结果

- 启动看 `startupMs` 的 P50/P90；`coldStartupFullyCompiled` 与 `coldStartup` 的差距说明
  「启动慢」是编译没跟上还是代码路径本身长。
- 滚动看 `frameOverrunMs`（超时量）与 `frameDurationCpuMs` 的 P90/P99。P50 好看但 P99 差，
  是典型的掉帧/首帧 Jank，优先查新加的组合组件与动效。
- 数字只和同一台设备、同一个构建类型、同一次会话内的基线比较；跨设备/跨变体的绝对值没有
  意义。

## 不跑 Gradle 也能采的粗基线（adb）

宏基准要构建，而且测的是它自己那套编译模式；只想先看清楚「现在到底慢在哪」时，直接用 adb
更快，也不用改代码：

```bash
# 冷启动：连测 5 次看 P50（TotalTime 就是到首帧的时间）
adb shell am force-stop com.cycling.sonic
adb shell am start -W -n com.cycling.sonic/.MainActivity

# 滚动掉帧：先 reset，甩几次，再读历史帧
adb shell dumpsys gfxinfo com.cycling.sonic reset
adb shell input swipe 540 1800 540 700 250      # 反复几次
adb shell dumpsys gfxinfo com.cycling.sonic

# 启动期的 slice 级证据：命中第一次 doFrame 里的 measure / relayoutWindow
adb shell atrace --async_start -b 20000 -c gfx view wm am dalvik binder_driver sched freq
adb shell am start -W -n com.cycling.sonic/.MainActivity
adb shell atrace --async_stop > trace.txt
```

两条必须先确认的测量前提，否则数字没有意义：

- **装了哪个变体**。`app-debug.apk` 是 `DEBUGGABLE` 的，JIT 与校验行为都不同；
  要让数字代表用户，装 `app/build/outputs/apk/benchmark/app-benchmark.apk`
  （release 形态 + debug 签名，见上文）。
- **profile 有没有生效**。刚 `adb install` 完的包还没被 dexopt 过，跑出来的是「冷到不能再冷」
  的数字。要测「用了一阵子」的常态，先手动喂一次：

  ```bash
  adb shell cmd package compile -m speed-profile -f com.cycling.sonic
  ```

## 实测记录（2026-10-18，M2104K10AC / Android 13 / benchmark 变体）

### 第五轮：全页面巡检 + 常驻动画收口（播放页的波浪）

条件：用户自己构建并安装的 **release** 包（R8 生效、不可调试，7.6 MB / PSS 206 MB），同一台设备、
120 Hz 显示（`mActiveModeId=5`）。每个页面静止 4 s、滚动 3–4 次后读 `dumpsys gfxinfo`；CPU 用
`/proc/<pid>/stat` 的 utime+stime 差分。设备在充电、CPU 68 °C（DVFS 降频），帧时间偏悲观；
同一场景冷却后再测，播放页从 P50 17 ms 回到 **8 ms、0 帧超时**。

**启动与静止**（无动画的页面就该 0 帧）：

| 场景 | 实测 |
| --- | --- |
| 冷启动 `am start -W` ×3 | 302 / 307 / 310 ms |
| 温启动（HOME 后回来）×3 | 55 / 50 / 50 ms |
| 首页 / 库页 / 歌曲列表（198 首）/ 专辑网格（175 张）/ 歌手（81 位）/ 设置 / 音乐库统计 / 排除文件夹 / 收藏 / 歌曲详情 / 专辑详情 / 歌手详情 | **全部 0 fps** |
| 库页 / 搜索页（有光标闪烁时） | 1.2–2.3 fps（每秒一次的进度上报 + 文本光标） |

**滚动**（120 Hz，frame budget 8.3 ms）：

| 列表 | 帧数 | P50 | P90 | P99 | 超时帧 |
| --- | --- | --- | --- | --- | --- |
| 歌曲列表 | 387 | 5 ms | 16 ms | 17 ms | 4（1 %） |
| 专辑网格 | 296 | 5 ms | 8 ms | 10 ms | 3（1 %） |
| 歌手 | 297 | 5 ms | 16 ms | 19 ms | 8（2.7 %） |
| 歌曲详情 | 369 | 5 ms | 7 ms | 10 ms | 1（0.3 %） |
| 设置 | 389 | 5 ms | 5 ms | 7 ms | 0 |
| 播放队列 sheet | 603 | 5 ms | 7 ms | 9 ms | 2（0.3 %） |

**动画页（问题所在）**：

| 场景 | 实测 |
| --- | --- |
| 播放页 · 播放中（波浪振幅 > 0） | **124–131 fps**，1.08–1.1 核；高温时 P50 17 ms、61 % 帧超时，冷却后 P50 8 ms、0 超时 |
| 播放页 · 播放中（进度 < 10 % 或 > 95 %，振幅为 0） | **27.5 fps**，0.55 核 —— 只剩动态色面的成本 |
| 播放页 · **暂停** | **124 fps**，0.94 核，95 % 帧超时（← bug，见下） |
| 歌词页 · 播放中 | 124–131 fps，1.1 核 |
| 歌词页 · **暂停** | **0 fps**，0.12 核（没有行在读逐字 tick，这是对的） |
| 播放页上开播放队列 / 播放设置 sheet | 124–135 fps，1.1–1.3 核（sheet 后面的播放页仍在动） |
| 扫描本地音乐 · 扫描中（6 s） | 119 帧（≈20 fps，进度条 + 波浪），P50 5 ms，1 帧超时；**198 首扫完 1574 ms** |

**转场（1.2–1.4 s 窗口，只剩一次性卡点）**：

| 转场 | 帧数 | P90 | P99 | 超时帧 |
| --- | --- | --- | --- | --- |
| 列表 → 歌手详情 | 47 | 19 ms | 46 ms | 7 |
| 专辑网格 → 专辑详情 | 53 | 24 ms | — | 11 |
| 歌曲菜单 → 歌曲详情 | 44 | **38 ms** | — | 19（43 %） |
| 歌曲列表 → 播放页 | 119 | 11 ms | **150 ms** | 13 |
| 播放页 → 歌词页 | 183 | 23 ms | 57 ms | 36（20 %） |
| 切下一首（换封面主题） | 121 | 11 ms | 57 ms | 17 |

三条结论：

- **暂停的播放页在 124 fps 空转**，直接违反 ADR-0005 的「暂停即冻结、暂停时应为 0 帧」。
  根因是 `LinearWavyProgressIndicator` 的波浪：`waveSpeed` 默认等于波长，于是它按屏幕刷新率
  无限重绘整页，与是否在播放无关（也解释了为什么进度 < 10 % 时页面自己就掉到 27 fps ——
  振幅为 0 时它不重绘）。第三轮已经在迷你播放器里踩过同一个坑（换成静态
  `LinearProgressIndicator`），但播放页那一处漏了。修法：`waveSpeed` 跟随 `isPlaying`
  （暂停时 `0.dp`），波浪冻结成静态波形，形状不变。
- **歌词页的逐字 tick 不读 `animator_duration_scale`**：暂停时它不造成重绘（0 帧，符合预期），
  但系统「移除动画」时仍让整页按 120 fps 跑（设 `animator_duration_scale=0` 实测 124 fps），
  而那时本该停在第一帧。修法：只在播放中且动画缩放 > 0 时跑。
- **剩余卡点都在一次性事件上，不在常驻渲染**：进详情页 P90 19–38 ms、进播放页 / 切歌有
  57–150 ms 的尖峰 —— 后者是**换封面主题**（整个 App 换 `colorScheme` + 重新组合）与首次
  组合播放页的成本。`rememberArtworkTheme` 目前每换一首歌都重解码封面 → Palette → HCT 全量
  展开，没有缓存；这是下一步最值得做的一处（按 `imageUrl` 缓存 `SonArtworkTheme`）。

改后预期（待同一台真机复核）：暂停的播放页 0 帧；播放中播放页在波浪振幅为 0 的区间本来就只有
27 fps，振幅 > 0 时仍是 120 fps（那是波浪本身的成本，可以按下面的开关再降）。

> 后续：播放页进度条已换成静态 `LinearProgressIndicator`（规则见 `docs/design/motion.md`
> 「性能预算」），「振幅 > 0 时 120 fps」那一档成本不再存在；本轮的波形数据保留作历史对照。

复核方法：

```bash
adb shell dumpsys gfxinfo com.cycling.sonic reset
adb shell input keyevent 85            # 暂停
sleep 5
adb shell dumpsys gfxinfo com.cycling.sonic | grep -E 'Total frames|Janky|percentile'
# 期望 Total frames rendered: 0（播放页 / 歌词页都不再重绘）
```

```powershell
# 各页面静止帧数巡检（逐页切过去，每页 4 秒）
adb shell dumpsys gfxinfo com.cycling.sonic reset
Start-Sleep -Seconds 4
adb shell dumpsys gfxinfo com.cycling.sonic | Select-String 'Total frames rendered|50th percentile'
```

### 第四轮：R8（本轮最大的一笔）

`release`（以及继承它的 `benchmark` 变体）打开了 `isMinifyEnabled` + `isShrinkResources`，
压缩规则见 `app/proguard-rules.pro`（刻意保持最小）。同一台设备、同一个 benchmark 变体：

| 指标 | R8 前 | R8 后 |
| --- | --- | --- |
| APK 体积 | 41.2 MB | **7.07 MB（-83 %）** |
| dex 数量 / 体积 | 4 个 / 39 MB | **1 个 / 5.65 MB（-86 %）** |
| 冷启动，刚安装未 dexopt | 484 ms | **282 ms（-42 %）** |
| 冷启动，`-m speed-profile` | 308 ms | **282 ms** |
| 首帧 `Choreographer#doFrame` | 136 ms | 131 ms |
| 启动期 JIT 编译 | 1 次 / 0.7 ms | 1 次 / 0.7 ms |

两个结论：

- **「装完就冷启动」这个场景才是 R8 的主战场**：类少了 86 %，类加载与验证的开销跟着消失，
  于是 484 → 282 ms。
- **R8 之后 profile 的边际收益几乎归零**（282 vs 282）。这说明之前那 300 ms 里有一大块是
  「dex 里类太多」，而不是「热路径没预编译」。所以：先做 R8，再考虑 app 自己的 Baseline Profile。

顺带把搜索页四个结果列表补上了 `key`（`SearchScreen.kt`），列表头部插入时不再整列重建。

### 第三轮：迷你播放器的常驻动画（本轮最值钱的一条）

同样的条件——前台、非播放页、正在播放、用户什么都不做，只改了一处代码：

| 指标（10 秒窗口） | 改前 | 改后 |
| --- | --- | --- |
| 帧数 | **1231（约 123 fps 持续重绘）** | **10（每秒 1 帧，只有进度那一下）** |
| CPU（进程） | 100 % ~ 114 % | 16 %（其中 ExoPlayer 解码约 7.5 %） |
| 暂停时帧数 | 1231（暂停也照跑） | 0 |

原因：迷你播放器底部的进度条用的是 `LinearWavyProgressIndicator`。那个波浪是**常驻动画**，
只要迷你播放器在场（即只要库里有当前曲目），整块界面就一直在重绘 —— 和是否在播放无关。
换成静态的 `LinearProgressIndicator` 后回到「每秒一帧」。

复现方法（不用跑构建，随时可验证）：

```bash
adb shell dumpsys gfxinfo com.cycling.sonic reset
adb shell input keyevent 85   # 暂停，界面保持静止
sleep 10
adb shell dumpsys gfxinfo com.cycling.sonic | grep 'Total frames rendered'   # 期望 0
```

### 第一轮：只差 profile 是否已应用（同一 APK）

| 场景 | profile 未应用 | profile 已应用 |
| --- | --- | --- |
| 冷启动 `am start -W` TotalTime（5 次中位） | 552 ms | **320 ms** |
| 首帧主线程 `Choreographer#doFrame` | 344 ms | 166 ms |
| 其中首次组合 + 测量 `measure` | 118 ms | 56 ms |
| 设置页滚动（686 帧） | 1.96 % janky，P90 8 ms | **0 % janky，P90 6 ms** |
| 温启动（进程还在、Activity 重建） | — | 52 ms |

### 第二轮：载入态改造之后（渐进首页 + 延迟转圈 + 主题预热 + 共享 ImageLoader）

| 场景 | 改造前 | 改造后 |
| --- | --- | --- |
| 冷启动，刚安装未 dexopt | 552 ms | **484 ms** |
| 冷启动，`-m verify`（≈无 profile） | — | 509 ms |
| 冷启动，`-m speed-profile` | 320 ms | **308 ms** |
| 首帧 `Choreographer#doFrame`（speed-profile） | 166 ms | 136 ms |
| 首帧 `measure`（`-m verify`） | 118 ms | 105 ms |
| 首帧 `measure`（speed-profile） | 56 ms | 49 ms |

结论与后续动作：

- **启动是编译问题，不是代码路径问题**。`bindApplication` 的 42 ms 里 `app.onCreate` 只占
  1.7 ms，剩下是 ART 装载 dex/oat 与 JIT；profile 一应用，首帧实测掉的 178 ms 几乎全在这里。
- **无 profile 时最贵的单帧事件是一次 JIT 编译本身**：`-m verify` 状态下的第二帧里
  `JIT compiling androidx.collection.MutableScatterMap.findInsertIndex` 独占 217 ms。
  这类帧不是 UI 写错了，别在那儿找原因。
- 主题预热（`SonBrandSchemes.prewarm`）只在**主题代码已经 AOT/JIT 热**时稳赢：HCT 展开在
  纯解释执行下要几十毫秒，可能赶不上首帧；此时它会退回同步计算（行为与改造前一致）。
  想让它每次冷启动都生效，仍然要靠 profile 覆盖 `com.cycling.core.ui.theme.*`。
- APK 里已经有 `assets/dexopt/baseline.prof`（14 KB），但那是 Compose 等库自带的 profile，
  app 自己的 `HomeScreen` / 主题 / Hilt 生成代码不在里面 —— 这正是「profile 已应用仍有
  300 ms」的那部分。
- 首帧 `relayoutWindow#first=true` 稳定占 65 ms（含 `setSurface` 22 ms、`allocateBuffers`
  31 ms），属于每个 app 都要付的一次性窗口/缓冲分配，暂不处理。

## 后续可加

- **Baseline Profile 生成（首选）**：需要在 `:app` 加 `androidx.profileinstaller` 与
  `androidx.baselineprofile` 插件，新开一个 `:baselineprofile` 模块。
- 有样本媒体的曲库滚动基准：给设备推一批测试音频后再加 `libraryScroll` 测试。
