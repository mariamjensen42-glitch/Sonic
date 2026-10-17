# Material 3 Expressive 审计与试点计划

基线：官方 [M3 Expressive 规范](https://m3.material.io/blog/building-with-m3-expressive) +
[Motion](https://m3.material.io/styles/motion/overview/how-it-works) + 官方示例构图。判断标准不是“用了
Material 3”，而是**有没有 expressive moment，以及 quiet surface 是否真的安静**。

## 现状结论

主题层与共享底座已经 expressive：`MaterialExpressiveTheme`、`MotionScheme.expressive()`、Emphasized 字号、
Roboto Flex + Material Symbols、封面取色、`SonColorField` / `SonShapeMorph`、导航层弹簧转场都在。

2026-10-18 起按票直接落了代码（跳过原型/方案文档这一轮）：首页与播放页两张试点、音乐库、列表族
（歌曲/专辑/歌手/文件夹/搜索）、内容族（歌曲详情/AI 解读）都换成了同一套页面语法；设置页按 #14 的
取舍**保持安静**，只跟随字阶与间距，不做色面/形变。

> 字体/图标选型已定：**C（Roboto Flex 全局 Latin + Material Symbols Rounded，中文回退系统）**。
> 决策与落地清单见 `docs/design/type-and-icons.md`，ADR 见 `docs/adr/0002-type-and-icons.md`。
> 已由 #15 落地：字体换成 Roboto Flex 子集，图标换成 Material Symbols Rounded + `M3SymbolIcon`，
> `material-icons-extended` 已移除；实测与偏差见 `docs/design/type-and-icons.md` 的「落地结果」。

## 页面语法（代码里的唯一事实）

各屏共用一套可复用的组合，新增页面照抄这三件即可，不要再手写字号/间距：

| 语法 | 实现 | 用在哪 |
| --- | --- | --- |
| 标题时刻 | `LargeFlexibleTopAppBar`（tab root / 页面首屏）或 `MediumFlexibleTopAppBar`（列表族、内容族） | 首页、音乐库、歌曲、专辑、歌手、文件夹、AI 解读 |
| 分区标题 | `SonSectionHeader`（`titleLargeEmphasized` + 可选「查看全部」） | 首页、音乐库 |
| 空态 / 载入态 | `SonEmptyState` / `SonLoadingState` | 首页、歌曲、专辑、歌手、文件夹、AI 解读 |

三条硬规则：色面只有 hero 光晕（`primaryContainer`）与受限动作簇（`secondaryContainer`）；
形变只有播放键按压与 hero 折叠；正文列表一律无分割线。

## 六条验收标记

每屏至少做到 4 条；试点屏必须 6 条全过：

1. 一个超大 Emphasized 标题时刻（display/headline 级，不是 body 放大）。
2. 一块真正饱和的色面（container 角色），而不是近黑底上的小色点。
3. 一个按下会形变的主控件（shape morph，不是 ripple 换色）。
4. 一次有物理感的转场（空间弹簧，允许轻微过冲）。
5. 一个 Expressive 组件，或官方组件组成的控制簇（FAB menu / ButtonGroup / SplitButton / Toolbar / Short nav / Loading / Wavy / 共享容器的动作簇）。
6. 正文区是 quiet surface：无分割线、少装饰、留白优先。

## 逐屏审计

| 屏幕 | 现在的构图 | 主要缺口 | 优先级 |
| --- | --- | --- | --- |
| 歌单 / 专辑 / 歌手详情（`CollectionDetailScreen`） | 沉浸式方形 hero + 色面光晕 + tonal 动作簇 + 介绍 + 72dp 无分割线列表 | 整屏转场已交给 `NavDisplay` 的空间弹簧；真机明暗截图待补 | P0（试点 1，代码已落） |
| 首页（`HomeScreen`） | `LargeFlexibleTopAppBar` 问候语 + 非对称最近播放 hero（大卡 + 两行）+ `ButtonGroup` 快速入口 + 安静分区横滑 | 真机明暗截图待补 | P0（试点 2，代码已落） |
| 播放页（`PlayerScreen`） | 封面背后的 `primaryContainer` 光晕 + `HorizontalFloatingToolbar` 运输簇 + 96dp 播放键按压形变 + `headlineMediumEmphasized` 标题 | 真机明暗截图待补 | P0（试点 3，代码已落） |
| 音乐库（`MyMusicScreen`） | `LargeFlexibleTopAppBar`（歌单计数副标题）+ 两个入口行 + 歌单分区 + 无分割线 | 只有一处色面（入口图标托底），按 #14 的“工具型页面”取舍 | P1（代码已落） |
| 全部歌曲 / 搜索 / 文件夹 / 专辑 / 歌手 | `MediumFlexibleTopAppBar` + 计数副标题 + 无分割线列表 + 统一空态 | 搜索仍是吸顶输入框（保留），没有自己的 moment | P1（代码已落） |
| 设置 | 分组卡片 + `ListItem` + Switch/滑块 | 按 #14 保持安静：不加大标题、不改扫读性；卡内分割线保留 | P2（有意不改） |
| 歌曲详情 / AI 解读 / 歌词 | 歌曲详情加了封面光晕 + `headlineMediumEmphasized`；AI 解读去掉两层卡片变成 surface 阅读面；歌词未改（已是最接近 Expressive 的一屏） | 歌词的色面与转场仍留白 | P2（代码已落） |

## 落地状态（2026-10-18）

代码已按上表落地；`docs/design/screens/<screen>/` 里的明暗截图与六条勾选仍需在真机上补
（本轮的 agent 只做静态检查，不跑构建、不连设备）。

- 首页：`LargeFlexibleTopAppBar` 承担第 1 条；hero 光晕承担第 2 条；`ButtonGroup` 承担第 3、5 条
  （被按项横向扩张是官方组件内部形变）；`NavDisplay` 的空间弹簧承担第 4 条；分区无分割线承担第 6 条。
- 播放页：光晕承担第 2 条；96dp 播放键的 `rememberPressShapeMorph` 承担第 3 条；浮动运输 toolbar 承担第 5 条；
  `headlineMediumEmphasized` 标题 + surface 正文承担第 1、6 条；第 4 条由导航层的 push/pop 弹簧承担。
- 音乐库 / 列表族 / 内容族：第 1、6 条稳定；第 5 条由 `MediumFlexibleTopAppBar`、分段按钮、`LoadingIndicator` 承担；
  第 2、3 条按屏取舍（工具型页面不硬塞色面）。

## 试点计划

### 试点 1：CollectionDetail 外壳（影响歌单、专辑、歌手三页）— 已落地（色面部分按 #6 规则修订，落地在 #18）

- hero 标题升到 `displaySmallEmphasized`，副标题用 label 级字距，形成“海报 → 说明”的跳级。
- 列表去掉分割线，行距交给留白；保留 72dp 行高与拖拽编辑态。
- 动作区合并为一个 tonal 控制簇（shuffle / 播放全部 / 添加到歌单），作为该屏的受限色面（secondaryContainer；#6 规则）。
- hero 用封面派生的 primaryContainer 光晕，到封面下沿收干净；正文从 surface 开始；动作簇用 secondaryContainer 补一块受限色面（见 [color-field-shape-morph.md](color-field-shape-morph.md)）。
- 拖拽排序保留“编辑态才出现行尾把手”，并把抬起行做成带背景与阴影的浮层。

六条标记 1、2、3、5、6 由本屏代码承担；第 4 条（整屏转场）在 #16 之后由导航层统一提供：
`NavDisplay` 的 push/pop 是「位移 + 淡入淡出」的空间弹簧（`defaultSpatialSpec` / `defaultEffectsSpec`），
不再挂在本屏。剩余工作只有真机明暗截图与勾选。

### 试点 2：首页 — 已落地（2026-10-18）

- `LargeFlexibleTopAppBar` 承载问候语（display 级）与副标题；搜索/设置降为 actions，滚动时收成 64dp。
- 「最近播放」改成左大右小的非对称 hero：大卡封面随宽度取 58%，右侧两行紧凑条目；色面是这一屏
  唯一的 `primaryContainer` 光晕，到大卡下沿归零。光晕跟着**页面主题**（当前播放封面或品牌色），
  不为 hero 单独换一套封面配色 —— 色面会画一层自己的 surface，换色会在 hero 区留下一块可见的矩形。
- **没有引入 FAB menu**：#5（导航外壳）已定「标准 `NavigationBar` + 无 FAB」，且 FAB menu 会与迷你
  播放器抢右下角。快速入口改成官方 `ButtonGroup`（全部歌曲 / 专辑 / 歌手），按下时被按项横向扩张。
- 分区标题统一走 `SonSectionHeader`（`titleLargeEmphasized` + 「查看全部」）；卡片圆角与色面跟着详情页语法走。

### 试点 3：播放页 — 已落地（2026-10-18）

- 封面背后是封面派生的 `primaryContainer` 光晕（B 规则，见 [color-field-shape-morph.md](color-field-shape-morph.md)），
  到封面下沿归零，标题以下全部落在 surface；封面本身保持大圆角。
- 运输控件改成官方 `HorizontalFloatingToolbar`：shuffle → prev → play → next → repeat；播放键仍是 96dp
  `LargeFloatingActionButton`，按压时 28dp 圆角 → 圆（`SonShapeMorph`）。
- 标题升到 `headlineMediumEmphasized`，歌手名降到 `labelLarge` 字距。
- 顶栏从四个图标收成「关闭 / 歌词 / 队列」；收藏与播放设置收进标题行右侧的动作簇，右上角不再堆图标。

## 技术约定

- 动效只从 `MaterialTheme.motionScheme` 取，场景与档位见 `docs/design/motion.md`（决策见 ADR-0003）；唯一的时序例外是歌词自动滚动与伴奏行进出，不再新增裸 `tween` / `spring` 参数。
- 字号、间距、尺寸只从 `SonTypography` / `SonSpacing` / `SonSizes` / `SonLayout` 取。
- 颜色只从 `MaterialTheme.colorScheme` 或 `LocalSonExtendedColors` 取；色面规则见 [color-field-shape-morph.md](color-field-shape-morph.md)（`primaryContainer` 只做 hero 光晕，`secondaryContainer` 只做受限动作簇；ADR-0004），不手写十六进制。
- 组件直接使用 `androidx.compose.material3`；`core/ui/components/` 只放 Material 没有的组合（当前只有 MediaCards 与底栏）。
- 每屏改完在真机上截图，对照六条标记逐条勾选；不达标不算完成。
