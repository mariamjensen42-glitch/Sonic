# 播放页与歌词页使用封面派生的动态色面

**状态**：Accepted（2026-10-20）。**修订** ADR-0004 中「D 全屏洗」被否的结论，仅对播放页与歌词页生效。规则实现见 `core/ui/theme/SonDynamicBackdrop.kt`。

## Context

用户要求播放页（`PlayerScreen`）与歌词页（`LyricsScreen`）具备 AMLL / Apple Music 风格的动态背景。

这与 ADR-0004 的结论直接冲突：那里把「D 全屏洗（整块 primaryContainer 铺满）」列为被否方案，
理由是「整屏 tint，阅读疲劳；OLED 大面积亮色」。但那条针对的是**静态**铺色：颜色深、面积大、
一直亮着。动态色面在四点上都不同 —— 颜色是封面 swatch 归一明度后混 surface（比整块 container
暗得多）、只在播放时流动、歌词页还额外压暗、暂停即静止。

技术上还受三条既有约束：

- **minSdk 30**：没有 AGSL（`RuntimeShader` 要 33+），也没有 WebGL 通道，Js 版 AMLL 的背景
  组件无法照搬；
- **ADR-0004 禁止 blur / RenderEffect / 大面积 graphicsLayer 模糊**，所以不能走「封面模糊 +
  旋转」那条路；
- `docs/design/motion.md` 规定常驻可见容器不得自发重绘，且自绘无限动画必须自己处理
  `animator_duration_scale = 0`。

## Decision

- **主题层新增 `SonDynamicBackdrop`**：4×4 控制点网格渐变；控制点颜色来自**封面上真实取到的
  颜色**（`ArtColorExtractor.backdropOf`：Palette 的 swatch 按「彩度 × 面积占比」排序、再挑色相
  彼此相差 ≥32° 的 2~3 档），经 `LocalSonBackdropColors` 注入；每档只保留色相，明度归一到
  `DARK_TONES` / `LIGHT_TONES`、彩度夹进 20~56，再按主题相关比例混到 `surface`。
  域扭曲缓慢流动，56×56 离屏位图 + GPU 双线性放大。**不引入 shader、模糊、WebView 或新依赖**。
- **不用配色方案里的 container 角色**：`SonTonalScheme.Expressive` 会把 secondary / tertiary
  的色相旋转 45°~120°（对齐 Material Color Utilities 的 SchemeExpressive），那两档颜色并不在
  封面上 —— 第一版实现就是这么做的，结果背景看起来「不是封面的颜色」。没有封面或封面接近
  灰阶（swatch 彩度全部 < 12）时，才退回配色方案的三个 container 角色。
- **播放页**：hero 段最浓，从 hero 下沿开始向 `surface` 收，标题/进度/运输控件落回安静
  底（沿用 ADR-0004 的几何）；替代原来的静态 `SonColorFieldBackground`。
- **歌词页**：整屏铺开，但压暗 22%，因为这一屏是阅读面；其余页面继续用 ADR-0004 的静态光晕。
- **三条硬约束**：暂停即冻结相位（不再请求帧）；系统动画缩放为 0 时停在第一帧；
  帧率上限 30fps。流动周期与帧间隔登记进 `SonMotionTiming`。
- **不做音频可视化**：Android 的 `Visualizer` 需要 `RECORD_AUDIO` 权限，为一个背景动画要
  麦克风权限不值得。AMLL 的 `lowFreqVolume` 这一维直接放弃。
- **不新增设置项**：暂停即静止已经覆盖省电诉求；若以后有耗电反馈再加 staticMode 开关。

## Considered Options

| 方案 | 结果 |
| --- | --- |
| A 保持静态光晕 | 否：用户明确要动态背景；也是原状 |
| B 封面位图模糊 + 旋转/缩放（AMLL PixiRenderer 路线） | 否：ADR-0004 禁止整屏模糊；`Modifier.blur` 在 API 30 上无效 |
| C AGSL `RuntimeShader`（33+）+ Canvas 兜底 | 否：两套实现、两套视觉效果，且无法用单测钉住 |
| D 网格渐变 + 小位图放大（选中） | 是：纯函数可测、全版本一致、每帧只算 56×56 像素 |
| E WebView 里跑 AMLL | 否：为一个背景引入 WebView 与 Js 桥，得不偿失 |
| F 封面取色后画多个呼吸光斑 | 否：光斑只有圆形对称，做不出 AMLL 的流动感；且每帧要新建 Brush / shader |

## Consequences

- 播放页的静态光晕被动态色面取代；Home / SongDetail / CollectionDetail 的光晕与动作簇不变。
- 颜色的来源从「配色方案」下移一层到「封面 swatch」：`rememberArtworkTheme` 现在一次 Palette
  分析同时产出种子色（给方案）与 backdrop 颜色（给色面）。种子色那条链路没有变，
  「近灰封面回退品牌色相」「亮色 alpha 高于暗色」等既有规则继续生效。
