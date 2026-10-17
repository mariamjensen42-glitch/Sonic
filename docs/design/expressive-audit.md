# Material 3 Expressive 审计与试点计划

基线：官方 [M3 Expressive 规范](https://m3.material.io/blog/building-with-m3-expressive) +
[Motion](https://m3.material.io/styles/motion/overview/how-it-works) + 官方示例构图。判断标准不是“用了
Material 3”，而是**有没有 expressive moment，以及 quiet surface 是否真的安静**。

## 现状结论

主题层已经 expressive：`MaterialExpressiveTheme`、`MotionScheme.expressive()`、Emphasized 字号、
封面取色都在。缺的是**页面级的使用**：字体族仍是 `FontFamily.Default`，列表普遍带分割线，动作区
是标准按钮，除 `LoadingIndicator` / `WavyProgressIndicator` 外没有新的 Expressive 组件。

> 字体/图标选型已定：**C（Roboto Flex 全局 Latin + Material Symbols Rounded，中文回退系统）**。
> 决策与落地清单见 `docs/design/type-and-icons.md`，ADR 见 `docs/adr/0002-type-and-icons.md`。
> 已由 #15 落地：字体换成 Roboto Flex 子集，图标换成 Material Symbols Rounded + `M3SymbolIcon`，
> `material-icons-extended` 已移除；实测与偏差见 `docs/design/type-and-icons.md` 的「落地结果」。

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
| 歌单 / 专辑 / 歌手详情（`CollectionDetailScreen`） | 沉浸式方形 hero + 三个动作按钮 + 介绍 + 72dp 列表 | hero 标题只有 28sp；列表有分割线；动作区是三个独立控件；正文没有色面 | P0（试点 1） |
| 首页（`HomeScreen`） | 问候语 + 搜索/设置 + 三组等权卡片网格 | 没有 hero；标题层级平；无 FAB / 色面；横向卡片只是普通 M3 | P0（试点 2） |
| 播放页（`PlayerScreen`） | 封面 + 标题/收藏 + 波浪进度 + 大圆角播放键 | 运输控件仍是标准图标；没有浮动 toolbar；标题偏小；封面外没有色面 | P0（试点 3） |
| 音乐库（`MyMusicScreen`） | 两行入口 + 歌单列表 | 入口是标准 ListItem；没有 hero / 色面；与首页语言不一致 | P1 |
| 全部歌曲 / 搜索 / 文件夹 / 专辑 / 歌手 | 标准列表 + 顶栏 + 多选 | 分割线与 72dp 行是默认 M3；没有 expressive moment | P1 |
| 设置 | 分组列表 + Switch | 功能优先，可保持安静；只在标题/分组间距上跟随 | P2 |
| 歌曲详情 / AI 解读 / 歌词 | 表单或沉浸式文本 | 歌词已经是全 App 最接近 Expressive 的一屏；歌曲详情偏表单 | P2 |

## 试点计划

### 试点 1：CollectionDetail 外壳（影响歌单、专辑、歌手三页）— 已落地（色面部分按 #6 规则修订，落地在 #18）

- hero 标题升到 `displaySmallEmphasized`，副标题用 label 级字距，形成“海报 → 说明”的跳级。
- 列表去掉分割线，行距交给留白；保留 72dp 行高与拖拽编辑态。
- 动作区合并为一个 tonal 控制簇（shuffle / 播放全部 / 添加到歌单），作为该屏的受限色面（secondaryContainer；#6 规则）。
- hero 用封面派生的 primaryContainer 光晕，到封面下沿收干净；正文从 surface 开始；动作簇用 secondaryContainer 补一块受限色面（见 [color-field-shape-morph.md](color-field-shape-morph.md)）。
- 拖拽排序保留“编辑态才出现行尾把手”，并把抬起行做成带背景与阴影的浮层。

试运行截图见本轮对话；六条标记目前 1、2、3、5、6 达成，第 4 条由 `MotionScheme.expressive()` 的组件级弹簧承担，整屏转场的弹簧留到首页/播放页试点时统一。色面与 morph 部分待 #18 按 #6 规则复核。

### 试点 2：首页

- `LargeFlexibleTopAppBar` 承载问候语与时间；搜索/设置降为 actions。
- 「最近播放」改成左大右小的非对称 hero 行，而不是三张等权卡片。
- 引入一个 FAB menu（AI 生成歌单 / 随机播放 / 扫描），放在迷你播放器与底栏之上；底部留白沿用 `SonLayout`。
- 分区标题升到 `titleLargeEmphasized`，卡片圆角与色面跟着详情页的语法走。

### 试点 3：播放页

- 封面背后加同色系 hero 光晕（B 规则，见 [color-field-shape-morph.md](color-field-shape-morph.md)），封面本身保持大圆角。
- 运输控件改成浮动 toolbar（shuffle / prev / play / next / repeat），播放键做 shape morph。
- 标题升到 `headlineSmallEmphasized`，歌手名用 label 级字距。
- 歌词入口、收藏、更多收进一个轻量 toolbar，减少右上角图标堆叠。

## 技术约定

- 动效只从 `MaterialTheme.motionScheme` 取，场景与档位见 `docs/design/motion.md`（决策见 ADR-0003）；唯一的时序例外是歌词自动滚动与伴奏行进出，不再新增裸 `tween` / `spring` 参数。
- 字号、间距、尺寸只从 `SonTypography` / `SonSpacing` / `SonSizes` / `SonLayout` 取。
- 颜色只从 `MaterialTheme.colorScheme` 或 `LocalSonExtendedColors` 取；色面规则见 [color-field-shape-morph.md](color-field-shape-morph.md)（`primaryContainer` 只做 hero 光晕，`secondaryContainer` 只做受限动作簇；ADR-0004），不手写十六进制。
- 组件直接使用 `androidx.compose.material3`；`core/ui/components/` 只放 Material 没有的组合（当前只有 MediaCards 与底栏）。
- 每屏改完在真机上截图，对照六条标记逐条勾选；不达标不算完成。
