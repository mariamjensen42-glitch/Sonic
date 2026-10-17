# 色面与 shape morph 规则

- 状态：已决策（2026-10-17）
- 决策票：[#6 色面与 shape morph 规则](https://github.com/mariamjensen42-glitch/Sonic/issues/6)
- ADR：[0004-color-field-and-shape-morph.md](../adr/0004-color-field-and-shape-morph.md)
- 原型：[color-field-shape-morph.html](prototypes/color-field-shape-morph.html)（分支 prototype/color-field-shape-morph，原型 commit 32eb34e）
- 样张：prototypes/color-field-shape-morph-{A,B,C,C-secondary,D,E,B-gray,B-dark,B-folded}.png
- 上位文档：[expressive-audit.md](expressive-audit.md)、ADR-0001、ADR-0002、ADR-0003、根目录 CONTEXT.md

## 结论

**B 做底**：hero 周围只有一层封面派生的 **primaryContainer** 光晕，到封面下沿收干净；标题、正文、列表从 **surface** 开始。CollectionDetail 保留一个**被限制在 action cluster** 的 **secondaryContainer** 色块，补“一块饱和色面”的 moment，不碰标题与列表。**不做 C 的整块、D 的全屏洗、E 的无色面**。近灰封面（种子 HCT chroma < 16）色相兜底品牌色。

这条规则同时回答三件事：色面用哪个角色、边界在哪、morph 落在哪。原型里的 A/C/D/E 作为被否方案保留，用于回看为什么不是它们。

## 1. 色面

### 1.1 角色

| 元素 | 角色 | 边界 / 说明 |
| --- | --- | --- |
| hero 光晕 | **primaryContainer**，主题相关 alpha | 唯一大面积色面；只到 hero 下沿，不到标题 / 列表 |
| 安静面 | **surface** | 标题、正文、列表、运输控件 |
| 动作簇 | **secondaryContainer** | 唯一非 primary 色块；只包 shuffle / 播放全部 / 添加 |
| 播放键 FAB | **primaryContainer** | 与色面角色区分；不叠同角色实心块 |
| 选中 / 当前行 | **secondaryContainer** | 现有语义；不得扩成页面级 field |
| surfaceContainer* | 组件容器 | dialog / sheet / card 等；不作为色面 |

规则只有一句：**primaryContainer 只做 hero 光晕，secondaryContainer 只做受限动作簇；其余 surface。** 页面里不出现 Color(0x...)，也不手写 alpha 常量——数值收进主题层 token（#18）。

### 1.2 几何与边界

光晕由两层叠成，都不使用 blur：

| 层 | 参数 | 暗色 | 亮色 |
| --- | --- | --- | --- |
| 径向 | 92% 50% at 50% 23% | 0.62 → 0.30 @55% → 0 @60% | 0.70 → 0.34 @55% → 0 @60% |
| 纵向 | 0 → 24% → 46% → 53% | 0.12 → 0.30 → 0.10 → 0 | 0.18 → 0.46 → 0.16 → 0 |

- 53% 就是 Player 封面下沿；标题在约 55% 起。**光晕必须在标题第一个基线之前归零**，标题、歌手、进度、运输控件全部落在 surface 上。
- CollectionDetail 的 hero 是通栏方形：光晕主要被封面遮住，可见部分在 top bar 与封面下沿；下沿的 fade 允许最多 12dp 进入动作区，**不进入首行歌曲**。样张里的 210px → 222px 就是这条 12dp。**#18 采用字段表的纵向峰值 0.46（亮）/ 0.30（暗）作为这 12dp 的起点**，与 1.3 的字段表一致；prototype 的 0.58/0.42 不采用。色面画在 hero list item 内，surface 只填 hero 区，不遮/不染下面的动作区与歌曲。
- top bar / 状态栏位于光晕内；保持 top bar 透明，图标用 onSurface / onSurfaceVariant，不额外加 scrim。暗色顶栏会被染成深色封面调，亮色顶栏会被染成浅容器调；两套都要在真机上复看状态栏图标。
- **#18 记录的上栏例外**：CollectionDetail 的 top bar 直接压在封面上，折叠前图标/标题用 onArtwork，surface band 变实后过渡到 onSurface / onSurfaceVariant。这是对上面 onSurface/onSurfaceVariant 规则的唯一例外，原因是封面颜色任意、onSurface 在深色封面上不可读；页面仍只从 MaterialTheme.colorScheme / LocalSonExtendedColors 取色。
- 禁止 blur / RenderEffect / 大范围 graphicsLayer 模糊；光晕是渐变，不是毛玻璃。
- 禁止硬边：C 的 32dp 圆角整块是被否方案，不在 B 里复活。

### 1.3 明暗数值与对比度

| 对 | 亮色 | 暗色 | 阈值 |
| --- | --- | --- | --- |
| 标题 onSurface / surface | 18.97 | 15.41 | ≥ 3.0（大字号） |
| 次要文字 onSurfaceVariant / surface | 12.64 | 11.15 | ≥ 4.5 |
| 动作簇文字 onSecondaryContainer / secondaryContainer | 15.19 | 10.21 | ≥ 4.5 |
| 播放键图标 onPrimaryContainer / primaryContainer | 13.07 | 9.36 | ≥ 4.5 |

- 数值来自原型的 OKLCH 近似渲染（洋红封面样本）；实现用真实 HCT 后由 #18 重新测量。余量足够，阈值不是擦线过的。
- 亮色 primaryContainer 是 tone 90，surface 是 98，只差 8 个 tone；**亮色 alpha 必须明显高于暗色**。暗色 0.30 就够，亮色要 0.46。不要为了统一写一个 alpha。
- 光晕最深处在封面背后（被不透明封面遮住），文字处已经归零；对比度表取的是文字实际落点，不是最深处。

### 1.4 封面裁剪

- **近灰封面**：ArtColorExtractor 仍取最有彩度的 swatch，并把彩度抬到至少 12；但色相不可靠。规则改为 **种子 HCT chroma < 16 时，色相兜底品牌色 SonicSeed**，其余照旧。原型 cover=gray 就是这条规则（灰封面 + 品牌紫调）。阈值 16 是起点，#18 用真实封面库校准；调整只动一个常量。
- **阈值校准记录（#18，2026-10-18）**：阈值保留 16；单元测试覆盖 15.999 / 16.0 / 48.0；常量是 SonTonalScheme.GRAYSCALE_CHROMA_THRESHOLD，提取器的最低彩度提升 12 不变。Agent 不能跑真机/封面库，设备测量由用户完成：取验收截图集里的近灰、深色、奶油、霓虹四类封面各 3–5 张，跑 scripts/ui-verify.ps1 出图；抽一张封面读 Hct.fromColor(seed).chroma（可在 debug 日志或临时单测里打印）。若 16–20 的灰封面仍出现随机色相，调高阈值；若低彩度彩色封面丢掉自己的色相，调低阈值。每次只改这一个常量并重跑 SonTonalSchemeTest。
- **过饱和封面**：不直接把 swatch 原始色铺到界面。SonTonalScheme 的 primary chroma 固定 48，页面只用角色色；饱和度由 role + alpha 决定，不由封面原图决定。光晕 alpha 上限 0.70（亮）/ 0.62（暗），不再往上加。
- **过暗封面**：hero 标题/控件的可读性由 artworkScrim（两套主题都是深色）负责，不由光晕负责；光晕的 tone 来自 scheme，不随封面明度变暗。B-dark 样张展示这条：深蓝封面 + 浅青容器光晕。
- 封面本身不裁切、不重新着色；裁剪只发生在 palette 的 hue/chroma 入口。

### 1.5 动作簇

- CollectionDetail 的 CollectionActions 容器从 surfaceContainerHigh 改为 **secondaryContainer**，内容色 **onSecondaryContainer**；播放全部按钮仍是 **primary / onPrimary**。
- 形状保持现有 CircleShape（动作簇的胶囊）；不新增形状，不手写 RoundedCornerShape。
- 尺寸 48–56dp 高；只包三个动作，不扩成整行，不覆盖列表。
- 动作簇是 CollectionDetail 的；Player 没有动作簇，Player 的饱和 moment 由封面本身 + 光晕承担。如果 #20 的验收认为 Player 仍然弱，只能在 Player 的方案里加一个受限的 secondaryContainer 块，不允许改成整块 primaryContainer 或全屏洗。
- **#18 落实**：CollectionActions 的 container/content color 是参数，默认 surfaceContainerHigh / onSurfaceVariant；只有 CollectionDetail 显式传 secondaryContainer / onSecondaryContainer。12dp 下沿 fade 是 CollectionDetail 的包装，不放进共享组件，因此 MyMusic 不会拿到第二块色面或 fade。

### 1.6 安静面

- 标题、正文、列表、运输控件全部 surface；CollectionDetail 旧的“primaryContainer 0.32 → surfaceContainerLow → surface”长渐变删除，列表不再被色面染色。
- 列表继续无分割线；行距与留白不变。
- 除动作簇外，页面里没有第二块色面；队列当前行、选中态仍用 secondaryContainer，但它们是组件状态，不是页面色面。

## 2. Shape morph

### 2.1 官方内置（直接用，不包壳）

| 组件 | 形变 |
| --- | --- |
| Button / FilledTonalButton | 官方按压形变 + ripple |
| IconButton / FAB | 官方按压反馈；播放键的额外形变见 2.2 |
| ListItem / SegmentedButton / Chip | 官方选中/按压 |
| 底部导航 / 迷你播放器 | 底部导航：标准 NavigationBar（#5 已定），无容器 morph；迷你播放器：悬浮卡片，不做 shape morph |

这些不需要主题层新能力。禁止写 M3Button / M3IconButton 之类的转发壳，也禁止 indication = null。

### 2.2 主题层（只有两处）

1. **播放键按压**：LargeFloatingActionButton 默认 28dp 圆角（MaterialTheme.shapes.extraLarge）↔ CircleShape，仅按压时形变，松手回弹；播放/暂停切换只换图标，不改形状。用 MaterialTheme.motionScheme.fastSpatialSpec()，不写裸 spring/tween；原型里的 220ms 是 CSS 近似，不是实现参数。
2. **hero 折叠**：封面在 hero list item 内连续改变尺寸、圆角（0 → 8dp，MaterialTheme.shapes.small）、位置；由滚动进度驱动，不是定时动画；顶栏缩略图/标题沿用原有 crossfade。标题保持固定字体、固定在 hero 底部，只做 alpha crossfade，不参与 scale/clip。**#18 限制**：hero 仍在 LazyColumn 的 item 内；真正的 shared-element fold（封面离开 item 落进 top bar 缩略图）需要 overlay + 每帧布局，会与本屏的折叠顶栏/列表重组路径冲突，本 ticket 采用 closest honest behavior，并在验收报告里记录。原型里的“播放折叠”只是检查手感的按钮，生产里没有这个按钮。

底栏按 #5 已定的标准 NavigationBar（无 FAB、无容器 morph）；迷你播放器保持悬浮卡片，不参与本规则的 shape morph。

### 2.3 禁止

- 悬停 / 桌面 hover 形变；滚动时非结构性的缩放；呼吸 / 浮动装饰；状态切换触发的装饰形变。
- 页面里手写 RoundedCornerShape(n.dp)、Color(0x...)、indication = null。
- 裸 tween/spring 参数；动效只从 MaterialTheme.motionScheme 取（歌词例外见 ADR-0003）。
- 为了“像 Expressive”给每个控件都加形变；一个屏幕最多一个主题层 morph moment。

## 3. 六条验收怎么用这套规则

| 标记 | 由什么承担 |
| --- | --- |
| 1 超大 Emphasized 标题 | CollectionDetail 现有 displaySmallEmphasized；Player 由 #9/#20 定 |
| 2 一块真正饱和的色面 | 动作簇 secondaryContainer；hero 光晕是第二层 |
| 3 按下会形变的主控件 | 播放键按压 |
| 4 有物理感的转场 | MotionScheme spatial；hero 折叠由滚动驱动 |
| 5 Expressive 组件 / 控制簇 | 动作簇（共享容器） |
| 6 正文是 quiet surface | surface；无 field；列表无分割线 |

本规则直接负责 2、3、4、6；1、5 由各屏方案决定，但不能再引入第二块色面或第二处主题层 morph。

## 4. #18 的实现边界

### 4.1 主题层

- 新增 **SonColorField**（core/ui/theme）：暴露 hero 光晕的 Brush / Modifier，参数只有 colorScheme、dark、heroBottom；颜色取 primaryContainer，alpha 取主题 token；页面不再拼 Brush。
- 新增 **SonShapeMorph**（core/ui/theme）：暴露 pressShapeMorph（FAB 用）与 hero fold 的 shape 插值；不新增 Material 转发壳。
- SonTonalScheme.from / ArtColorExtractor：近灰阈值改为 16，兜底品牌色相；这是唯一允许改 palette 入口的地方。
- 不新增颜色 token、不新增 Shape token：播放键用现有 extraLarge ↔ CircleShape，hero 用 RectangleShape ↔ small。

### 4.2 CollectionDetail

- 删除 List 背景的旧四段渐变，背景改为 surface。
- 光晕在 hero item 内绘制（SonColorFieldBackground），surface 只填 hero 区；cover 收缩时从四周露出；下沿 12dp fade 由 CollectionDetail 包装动作区，共享 CollectionActions 不画。
- top bar 保持 CenterAlignedTopAppBar + 32dp 缩略图/标题 crossfade；surface band 负责滚动态底色。
- hero 折叠在 item 内做尺寸/圆角/位置插值；标题 fixed font + alpha；顶栏缩略图仍是 crossfade（见 2.2 的限制）。
- CollectionActions 容器参数由页面传入 secondaryContainer / onSecondaryContainer；默认仍是中性。
- 播放全部仍 primary；保留现有 hero 标题、动作簇、列表留白与拖拽排序。

### 4.3 播放页（#9 / #20）

- 封面 16dp 边距内加光晕；标题起 surface；运输控件不变。
- 播放键 FAB 的 shape morph 由 SonShapeMorph 提供；不改变现有 96dp 尺寸。
- Player 没有动作簇，不要为了六条硬塞一个。

### 4.4 灰封面

- 在 SonTonalScheme.from 里做 chroma < 16 → 品牌 hue；不要在页面里判断。
- 校准方法与记录见 1.4；本 ticket 只固定阈值 seam，设备测量由用户完成。

### 4.5 验证

- 真机明暗两套截图：B 样张、灰封面、深色封面、折叠态。
- 对比度表重算；光晕文字落点、动作簇、播放键三组。
- 色面与 FAB 的角色互斥检查：任何新页面如果要用 primaryContainer 实心块，必须同时改 FAB 角色或改用 secondaryContainer 受限块。
- CollectionDetail 的顶栏在封面上的图标颜色按 1.2 的例外复看；如果 onArtwork 在亮色封面上不够，调 hero scrim，不要改回页面自拼色。
- 12dp 下沿 fade 取纵向峰值 0.46/0.30；截图里确认首行无染色。
- 构建/测试由用户执行；Agent 只做静态检查。

## 5. 被否的方案

| 方案 | 结论 | 原因 |
| --- | --- | --- |
| A 无色面 | 否 | 六条第 2 条过不了 |
| C 整块 primaryContainer | 否 | FAB 默认同角色，播放键形状消失；quiet surface 被吃掉 |
| C 整块 secondaryContainer | 否 | FAB 回来了，但整块仍覆盖首行；quiet 不过 |
| D 全屏洗 | 否 | 整屏 tint，阅读疲劳；OLED 大面积亮色 |
| E 中性强调 | 否 | 最安静，但丢掉“封面即主题”，第 2 条也弱 |
| 旧 B（渐变延伸到列表） | 否 | 色面进入首行，quiet 不过；正是现状 CollectionDetail 的做法 |

C 的 secondaryContainer 开关是这次最有价值的反例：它证明“换角色”能救 FAB，但救不了整块色面。最终把色块缩到动作簇，才同时满足 FAB 与 quiet。

## 6. 不在本规则内

- 底栏 / 迷你播放器容器（#5 已定：标准 NavigationBar + 悬浮迷你播放器；不做容器 morph）。
- 首页、播放页的构图与信息层级（#8 / #9 / #19 / #20）。
- 自适应 / 平板 / 横屏 / 系统面。
- 歌词的高亮与自动滚动（已有规则）。

## 7. 样张

- B 明暗：prototypes/color-field-shape-morph-B.png
- B 灰封面：prototypes/color-field-shape-morph-B-gray.png
- B 深色封面：prototypes/color-field-shape-morph-B-dark.png
- B 折叠态：prototypes/color-field-shape-morph-B-folded.png
- 被否的 A/C/C-secondary/D/E 同目录；原型可切 URL 参数复看。

原型留在 throwaway 分支（连同本规则作为一手材料）；主分支只保留规则与 ADR，页面代码按 #18 落地。