- 色面的对比度不再能直接套用 M3 角色对：它在混入 surface 前被归一了明度，所以文字落点的
  对比度按「surface 与归一后色面的混合」估算；歌词页再压暗 22% 留余量。
- 语义变化：这两页的色面不再要求「53% 处必须归零」，改为「hero 段浓、下半个屏幕收干净」；
  歌词页用整屏 + 压暗，正文落点仍在接近 surface 的底上。
- 这是本项目第一处**自绘无限动画**，所以降级规则从「本项目没有自绘无限动画」改为
  「这一处自己读 `ValueAnimator.getDurationScale()`」。
- 验收：真机明暗两套截图；暂停播放后 `dumpsys gfxinfo <pkg>` 帧数应为 0；单测覆盖
  `SonMeshGradientTest`（不透明、无接缝、明暗分离、**色面相位于封面颜色**）。
- 构建与设备测量由用户执行；Agent 只做静态检查与单测代码。

## 修订（2026-10-20，真机验收后）

第一版在真机上铺出来的仍是**品牌紫**而不是封面色，两个原因都出在「颜色怎么进到色面」这条链上：

1. **着色器之前的取色环节没有色可用**。`backdropOf` 当时自己设了「HCT 彩度 ≥ 12」的门槛，
   而 `resolveSeedHue` 的灰封面阈值是 16、取色器把种子彩度抬到 12 —— 三个常量互相打架：
   凡是淡封面（奶油色、灰蓝、莫兰迪色），种子被判成灰（回品牌色相），backdrop 又全部被门槛
   筛掉（回落配色方案的 container），于是整页都是品牌色。现在改为**直接用 `androidx.palette`
   的 vibrant / muted / dominant 档位**按色相挑 2~3 个，不再自己设彩度门槛；彩度不足交给
   `SonMeshGradient.palette` 统一抬高（那里本来就有 20~56 的彩度区间）。灰封面阈值降到 8，
   低于取色器的 12，保证「抬过彩度的种子」一定保留封面色相。
2. **封面位图读不到**。取色曾用 Coil 的 `SingletonImageLoader.execute` 读
   `content://media/external/audio/albums/<id>`：这条 legacy albums 行在 Android 10+ 上
   `openInputStream` 会抛 `no such column: _data`，而 Coil 内存缓存里可能是硬件位图
   （`Palette` 取色会抛异常）。现在改用与通知栏 / 桌面小组件同一条已验证的路径
   （`CoverArtworkLoader`：借专辑下的一首曲目走 `ContentResolver.loadThumbnail` → 图片流 →
   内嵌封面），并去掉 Coil 依赖；失败时 `Timber.w` 记一行，`SonArtworkTheme` 仍回落品牌色。

另外：只要有封面，`backdrop` 就不会为空 —— Palette 一个 swatch 都给不出来时用种子色兜底，
品牌 container 只留给「真的没有封面」的情况。

验收改为：播放一首**淡色封面**（例：Taylor Swift《Fearless》）与一首**深色低彩度封面**
（例：TheFatRat《Monody》），色面与整页配色都应呈现封面色相；日志里
`rememberArtworkTheme: seed=#...... backdrop=#......` 可直接对照。

## 修订 2（2026-10-20，真机验收后）

色相对了之后用户报「动态背景没有动」。真机上量出来的结论是**动画在跑，但看不见**：

- 播放中用 `adb exec-out screencap` 相隔 12 秒取两帧，色面区域的像素差最大只有个位数
  （`dumpsys gfxinfo` 同期帧数正常增长、进度条波形在动，所以帧循环本身是活的）；
- 原因在数值上：相位走 37.5% 的周期，采样坐标只挪 `WARP(0.05) × 4 格 ≈ 0.2 格`，
  而离屏位图只有 48 像素宽，0.2 格 ≈ 2 个纹素 —— 双线性放大到全屏后就完全看不出来。
  周期 32s 又把这个变化摊薄了一倍。

所以把「周期」和「振幅」当成一组参数一起改：

- 周期 32s → **16s**（`SonMotionTiming.BACKDROP_CYCLE_MS`）；
- 域扭曲振幅 0.05 → **0.22**，细节层 0.022 → **0.08**；
- 新增整体漂移 `DRIFT = 0.05`，让色场平移而不只是原地呼吸（乘边缘衰减，最外圈不动）；
- 离屏位图 48² → **56²**，大振幅下放大后不会露出网格感。

离线把 `SonMeshGradient` 逐像素重算了一遍再定的值：旧参数「一秒的变化」平均 0.8/765（不可见），
新参数约 8/765、三秒约 20~26/765（能明显看出在流）。这条下限写进了
`SonMeshGradientTest` 的「一秒内动得出来」用例 —— 以后谁把周期调慢或振幅调小，会直接变红。
