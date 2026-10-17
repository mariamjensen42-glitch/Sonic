# 色面只托住 hero，morph 只落在按压与折叠

**状态**：Accepted（2026-10-17）。决策票 [#6 色面与 shape morph 规则](https://github.com/mariamjensen42-glitch/Sonic/issues/6)。原型分支 prototype/color-field-shape-morph（原型 commit 32eb34e）。规则 [docs/design/color-field-shape-morph.md](../design/color-field-shape-morph.md)。

## Context

全 App 的封面取色已经由 SonTonalScheme 展开成完整 M3 配色，但页面层还没有色面规则：Player 完全没有色面，CollectionDetail 用一条从 hero 下沿一直渗到列表的 primaryContainer 0.32 → surfaceContainerLow → surface 长渐变。六条验收要求每屏有一块真正饱和的 container 色面，同时正文必须是 quiet surface；这两条在“色面放哪、放多大”上直接冲突。

另外，Material 的 LargeFloatingActionButton 默认就是 primaryContainer。任何用 primaryContainer 做实心色块的页面都会让播放键形状消失（#6 原型的 C 变体实测），这不是颜色偏好问题，是角色冲突。

shape morph 也需要边界：Material 组件自带按压反馈，但 hero 封面折叠和播放键形变需要主题层能力；如果不限制，会滑向每个控件都加形变。

## Decision

- **B 做底**：hero 周围只有 primaryContainer 光晕（径向 + 纵向两层），到封面下沿收干净；标题、正文、列表用 surface。亮色 alpha 0.46 / 0.70，暗色 0.30 / 0.62；文字处必须归零。
- **动作簇 secondaryContainer**：CollectionDetail 的三个动作收在一个 secondaryContainer 胶囊里，补一块受限的饱和色面；不覆盖列表，不动 FAB 的 primaryContainer。
- **近灰封面兜底品牌色**：种子 HCT chroma < 16 时用 SonicSeed 的色相；阈值由 #18 校准。
- **morph 只有两处主题层能力**：播放键按压 28dp ↔ CircleShape；hero 折叠（尺寸/圆角/位置连续插值，滚动驱动）。其余交给 Material 组件内置；底栏按 #5 已定标准 NavigationBar（无 FAB、无容器 morph），迷你播放器保持悬浮卡片。
- 页面不写颜色、不写形状；数值进主题层（#18）。

## Considered Options

| 方案 | 结果 |
| --- | --- |
| A 无色面 | 否：第 2 条过不了；作为基线保留 |
| B 只托 hero | 选中；加动作簇后同时过 FAB 与 quiet |
| C 整块 primaryContainer | 否：FAB 同角色，播放键形状消失；quiet 被吃掉 |
| C 整块 secondaryContainer | 否：FAB 回来，整块仍覆盖首行 |
| D 全屏洗 | 否：阅读疲劳；OLED 大面积亮色 |
| E 中性强调 | 否：没有色面，丢掉封面即主题 |
| 旧 B：渐变到列表 | 否：色面进入首行；现状 CollectionDetail 的做法 |

C 的 secondaryContainer 开关是测试里最有价值的反例：换角色能救 FAB，救不了整块色面。最终把色块缩到动作簇。

## Consequences

- CollectionDetail 的旧渐变会被删除，列表回到 surface；动作簇换角色。这是可见的观感变化，需要用 #10 的六条复核对齐。
- Player 会第一次出现色面（hero 光晕）；#9/#20 的构图方案可以直接用，但不要为了六条再塞动作簇。
- 亮色 alpha 明显高于暗色；不要合并成一个常量。顶栏/状态栏会被染色，必须在真机上复看图标。
- 近灰封面会得到品牌紫调；这是接受的代价（稳定 > 贴近封面）。
- 底栏与迷你播放器按 #5 的决定保持标准 NavigationBar + 悬浮卡片，不纳入本规则的 morph 候选。
- 对比度数值来自 OKLCH 原型，HCT/Compose 的真实值由 #18 重测；余量足够。
- 原型留在 throwaway 分支，主分支只保留规则与 ADR；#18 负责主题层能力与最小验证。
