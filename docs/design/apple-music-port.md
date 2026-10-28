# Apple Music 设计层（core/ui/apple）

对标样本：`com.apple.android.music` 7.0.0-beta（versionCode 1611）。
所有数值由 APK 资源表静态提取，不是目测：`aapt2 dump resources` 解析出的
`color / dimen / style` 条目，逐条对应到本项目的 Compose 常量。

实现位置：

| 文件 | 职责 |
|---|---|
| `core/ui/apple/AppleTokens.kt` | 色板、字阶、尺寸、SF Pro 字体族 |
| `core/ui/apple/AppleComponents.kt` | 大标题、分区标题、卡片、列表行、菜单行、分隔线、封面拼图 |
| `core/ui/apple/AppleMiniPlayer.kt` | 48dp 胶囊迷你播放器 |
| `presentation/navigation/AppleBottomNavigationBar.kt` | 底部 Tab 条（含 11sp 标签与选中色） |
| `presentation/home/HomeScreen.kt` | 首页（Listen Now 结构） |
| `presentation/mymusic/MyMusicScreen.kt` | 资料库（列表段已替换为 Apple 行结构） |
| `presentation/components/CollectionDetailScreen.kt` | 专辑 / 歌手 / 歌单详情的头部文案与动作条 |
| `presentation/components/SongComponents.kt` | `SongListItem` 改为 Apple 曲目行（全应用生效） |
| `presentation/player/MiniPlayer.kt` | `Song` → `AppleMiniPlayer` 的数据适配薄壳 |

---

## 1. 这是 AGENTS.md 的显式例外

`AGENTS.md` 的「设计体系」要求：颜色只取 `MaterialTheme.colorScheme`、排版只取
`MaterialTheme.typography`（MiSans）、形状只取 `MaterialTheme.shapes`。Apple Music
的观感依赖 SF Pro、iOS 语义色板与非 Material 的组件几何，两者无法同时满足。

**取舍**：按用户要求做像素级复刻，例外范围收束到 `core/ui/apple/` 一个包，
以及引用它的两个页面。其余页面继续走 Material 3 Expressive，不受影响。

回滚方式：把两个页面里对 `AppleTheme` / `Apple*` 组件的引用换回原组件即可，
令牌层没有污染 `core/ui/theme/`。

**许可提示**：`res/font/sf_pro_android_ui.ttf` 是 Apple 的专有字体，随本源码一起
分发存在授权风险。若要公开发布，把它换成 Inter 或保留 MiSans，并调整
`AppleFontFamily` 即可，其余令牌不受影响。

---

## 2. 色板（`AppleColors`）

来源 `res/values/colors.xml`，按 `default` / `night` 两个配置解析到底
（`@color/xxx` 引用链已展开，框架色 `0x0106000b = white`、`0x0106000c = black`）。

| 令牌 | 资源名 | 日间 | 夜间 |
|---|---|---|---|
| background | `background_color` | `#FFFFFF` | `#000000` |
| backgroundLayer1 | `background_color_layer1` | `#FFFFFF` | `#1C1C1E` |
| secondaryBackground | `secondary_background_color` | `#F2F2F7` | `#1C1C1E` |
| tertiaryBackground | `tertiary_background_color` | `#FFFFFF` | `#2C2C2E` |
| label | `label_color` | `#000000` | `#FFFFFF` |
| secondaryLabel | `secondary_label_color` | `#3C3C43` @60% | `#EBEBF5` @60% |
| tertiaryLabel | `tertiary_label_color` | `#3C3C43` @30% | `#EBEBF5` @30% |
| quaternaryLabel | `quaternary_label_color` | `#3C3C43` @18% | `#EBEBF5` @18% |
| separator | `separator_color` | `#C6C6C8` | `#38383A` |
| systemRed | `system_red` | `#FF3B30` | `#FF3B30` |
| key | `key_color` | `#0088FF` | `#0091FF` |

`systemRed` 是 iOS 的系统红，也是 Apple Music 品牌红的近似值。

---

## 3. 字阶（`AppleType`）

来源 `res/values/styles.xml` 中带 `android:textSize` 的文本样式。字号为实测值，
行高与字距 Apple 未写进资源，按 iOS HIG 补齐（表中标 *）。

| 令牌 | 对应样式 | 字号 | 字重 |
|---|---|---|---|
| largeTitle | `SystemFont40dp` | 34sp | Bold |
| title0 | `Title0` | 32sp | Bold |
| title1 | `Title1` | 28sp | Regular |
| title2Emphasized | `Title2ShortEmphasized` | 22sp | Bold |
| sectionTitle | `MusicTitle2` | 20sp | Bold |
| title2 | `Title2` | 20sp | Regular |
| headline | `Headline` | 17sp | Medium |
| body | `Body` | 17sp | Regular |
| subhead | `Subhead` | 15sp | Regular |
| footnote | `Footnote` | 13sp | Regular |
| caption1 | `Caption1` | 12sp | Regular |
| caption2 | `Caption2` | 11sp | Regular |

`MusicTitle2` 就是分区标题（「最近播放」「推荐专辑」）用的那一档 —— 20sp Bold，
颜色 `label_color`。注意它**不是** 17sp：手机上的分区标题比正文大一档。

字体：`res/font/sf_pro_android_ui.ttf` 是**单个可变字体**，Apple 用 77 个字体 XML
（`regular.xml` / `medium.xml` / `bold.xml` …）通过 `fontVariationSettings` 的
`'wght'` 轴切分字重。本项目用 `FontVariation.Settings(FontVariation.weight(w))`
复现同一机制，只打包一个 ttf。

CJK 字形不在 SF Pro 里，回落系统字体 —— Apple Music 自己也是这样。

---

## 4. 尺寸（`AppleDims`）

来源 `res/values/dimens.xml`。

| 令牌 | 资源名 | 值 |
|---|---|---|
| cornerXs / S / M / L / XL / XXL / pill | `corner_radius_xs/s/m/l/xl/xxl/pill` | 3 / 5 / 7 / 10 / 14 / 24 / 80 dp |
| padding | `default_padding` | 16dp |
| topMargin | `default_top_margin` | 32dp |
| listItemHeight | `default_list_item_height` | 48dp |
| largeListImage | `large_list_image_height` | 88dp |
| gridGutter | `dynamic_grid_item_spacing_gutter` | 10dp（360dp 宽屏 12dp） |
| gridPeek | `dynamic_grid_peek` | 36dp |
| sectionTitleTop / Bottom | `header_section_a` 的 padding | 13 / 10 dp |
| menuRowVertical / End | `large_list_a_item` 的 padding | 13 / 20 dp |
| menuRowGap | `default_padding` | 16dp |
| trailingIcon | `list_action_icon_width` | 26dp |

---

## 5. 组件解剖（对照原始 layout）

### 分区标题 `header_section_a`

```
RelativeLayout
├── View(style=Divider)                    高 line_divider_stroke
└── LinearLayout(paddingTop=13dp, paddingBottom=10dp, alignParentStart)
    ├── LinearLayout(weight=1, vertical)
    │   ├── CustomTextView(style=MusicTitle2)   20sp Bold
    │   └── CustomTextView(第 2 行)             副标题
    └── (行尾) 展开入口
```

对应 `AppleSectionHeader(title, subtitle, onClick)`：有 `onClick` 才画行尾尖括号。

### 图标菜单行 `large_list_a_item`

```
FrameLayout(foreground=selectableItemBackground)
└── LinearLayout(vertical)
    ├── View(style=Divider)
    └── LinearLayout(paddingTop/Bottom=13dp, paddingEnd=20dp)
        ├── ViewStub          前置图标，marginEnd=16dp
        ├── CustomTextView(style=LargeListAText)  Title2 20sp + color_primary
        ├── Space(weight=1)
        └── ImageView(26×26dp) 行尾图标
```

对应 `AppleMenuRow(icon, label, onClick)`：图标红色、文字 20sp、行尾 26dp 尖括号。
`LargeListAText` 继承 `Title2`（20sp）并覆盖成 `color_primary`。

### 资料库封面行 `large_list_a_item_library`

`paddingEnd=16dp`、`paddingBottom=10dp`，封面与行同高 88dp，选中态用
`selectableItemBackground`。对应 `AppleLargeListRow`。

### 横滑卡组 `header_a_swiping_grid_a`

```
LinearLayout(vertical)
├── include(layout/header_section_a)      ← 分区标题
└── AbsolutePeekCarousel                 ← 横滑，行尾露出 dynamic_grid_peek
```

对应 `AppleCarousel` + `AppleCard`：左右留白 16dp、间距 10dp、行尾露出 36dp。

### 首页根 `fragment_listen_now`

```
FrameLayout
├── BlurTarget                            ← 背景模糊层
├── SwipeRefreshLayout
│   └── ScrollInterceptingEpoxyRecyclerView   ← Epoxy 驱动的信息流
├── ComposeView(gravity=bottom)           ← 底部吸附区
└── include(layout/view_loader_include)   ← 加载态
```

信息流是 Epoxy 模型拼的，条目级文字样式绑在 Java 侧而不是 layout 里，
因此**卡片标题的字号（15sp）与副标题（13sp）是推断值**，其余数值均已证实。

### 迷你播放器 `mini_player`

```
FrameLayout
└── ConstraintLayout(background=bg_apple_material, height=48dp,
│                   marginStart/End=16dp, maxWidth=600dp, paddingStart/End=16dp)
    ├── CardView(30dp, radius=5dp)          ← 封面
    │   └── NowPlayingContentView
    ├── LinearLayout(vertical, marginStart=8dp)
    │   ├── TextView(style=Caption1Emphasized)  12sp Medium，带前置图标
    │   ├── TextView(style=FootnoteEmphasized)  13sp Medium
    │   └── TextView(style=Caption1)            12sp
    └── ConstraintLayout
        ├── TintableImageView(32dp)
        └── TintableImageView(32dp, marginStart=8dp)
```

`bg_apple_material` 是两层 shape 的 layer-list：`layer_artwork_color_scheme`（可被封面染色的底）
叠在 `layer_apple_material` 上 —— 后者 `solid=apple_material_background`、
`corners radius=corner_radius_pill(80dp)`、`stroke 0.5dp=apple_material_stroke`。
材质色：日间 `#F5F5F7` / 夜间 `#262628`；描边：日间 `#1A000000` / 夜间 `#33FFFFFF`。

**关键点：胶囊形，且布局里没有进度条。** 原实现的进度只在播放页。

对应 `AppleMiniPlayer`：48dp 胶囊、16dp 外边距与内边距、30dp 封面、两个 32dp 控件、
`apple_material_background` 实色底 + 0.5dp 描边（毛玻璃落成实色，不上 RenderEffect）。

### 底部 Tab 条 `bottom_navigation`

```
ConstraintLayout(gravity=bottom)
└── FrameLayout(elevation=4dp, constraintBottom_toBottomOf=parent)
    ├── BottomNavigationView(background=background_color_layer1,
    │                        itemIconTint/itemTextColor=selector_bottom_navigation_text,
    │                        itemTextAppearance=BottomNavigationView[.Active],
    │                        labelVisibilityMode=labeled)
    └── View(1dp, style=Divider)          ← 顶部发丝线
```

`selector_bottom_navigation_text`：`state_checked=true → key_color`，否则 `label_color`。
标签字号 `bottom_navigation_view_textsize` = **11sp**。

对应 `AppleBottomNavigationBar`：等宽 Row、56dp 内容高、11sp 标签、
选中 `key_color`（日间 `#0088FF` / 夜间 `#0091FF`）、顶部 0.5dp 半透明分隔线，
以及 `windowInsetsPadding(WindowInsets.navigationBars)` 处理边到边。
迷你播放器与栏之间留 8dp（`miniplayer_to_navtabs_spacing`），间隙透明。

---

### 专辑 / 歌手详情 `collection_page_list_item` + `button_group_play_shuffle`

曲目行（原版 48dp，`default_list_item_height`）：

```
ConstraintLayout(height=48dp, background=selectableItemBackground)
├── ImageView         播放态音柱 / 曲目序号（min 24dp）
├── 右侧一组：歌词、下载进度、下载完成图标（每枚 24~32dp）
├── Barrier(End)      把上面这排的最大宽度作为文字起点
├── TextView(style=SmallListATitleStyle)     17sp，color=collection_label_text
├── TextView(style=SmallListASubTitleStyle)  12sp，secondary_label_color（默认 gone）
├── ImageView(32dp)   时长 / 下载
└── 行尾 26dp 更多按钮
```

动作条（`button_group_play_shuffle_collection` → `button_group_play_shuffle_buttons`）：
顶部 `Divider` + 内边距 16dp 的容器，里面是「播放 / 随机播放」两个胶囊按钮。

本项目的落地：`CollectionSongRow` → `SongListItem`（48dp、17sp/12sp、26dp 更多按钮），
`CollectionActions` → 两个 40dp 胶囊（播放=品牌红实心，随机=fill 底）。

曲目表行首按原版实现为**序号**：`SongListItem(trackNumber = null)` 走封面行（资料库类列表），
传了序号就走 Apple 曲目表（专辑 / 歌单 / 歌手详情），**不再显示封面**。
当前曲目的序号换成 `ApplePlayingBars`（对应 `IndexDancingBarView`，24dp 宽、3 根柱子错相位）。

歌手页在共享屏之上多了两段：`headerExtra` 挂「热门歌曲」标题，`songsFooter` 挂「专辑」横滑
（数据来自 `AlbumRepository.getAlbumsByArtist`），点击专辑跳专辑详情。

## 6. 未覆盖的部分

- **搜索、全部歌曲、设置、均衡器、统计、排行榜、文件夹、在线源、AI 页**：已随第四批改为 Apple 组件（见 §10）。
- **`BlurTarget` 模糊背景**：需要 RenderEffect 方案，未实现；迷你播放器的材质层按实色落地。
- **Tab / 列表图标**：Material Symbols 替代 SF Symbols，语义一致但字形不是 Apple 的。
- **歌手页的「参与合作 / 关于」两段**：需要 `appearsOn` 查询与艺人简介数据，当前库中没有。
- **图表刻度与配色**：`ListeningCharts` / `PieChart` 只换了色与字号，几何仍是自绘 Canvas，未按 Apple 的统计页重排。

---

## 7. 搜索页（`fragment_landing_search`）

原版结构：

```
ConstraintLayout
├── BlurTarget                        ← 整页内容背景层
│   └── ExclusiveViewPoolEpoxyRecyclerView   ← 搜索结果流
├── ComposeView / Space / include      ← 顶部条与 tab
├── BlurView (search_view_background_height = 48dp)  ← 搜索框背后的真实模糊层
└── include 搜索栏 / 状态 / 空态
```

`header_section_a_recently_searched`：`Divider` + `TextView(style=MusicTitle2 20sp Bold)` + 次级行。

落地：

| 元素 | Apple 依据 | Sonic |
|---|---|---|
| 搜索框 | `search_view_background_height` 48dp，圆角 `corner_radius_l` 10dp | `AppleSearchField`（新组件，`BasicTextField` + fill 底） |
| 区块标题 | `MusicTitle2` 20sp Bold | `AppleType.sectionTitle` + `label` 色，13/10 内边距 |
| 历史 / 补全 | 胶囊填充 | 32dp 胶囊，`cornerPill` + `fill` 底 |
| 歌曲行 | `collection_page_list_item` 48dp | `AppleSongRow` |
| 专辑 / 歌手行 | 中列表 60dp | `AppleLargeListRow(imageSize = 56.dp)`，歌手走 `circular = true` |
| 歌词命中行 | — | 自定义行：标题 17sp + 命中片段 12sp（key 色，两行） |

**未做**：搜索框背后的 `BlurView`（需要 backdrop blur 依赖，与底栏玻璃同一个问题）。

---

## 8. 全站覆盖进度

### 统一骨架

- `AppleTopBar`（新）：状态栏间距 + 返回行 + 大标题。直接塞进 `Scaffold(topBar = ...)`，
  Scaffold 用它实际高度算 `innerPadding`，**页面内容一行都不用改**。
- `appleBottomBarClearance()`（新，`AppleInsets.kt`）：悬浮胶囊不占布局高度，
  列表底部要让出「胶囊 64 + 上下浮动 16 + 系统导航栏 inset」。已替换全站 10 处
  `SonLayout.bottomBarClearance`，修掉三键导航机器上最后一行被压住的问题。

### 页面状态

| 页面 | 顶栏 | 列表行 | 底部留白 |
|---|---|---|---|
| 首页 | ✅ 大标题 | ✅ Apple 行 | ✅ |
| 资料库 | ✅ 大标题 | ✅ 菜单行/88dp 封面行 | ✅ |
| 专辑 / 歌手 / 歌单详情 | ✅ 大标题 + 折叠头部 | ✅ 序号 + 音柱 | ✅ |
| 搜索 | ✅ 大标题 + `AppleSearchField` | ✅ | ✅ |
| 全部歌曲 | ✅ `AppleTopBar` | ✅（`SongListItem`） | ✅ |
| 专辑 / 歌手列表 | ✅ `AppleTopBar` | ✅ | ✅ |
| 文件夹 | ✅ `AppleTopBar` | ✅ | ✅ |
| 排行榜 / 听歌统计 | ✅ `AppleTopBar` | ⚠️ 图表区仍为 Material | ✅ |
| 设置（2294 行） | ✅ `AppleTopBar` | ✅ `AppleSettingsRow`（已上提到 `core/ui/apple`） | ✅ |
| 歌曲详情 / AI 解读 | ✅ `AppleCompactBar` / `AppleTopBar` | ✅ Apple 行 + 胶囊动作 | ✅ |
| 均衡器 / 在线源 | ✅ 由设置页提供 | ✅ Apple 行 + 胶囊 + 面板 | ✅ |
| 播放页 / 歌词页 | ✅ 见 `apple-player-spec.md` | — | — |

✅ 已对齐　⚠️ 仅统一底色与留白，结构与组件未换

---

## 9. 第三批：设置 / 均衡器·在线源 / 图表

### 设置页

原版设置项规格（`item_preference_title_description` / `item_preference_switch`）：

| 元素 | 实测 |
|---|---|
| 内边距 | 上下 16dp、左右 16dp |
| 标题 | **16sp**（`DefaultFont`） |
| 副标题 | **14sp**，`paddingEnd=16dp` |
| 右侧 | `SwitchCompat` 或箭头 |

落地：`AppleType.settingsTitle`(16sp) / `settingsSubtitle`(14sp) 两个新令牌；
`SettingsRow` 从 `ListItem` 改为 Apple 行（16dp 内边距、20dp 图标、12dp 图标文字间距）。
`SettingsNavigationRow` / `SettingsSwitchRow` 都走 `SettingsRow`，一次改全覆盖。
顶栏换成 `AppleTopBar`（标题按 `currentDestination` 切换、返回走 `NavigateBack`）。
`SettingsHeader` 的圆形图标底改 `fill`、文字改 Apple 字阶。

**遗留**：`ExcludedFolderItem` 与「音质说明」两行仍是 `ListItem`，位于子页面内。

### 均衡器 / 在线源

这两页**没有自己的顶栏** —— 它们是 `SettingsScreen` 的子目的地，chrome 由设置页统一提供，
所以顶栏跟着一起变 Apple 了。内部行组件仍是 Material。

### 图表（统计 / 排行榜）

`ListeningCharts.kt` + `PieChart.kt`：数据系列色 `colorScheme.primary` → `systemRed`（8 + 2 处），
刻度标签 `labelSmall` → `AppleType.caption1`。图表本身是自绘 Canvas，几何未动。

---

## 10. 第四批：二级页、弹层与共用件

### 新增共用件

| 文件 | 内容 | 替代的 Material 组件 |
|---|---|---|
| `core/ui/apple/AppleSurfaces.kt` | `AppleSheet` / `AppleSheetTitle` / `AppleSheetRow` / `AppleChip` / `AppleActionButton` / `AppleTextField` / `AppleAlert` | `ModalBottomSheet`（默认圆角与底色）、`FilterChip`、`Button`、`OutlinedTextField`、`AlertDialog` |
| `core/ui/apple/AppleSettings.kt` | `AppleSettingsRow` / `AppleSettingsNavRow` / `AppleSettingsSwitchRow` | 设置页原本的私有行实现（已搬到这里，设置页用 import alias 保持调用点不变） |
| `core/ui/apple/AppleTopBar.kt` | 新增 `AppleCompactBar` | `CenterAlignedTopAppBar`（编辑类页面的紧凑导航栏） |

面板几何：底色 `background_color`、上圆角 14dp、抓手 36×5dp 三级标签色、内容底部 16dp + 导航栏 inset。
弹窗：270dp 宽、14dp 圆角、按钮之间 0.5dp 发丝线、`key_color` 文字（破坏性动作 `system_red`）。

### 本轮改写的页面

| 页面 | 主要改动 |
|---|---|
| 歌曲详情（含编辑态） | `CenterAlignedTopAppBar` → `AppleCompactBar`；12 个 `OutlinedTextField` → 标签 + `AppleTextField`；播放/取消/保存 → `AppleActionButton`；丢弃确认 → `AppleAlert`（破坏性红字）；信息行改 `AppleType` + `AppleSeparator` |
| 歌词信息面板 | `AlertDialog` → `AppleSheet`（内容多，270dp 宽放不下表单）；字段同上 |
| 封面选择 / 在线元数据面板 | `ModalBottomSheet` + `ListItem` → `AppleSheet` + `AppleSheetRow`（带 48dp 封面、行尾「最佳匹配」） |
| 歌词版本选择 / 选择歌单 | 同上；新建歌单的输入框改 `AppleTextField` + `AppleAlert` |
| AI 解读 | `MediumFlexibleTopAppBar` → `AppleTopBar`；空态/错误态改用 `AppleType` + 胶囊按钮 |
| 均衡器 | 开关行、分区标题、预设胶囊、复位按钮全部换 Apple；响应曲线改 `systemRed` |
| 耳机校正面板 | 搜索框改 `AppleSearchField`，筛选项改 `AppleChip`，结果改 `AppleSheetRow` |
| 在线音源 | 分段按钮 / FilterChip → `AppleChip`；插件卡片改「分组行 + 发丝线」；导入对话框 → `AppleAlert` |
| 设置页 | 行组件上提到共用件，删掉本地重复实现；排除文件夹与音质说明两行仍是 Material `ListItem`（待下一批） |

---

## 11. 第五批：收尾（Material 组件清零）

至此 `presentation/` 里已没有 `ListItem` / `AlertDialog` / `ModalBottomSheet` / `OutlinedTextField` /
`FilterChip` / `SegmentedButton` 的实例化（`SongListItem` 是本项目自己的组件，不在此列）。

| 位置 | 改动 |
|---|---|
| 设置页 · 小金标 / 缓存上限 / 清缓存 | `AlertDialog` → `AppleSheet`（选项列表）/ `AppleAlert`（破坏性确认，红字）；缓存面板 `ModalBottomSheet` → `AppleSheet` |
| 设置页 · 12 个 `OutlinedCard` | → `AppleGroupedSection`（`secondaryBackground` + 14dp 圆角，无描边），分区标题改 13sp 半粗 `secondaryLabel` |
| 设置页 · API Key 页 / 排除文件夹 / 音质统计 | `OutlinedTextField` → `AppleTextField`（支持密码变换），按钮改胶囊，行改 `AppleSettingsRow` 语法 |
| 我的音乐 · 新建歌单 / AI 生成 / 重命名 | 面板与弹窗同上；预览曲目行改 `AppleSheetRow` |
| 统计页 · 日详情 / 完整榜单 / 清空确认 | 两个面板 + 一个确认弹窗 |
| 播放页 · 播放设置面板 | 面板容器 + 倍速 / 定时档位改 `AppleChip` |
| 播放页 · 队列行 | `ListItem` → 手写 Row（拖拽手柄 + 48dp 封面 + 两行文本），当前曲目底色改 `fill` |
| 歌词路由 · 更多 / 设置 / 对轴 | 面板与弹窗同上 |
| 文件夹 / 搜索 · 来源与 Tab 切换 | `SegmentedButton` → `AppleChip` |
| 排行榜 · 口径 / 窗口筛选 | `FilterChip` → `AppleChip` |

---

## 12. 第六批：设置主页对齐 MeloX 骨架

参照 `MeloX-Android` 的 `ui/settings/SettingsScreen.kt` + `ui/glass/MeloXIosComponents.kt`，
设置主页从「Material 结构 + Apple 配色」改成真正的 iOS grouped list 骨架。

| 项 | 改前 | 改后 |
|---|---|---|
| 顶部 | `Scaffold` 顶栏 + `AppleTopBar` | **保持不变**：顶栏固定，只有搜索框与列表滚动（试过把大标题塞进 `verticalScroll`，滚起来顶栏没了，返回键也跟着消失） |
| 搜索 | 无 | `AppleSearchField`，按标题/副标题/关键词实时过滤，无结果显示「没有匹配的设置项」 |
| 强调色 | 图标灰、箭头灰 | 图标与箭头统一 `systemRed`（Apple Music 红） |
| 分组卡 | `secondaryBackground`（写死的 `#1C1C1E` / `#F2F2F7`） | `label.copy(alpha = 0.06f)`：把页面底色抬 6%，跟着壁纸动态取色走；无描边 |
| 分隔线 | 从图标右侧起到行尾 | 两侧各缩进 16dp，由分区统一插入（过滤后不会留孤线） |
| 行高 | 内容撑开 | `heightIn(min = 56.dp)`，标题 16sp / 副标题 14sp 至多两行 |
| 开关 | Material `Switch` | `AppleSwitch`：51×31dp 胶囊 + 27dp 白圆头 + 苹果绿（`#30D158` / `#34C759`） |
| 页边距 | 16dp | 16dp（与 `AppleTopBar` 大标题的左边缘对齐；分区标题再缩进 8dp） |

实现要点：

- 主页面改成数据驱动：`SettingsEntry`（标题/副标题/关键词/图标/动作）→ `SettingsSectionSpec`，
  由 `SettingsSectionBlock` 渲染。行是内联声明的话，搜索框「找得到但点不进去」几乎没法避免，
  所以顺手把 42 行手工分隔线也一起删了。
- 顶栏固定（`Scaffold` + `AppleTopBar`），主页面与二级页同一套 chrome，内容只滚自己那一块。
- 卡片底色不写死：页面底色来自壁纸动态取色，固定中性灰会出现「卡片偏冷、页面偏紫」的脏对比，
  所以改成把页面底色抬 6%（亮色下 ≈ `#F0F0F0`，暗色下是白 6%），任何壁纸下都成立。
  玻璃材质（85% alpha + 白描边）被否掉 —— 压在纯黑上会被抬成 `#212123`，观感变成描边卡片。
- 没有做 MeloX 底部的「重置 / 登出」危险卡：本项目没有对应的重置用例，凭空加一个按钮等于给
  用户一个会删配置的坑。

新增共用件（`core/ui/apple/AppleSettings.kt`）：`AppleSettingsSectionTitle` /
`AppleSettingsSeparator` / `AppleSwitch`；行组件增加 `accent` 参数（默认 `systemRed`），
行高 `heightIn(min = 56.dp)`，`trailingContent` 改为可空（信息行不用再传 `{}`）。

**统一做法**：字阶与语义色用一张映射表批量替换（`bodyLarge→body`、`onSurfaceVariant→secondaryLabel`、
`primary→key`、`error→systemRed` …）。`MaterialTheme.shapes.*` 与 `motionScheme` 保留 —— 前者几何与
iOS 圆角接近、后者是 Material 独有的动效 token，替换没有收益。

**仍可改进**：统计页图表（自绘 Canvas）的刻度与网格仍是 M3 观感；`MaterialTheme.shapes` 尚未收敛到
`AppleDims`。

---

## 13. 详情页顶栏按钮：玻璃圆钮（照 MeloX 歌单详情页）

参照 `MeloX-Android` 的 `MeloXPlaylistToolbar` + `MeloXGlassCircleButton`
（`ui/library/LibraryScreen.kt`）：那颗按钮不是 IconButton 也不是实色 scrim，而是
**44dp 半透明玻璃圆钮**，底色跟着图标明暗走。

| 项 | MeloX | Sonic 落地 |
|---|---|---|
| 尺寸 | 44dp 圆 / 22dp 图标 | 同 |
| 托底色 | 前景 == 纯白 → 黑 22%；否则白 64% | `glassSurfaceFor(tint)`：按相对亮度分档（插值出来的中间色不会跳） |
| 按压 | `meloXLiquidButton` 的液面折射 | 无涟漪，底色 alpha ×1.35 |
| 顶栏行 | 58dp 高、左右 18dp、右组间距 8dp | 沿用 `CenterAlignedTopAppBar`（它负责折叠），只换按钮 |
| 动作图标色 | `#FF3147` | `AppleColors.systemRed`（#FF3B30，同色系，走设计令牌） |

落地两处：

- `core/ui/apple/AppleComponents.kt` 新增 `AppleGlassCircleButton`：`icon / tint / size /
  iconSize / glassAlpha / enabled`，`glassAlpha` 给「顶栏随滚动淡出」的页面用。
- `components/CollectionDetailScreen.kt`：`DetailTopBarAction` 从 `IconButton + scrim`
  改为这颗玻璃圆钮；原来那层 `0.60f × (1 - 2×collapse)` 的黑色 scrim 换成同一条曲线上的
  `glassAlpha`，折叠过半照样退干净（否则实底顶栏上会留两个玻璃点）。
  动作图标由 `onArtwork` 改为「品牌红 → onSurfaceVariant」的插值，返回键仍是自适应色。

顺带：歌单 / 专辑 / 歌手三个详情页共用这个组件，所以三页一起换了。

---

## 14. 迷你播放器与底部导航的图标

两处原来用 Material Symbols 字体的图标，换成 XML 矢量资源（Apple 造型）：

| 位置 | 改前 | 改后 |
|---|---|---|
| 迷你播放器 播放/暂停 | `SymbolIconName.PlayArrow` / `Pause` | `ic_flamingo_mini_play` / `ic_flamingo_mini_pause` |
| 迷你播放器 下一首 | `SymbolIconName.SkipNext` | `ic_flamingo_mini_fast_forward` |
| 导航 首页 | `SymbolIconName.Home` | `ic_nav_home`（SF `house` 轮廓） |
| 导航 音乐库 | `SymbolIconName.LibraryMusic` | `ic_nav_library`（SF `music.note.list`） |
| 导航 搜索钮 | `SymbolIconName.Search` | `ic_nav_search`（SF `magnifyingglass`） |

- `mini_*` 三个是 Apple Music 专门给 48dp 胶囊画的版本（三角与竖条比大播放键粗一档），
  缩到 24dp 仍是原比例，不用再调字重。
- 导航三颗当时都是手写的（那批只抽了播放页）。**音乐库那颗后来已换成真图**
  `ic_flamingo_tab_library`（见 §15）；`ic_nav_home` / `ic_nav_search` 仍是按 SF 描边语言
  手写的 XML —— Flamingo 的 tab 栏只有资料库一颗，没有 home / search 可用。
- `BottomNavTab` 去掉了 `selectedIcon`：三颗 tab 图标本来就是同形，选中只换颜色
  （原来那个字段两处传的是同一个值，纯冗余）。

---

## 15. Flamingo 图标全量抽取（39 → 65）

之前只抽了播放页 / 歌词页用到的 39 个；现在把 Flamingo 1.0.0 里**应用自有**的图标全抽了，
新增 26 个，`res/drawable/ic_flamingo_*` 一共 65 个。

| 组 | 新增资源 |
|---|---|
| tab 栏 | `ic_flamingo_tab_library`（资料库 tab 的原图） |
| 资料库分类 | `ic_flamingo_library_album` / `_artist` / `_playlist` / `_song` |
| 播放模式（细） | `ic_flamingo_shuffle_plain` / `_shuffle_off` / `_repeat_plain` / `_repeat_off` / `_repeat_one_plain` |
| 迷你条模式 | `ic_flamingo_mini_shuffle` / `_mini_shuffle_off` / `_mini_repeat` / `_mini_repeat_off` / `_mini_repeat_one` |
| 占位图 | `ic_flamingo_placeholder_folder` / `_placeholder_artwork` / `_placeholder_playlist` / `_placeholder_playlist_new` |
| 其他 | `ic_flamingo_tips_round_corner` / `ic_flamingo_credits_person` |
| 应用图标 | `ic_flamingo_app_icon` / `_mono` / `_notification` / `_share` / `_ticker` / `_widget` |

抽取工具链留在 `E:\deepseekworkspace\flamingo_x\`：

- `gen_all.py` —— 目标清单 + 循环；
- `bbox3.py` —— **字符级** path 解析。原来的 `bbox2.py` 按空白切 token，遇到「flag 与坐标
  粘在一起」的弧命令（`a4 4 0 0112 0`，vector drawable 里很常见）会直接抛错，
  这批里有 16 个图标栽在这上面；现在按 SVG 规范让 `A` 的两个 flag 各只吃一个字符。
- `gen_xml.build_with(..., bbox)` —— 复用原有导出逻辑。这里踩过一个**线上崩溃**：
  aapt2 的 `xmltree` 输出是 `"<值>" (Raw: <原始串>)`，个别资源的值自带引号，
  原来的 `clean()` 正则会连 `(Raw: …)` 一起吞进 pathData —— 表现为路径尾部多出
  `R`、`w`、`)`，运行时 `PathParser` 抛 `IllegalArgumentException: Unknown command for: R`。
  现在先 `split("(Raw:")[0]` 砍掉 Raw 段再清引号，并且**加了一道字符集断言**：
  pathData 只允许 `MmLlHhVvCcSsQqTtAaZz0-9 .,+-eE`，出现别的字符直接中止生成。

  ⚠ 教训：只做 `ET.parse`（XML 是否 well-formed）根本验不出这种问题 ——
  文件是合法 XML，错的是属性里的路径语法。要验就验 pathData 本身。

导航栏「音乐库」那颗随之换成真图 `ic_flamingo_tab_library`，手画的 `ic_nav_library` 删了。
`ic_nav_home` / `ic_nav_search` 保留手绘版 —— Flamingo 里没有对应资源（它的 tab 栏只有资料库）。

---

## 16. 资料库分区索引与排序（Sonic 未实现）

来源与前 15 节不同：这批不是 `res/` 里的 layout/style，而是
`assets/Resources.zip` 里的 **iOS 资源包**（`MediaLibraryCore/<lang>.lproj/Sections.plist`、
`SortingDetails.plist`），加上 `content_layout_library_section.xml` / `fastscroller.xml` /
`item_sort_submenu.xml` / `library_edit_sections_fragment.xml` 四份布局。
资源包路径本身就是证据：分区字母表是 iOS 那套 `.lproj` 直接搬过来的。

### 16.1 分区字母表 = A–Z，中文走 ICU 转写

`en.lproj/Sections.plist`：`SectionIndices` 与 `SectionHeaders[0].Headers` 都是 A–Z，
`LastCharacter = Z`，`FirstCharacterAfterLanguage = ʒ`。

`zh_CN.lproj/Sections.plist` 是**二进制 plist**，读出来 `FirstCharacterAfterLanguage = 汉`，
Headers 仍是 A–Z，并带一条 ICU 变换：

```
Han-Latin; NFD; [:Mn:] Remove; NFC
```

即**中文条目按拼音首字母归档**，转换不是手写映射表，而是调 ICU 的 Han-Latin 音译器。

Android 侧同一条规则可直接拿到（API 29+，本项目 minSdk 30 够用）：

```kotlin
android.icu.text.Transliterator
    .getInstance("Han-Latin; NFD; [:Mn:] Remove; NFC")
    .transliterate("周杰伦")   // → "zhou jie lun"
```

**同一个 ICU 规则，零依赖**，不需要引 TinyPinyin 之类。

### 16.2 排序忽略冠词

`SortingDetails.plist` 只有一项：

```
InsignificantSortPrefixes = ["the ", "an ", "a "]
```

即按标题排序时先剥掉冠词再比较（"The Beatles" 归 B 不归 T），**显示仍是原名**。
Sonic 现在 `SongsScreen.kt:461` 是 `songs.sortedBy { it.title.lowercase() }`，没有这一步。

### 16.3 A–Z 快速索引条

`content_layout_library_section.xml`：

```
ConstraintLayout
├── SwipeRefreshLayout → SafeRecyclerView(vertical, scrollbars=none)
├── com.apple.android.music.library.FastScroller
│     gravity=right|center_vertical, width=wrap, height=match, 默认 GONE  ← 滚动时才出现
└── FrameLayout(空态 / 加载态)
```

`fastscroller.xml`：

| 元素 | 实测 |
|---|---|
| 触摸带 | `View` **100dp** 宽、满高，`alignParentEnd` |
| 拇指 | `ImageView` src=`fastscroller_bubble`，**6×30dp、圆角 8dp**，色 `color_primary`，左右 padding 6dp |
| 拖动气泡 | `CustomTextView` **40sp 白字**，底 `fastscroller_handle`：layer-list = **80×80 圆** + 5×5 点，`inset left/top 40dp`、`bottom 1px` |

`dimens.xml`：`fast_scroller_default_width` 8dp、`fastscroll_default_thickness` 8dp、
`fastscroll_margin` 0dp、`fastscroll_minimum_range` 50dp。

对照：Sonic 的歌曲 / 专辑 / 歌手列表**没有**任何索引条，也没有分区标题（`SortOrder` 只是整体排序）。

### 16.4 排序子菜单 `item_sort_submenu`

```
FrameLayout(minWidth=150dp, marginEnd=3dp, height=?attr/listPreferredItemHeightSmall)
├── CustomTextView(标题行)      textAppearance=?attr(下拉项)
├── FrameLayout(style=Divider)
├── CustomTextView(副标题行)
└── ConstraintLayout
    ├── ImageView 前置图标       min 19dp
    ├── TextView 主文本          marginStart/End 16dp, singleLine, 走马灯
    └── ImageView 28dp           选中勾，tint=label_color
```

即**两行文字 + 右侧 28dp 勾**。Sonic 现在是 `DropdownMenu` + 单行 `Text`。

### 16.5 分区可编辑

`library_edit_sections_fragment.xml`：`RecyclerView`（`marginTop=8dp`、
`nestedScrollingEnabled=false`、底部让出 `miniplayer_height` 48dp）。
条目来源就是同一份 `Sections.plist` —— **资料库首页的分区顺序可改、可隐藏**。

### 16.6 置顶（Pin）

native 侧有完整的一条：`mlcore::Pin`（10 个符号）+ `SVEditPinState::run()`（`0x3900F4`，
19.3 KB，全库第二大函数）。7.0 的资料库支持「置顶」，置顶项排在字母表之前。
Sonic 无此概念。

### 16.7 差距汇总

| 能力 | Apple | Sonic | 落点 |
|---|---|---|---|
| A–Z 分区标题 | ✅ | ❌ | `SongsScreen` / 专辑 / 歌手列表 |
| 中文拼音归档 | `Han-Latin` ICU | ❌ | 可直接用 `android.icu.text.Transliterator` |
| 快速索引条 | ✅ 6×30dp 拇指 + 80dp 气泡 | ❌ | 新组件 `AppleFastScroller` |
| 排序忽略冠词 | ✅ the/an/a | ❌ | `sortSongs()` 排序键 |
| 排序菜单两行 + 勾 | ✅ | 单行 `Text` | `SongsScreen` 下拉 |
| 分区顺序可编辑 | ✅ | ❌ | 资料库首页 |
| 置顶 | ✅ | ❌ | 需 Room 字段 + 列表段 |

> 露骨歌词过滤另有一条：`mlcore::ContentRestrictions::restrictExplicitLyrics()` ——
> 过滤在 native 层强制，UI 只负责设置项开关与行内 `E` 标记。Sonic 的 `Song` 已有
> 相关字段，但没有任何过滤 UI。

---

## 17. 歌单详情页头部（`epoxy_layout_collection_page_uber_header`）

手机版合集页走的是 **uber 头部**：`header_non_uber_cover_art_size` 在手机上取 `0dp`，
只有 `w600dp` 才给 300dp —— 也就是说窄屏不存在"独立小封面"那套，必须用 uber 版式。

### 17.1 结构

```
ConstraintLayout
├── Space  h=?attr/actionBarSize（56dp）    顶栏带：返回 / 分享 / 更多压在这条带上
├── Guideline begin=72dp / end=72dp         header_non_uber_cover_art_margin_horiz
├── Space(1:1, 72dp 内)                     封面槽位，marginTop 10 / marginBottom 15
├── Guideline begin=16dp / end=16dp          default_padding —— 文字与按钮的列宽
├── CustomImageView 0dp × 0dp, ratio 3:4    满宽背景封面（TextureView 叠在其上）
├── View h=120dp  background=collection_header_uber_toolbar_gradient   顶栏渐变
├── View h=250dp                             header_uber_extend_color_gradient_height 颜色延伸
├── TextView   style=Title2Medium   maxLines=2   标题 20sp Medium
├── TextView   style=Title3         maxLines=2   副标题 20sp
├── FlexboxLayout  alignItems=center, 横向分隔符 page_header_caption_divider_horizontal
├── EditorsTextParagraph  style=Subhead  maxLines=2, ellipsize=start   简介
├── CustomCheckBox 28×48                     展开开关
├── View  style=Divider
└── include button_group_play_shuffle_buttons  weightSum=2
```

**三个易错点**：

1. **两套 guideline 不要混**。72dp 只管**封面**，文字 / 简介 / 分隔线 / 按钮全部落在 **16dp**
   那一对 guideline 上（`0x7f0a0abb` / `0x7f0a0abf`）。
2. **首个 `Space` 不是状态栏，是 `?attr/actionBarSize`（56dp）**。封面从这条带**下面**开始，
   顶栏按钮不与封面重叠。把它当成状态栏写过（`statusBar + 10`）会让分享 / 更多压住封面左上角。
3. **背景图必须裁在头部范围内**，且高度 = 头部高度（`matchParentSize`）。按 3:4 铺一张满宽图、
   图高 > 头部高时，Compose 默认不裁子项溢出，封面会一路画到曲目列表后面。

### 17.2 动作按钮 `button_b`

| 项 | 实测 |
|---|---|
| 高度 | 44dp（`button_b` 里 TextView 的 `layout_height`） |
| 圆角 | `corner_radius_pill` = 80dp |
| 底色 | `tertiary_fill_color`：日间 `#1F767680`、夜间 `#3D767680` |
| 字 | `ButtonB` = SubheadEmphasized 覆写 18sp，居中、非全大写 |
| 排布 | `weightSum=2`，两个各占一半，中间 8dp |

即**两颗胶囊同底色同字色**，不是"播放=品牌红实心"。Sonic 原先那个红色播放键是
MeloX 的写法，这一版按实测改回 `tertiary_fill`。

### 17.3 Sonic 落地

新增 `presentation/components/PlaylistHeader.kt`（`ApplePlaylistHeader`）：

| 元素 | 值 | 来源 |
|---|---|---|
| 顶栏带 | 状态栏 + 56dp（`?attr/actionBarSize`） | 首个 `Space` 的 `layout_height` |
| 封面左右留白 | 72dp（边长 = 屏宽 − 144dp） | `header_non_uber_cover_art_margin_horiz` |
| 封面投影 | 10dp | `header_non_uber_cover_art_shadow_elev` |
| 封面到标题 | 15dp | `header_non_uber_title_margin_top` |
| 标题 | `AppleType.title2Medium` 20sp Medium / 2 行 / 居中 | `style/Title2Medium` |
| 副标题 | `AppleType.title2` 20sp | `style/Title3` |
| 元数据 | `AppleType.caption2Emphasized` 11sp，圆点分隔 | `style/Caption2Emphasized` |
| 简介 | `AppleType.subhead` 15sp / 2 行 | `style/Subhead` |
| 胶囊 | 44dp、pill、`AppleColors.fill`、`AppleType.button` 18sp | `button_b` |
| 分隔线 | 0.5dp `separator`，左右 16dp | `style/Divider` |

改造点：

- `CollectionDetailScreen` 新增 `heroStyle: CollectionHeroStyle`（`FullBleed` 默认 /
  `CenteredCover`）与 `headerMeta`。歌单页传 `CenteredCover`，专辑 / 歌手页不动。
- **折叠进度改按头部实测高度归一**（`onSizeChanged`）。原来固定除以边长：
  满铺 hero 恰好等于边长所以没暴露问题，换成居中封面版式后头部高得多，
  仍除以边长会让顶栏标题在滚过三分之一时就抢跑。
- 简介（`PlaylistDescription`）改走 Apple 字阶，并新增 `collapsedLines` /
  `horizontalPadding` / `verticalPadding` 参数：歌单页 2 行 + 16dp、垂直留白交给头部；
  我的音乐页仍是 3 行 + `SonSpacing.small`。展开开关从「另起一行的更多」改成**行尾 24dp 箭头**
  （原版 `CustomCheckBox` 28×48、`bias=end`）。
- **歌单页的顶栏直接用自带的 `AppleTopBar`**：新增 `topBarCollapse: Boolean = true`，歌单传 `false`，
  Scaffold 的 `topBar` 走 `AppleTopBar`（状态栏间距 + 44dp 返回行 + 34sp 大标题 + 标题行尾动作），
  不再自绘透明浮层。专辑 / 歌手页仍是原来的 `CenterAlignedTopAppBar` + 收起态。
  - 大标题从封面下方移到顶栏里，所以 `ApplePlaylistHeader` 加了 `showTitle = false`，避免标题出现两次；
    同时 `reserveTopBar = false`，头部不再让出「状态栏 + 56dp」，封面直接从内容区顶部 +10dp 开始。
  - `LazyColumn.contentPadding.top` 取 `innerPadding.calculateTopPadding()`，内容从栏下方开始。
  - 动作按钮共用一份 `barActions`；透明浮层上用玻璃圆钮 `DetailTopBarAction`，实底栏上换成
    自带的 `AppleIconButton`（`CollectionBarAction` 按 `glassAlpha == null` 分派）。

**与原版的差异（已知）**：

- 原版背景是 `BlurTarget` + `BlurView` 实时模糊 + 250dp 颜色延伸。Sonic 用 **Coil 按 24×24 解码
  再双线性拉满整块** —— 设备无关的等效模糊。`Modifier.blur` 在 API 31 以下不生效，
  API 30 上会静默退化成一张清晰封面（看起来像"下面又叠了一张封面"），所以不能用它。
- 顶栏遮罩按 `collection_header_uber_toolbar_gradient` 落地：120dp、`#40000000`（black 25%）→ 透明。
- 动作条只放**两颗等宽胶囊**。原版 `button_group_play_shuffle_buttons` 是 4 个等权槽位
  （`weightSum=2`），其中两个默认 gone，运行期只剩播放 + 随机；加歌在顶栏那颗 ≡+ 上，
  再塞进这一排会把两颗胶囊挤成不等宽。
- 简介的展开开关放在**行尾**（24dp 图标），不是另起一行「更多」；简介块自身不加垂直留白，
  到分隔线的 16dp 由头部的 `marginBottom` 给（`EditorsTextParagraph` 的 16dp）。
- 本地歌单没有策展人，副标题留空，只保留「N 首歌曲 · X 小时 Y 分钟」元数据行。

> ⚠ **§17.3 的实现已被 §18 取代**：歌单页最终按 `MeloX-Android` 的
> `MeloXPlaylistDetailScreen` 复刻（用户指定），不是 Apple 的 uber 头部。
> §17.1 / §17.2 的 Apple 侧实测数据保留作对照资料。

---

## 18. 歌单页改按 MeloX 复刻（现行实现）

对标：`D:\Programming\Kotlin\MeloX-Android-main` 的
`ui/library/LibraryScreen.kt` → `MeloXPlaylistDetailScreen` / `MeloXStandardPlaylistHero` /
`MeloXPlaylistToolbar` / `MeloXGlassCircleButton`。
落地文件：`presentation/components/PlaylistPage.kt`（取代已删除的 `PlaylistHeader.kt`）。

### 18.1 整页结构

```
整屏背景  MeloXPlaylistBackdrop：封面 24×24 解码拉满 + 明暗可读性层 + 下沿收底色
├── Scaffold topBar  MeloXPlaylistToolbar（58dp，透明，浮在背景上）
│     左：返回玻璃圆钮 44dp ｜ 右：动作玻璃圆钮 44dp × N（间隔 8dp）
└── LazyColumn（contentPadding.top = 工具条高度，背景透明）
      ├── MeloXPlaylistHero
      └── 曲目行（行首封面，不是序号）
```

### 18.2 英雄区几何（`MeloXStandardPlaylistHero` 实测）

| 元素 | 值 |
|---|---|
| 整块上下留白 | 26dp / 22dp |
| 封面 | `min(屏宽 × 0.68f, 300dp)`，圆角 12dp，投影 18dp |
| 标题 | 22sp / 行高 27 / Bold / 居中 / 至多 2 行，上 24dp、左右 24dp |
| 创作者 | 20sp / 行高 24 / 居中 / 1 行，上 8dp |
| 元数据 | 15sp / 行高 19 / Medium / secondary，上 7dp |
| 动作行 | 上 17dp，间隔 14dp：玻璃圆钮 54dp ＋ 胶囊 140×50 ＋ 玻璃圆钮 54dp |
| 播放胶囊 | 圆角 25dp；深色主题白 82% 底 + 黑字，浅色主题黑 82% 底 + 白字；图标 19dp + 文字 19sp Bold |
| 简介 | 14sp / 行高 20 / secondary / 折叠 3 行、展开 12 行 / **整段可点**，上 24dp、左右 20dp |

### 18.3 与 §17（Apple 版）的差别

| 项 | Apple uber 头部 | MeloX（现行） |
|---|---|---|
| 封面 | 左右各 72dp，边长 = 屏宽 − 144dp | 屏宽 × 0.68，上限 300dp |
| 标题位置 | 封面下方 20sp Medium | 封面下方 22sp Bold |
| 动作区 | 两颗等宽胶囊 + 可选圆钮 | 圆钮 54 ＋ 胶囊 140×50 ＋ 圆钮 54 |
| 简介开关 | 行尾 24dp 箭头 | 点整段文字切换 3 行 ↔ 12 行 |
| 顶栏 | 透明浮层 + 折叠标题 | 58dp 工具条，只有玻璃圆钮，不折叠 |
| 背景 | 头部内 3:4 模糊封面 | 整屏模糊封面（列表也压在背景上） |
| 曲目行 | 序号 / 播放音柱 | 封面 48dp + 歌名 + 歌手（`showTrackNumbers = false`） |

### 18.4 壳层接口

- `CollectionDetailScreen(topBarCollapse = false)` → 顶栏走 `MeloXPlaylistToolbar`，
  内容背景改透明并挂 `MeloXPlaylistBackdrop`，`contentPadding.top` 取工具条高度。
- 新增 `showTrackNumbers: Boolean = true`；歌单传 `false` → `SongListItem(trackNumber = null)`
  走封面行。专辑 / 歌手页不受影响（仍是序号 + 播放音柱、仍是苹果式头部）。

---

## 19. 首页改成「全部歌曲」（2026-10-26）

按用户要求，首页不再做 Apple 的 Listen Now 结构：推荐专辑 / 推荐歌手 / 最近添加三个分区、
横滑卡组 `AppleCarousel` / `AppleCard` 全部移除，只保留一份全库曲目列表。

| 项 | 改前 | 改后 |
|---|---|---|
| 顶部 | 问候语大标题 + 搜索 / 设置 | 大标题「全部歌曲」+ 设置入口（搜索已在底栏圆形钮上） |
| 内容 | 三个分区 + 横滑卡组 | `LazyColumn` + `SongListItem`，点行即播并进播放页 |
| 数据 | 歌曲 / 专辑 / 歌手三源，谁先到谁先画 | 只有 `SongQueryUseCase.all()` 一源 |
| 状态 | `HomeUiState.recentlyAdded / topAlbums / topArtists` | `HomeUiState.songs` |

`HomeContract` 同步收窄（Intent 只剩 `LoadData / SongClick / NavigateToSettings`，
Effect 只剩 `ShowToast / NavigateToPlayer / NavigateToSettings`），`AppNavGraph` 的
`Screen.Home` 入口去掉分区跳转参数；`HomeViewModelTest` 按新 API 重写。

---

## 20. 首页 A–Z 索引条（落地 §16 的规格）

`AppleAlphabetIndex`（`core/ui/apple/AppleAlphabetIndex.kt`）+ `sectionSongsByAlphabet`
（`presentation/components/AlphabetSectioning.kt`），挂在首页「全部歌曲」上。

| 项 | 实现 |
|---|---|
| 分区字母 | 首字符是 A–Z 直接用；其余非 ASCII 走 ICU `Han-Latin; NFD; [:Mn:] Remove; NFC` 转写取首字母；数字/符号/转写失败归 `#`（排在最后） |
| 排序 | 由 `sectionSongsByAlphabet` 一次完成（字母 → 标题），索引条才跳得准 |
| 触摸带 | 32dp（原版 100dp 是透明命中区，这里视觉与命中区合一，不吃掉列表一列） |
| 字母 | **按屏高自适应**：整条占可用高度 42%，格高 = 高度 / 字母数（夹 12–18dp），字号 = 格高 × 0.8（夹 9–13sp）；整条垂直居中，当前分区用 `label` 色 + SemiBold |
| 拖动气泡 | 80dp 圆（`fastscroller_handle`）、`material` 底 + 0.5dp 描边 + 12dp 投影、40sp 中等字重 |
| 命中公式 | `(y / height * n)`，与等高等距的落点同源，手指在哪气泡就在哪 |
| 跳转 | `listState.scrollToItem(sectionStartIndex[letter])`；下标按「大标题 1 格 + 每区 1 头 + 该区曲目数」累加 |
| 高亮 | `firstVisibleItemIndex` 反查最近的分区起点 |

**列表里没有分区字母头**（用户要求去掉）：字母只出现在右侧索引条上，列表就是一条连续的歌名流。

---

## 21. 首页长按歌曲弹菜单

`SongListItem` 新增 `onLongClick`：

- 传了 `onLongClick` → 用 `Modifier.combinedClickable`（`indication = null`，保持 Apple 的无涟漪按压）；
  没传 → 仍是 `applePress`。
- **下拉菜单从行尾那颗 ⋮ 里搬到了整行这一层**：之前 `DropdownMenuPopup` 嵌在 `if (showMoreButton)` 里，
  没有 ⋮ 的行根本没有弹层可用。现在弹层挂在 `Box { Row(...) ; popup }`，锚点仍是行尾，
  长按与点 ⋮ 走同一个弹层。

首页接线：

| 项 | 来源 |
|---|---|
| 下一首播放 / 加入队列 | `HomeIntent.PlayNext` / `AddToQueue` → `playerRepository` |
| 收藏 / 取消收藏 | `HomeIntent.ToggleFavorite` → `playerRepository.setSongFavorite(id, !isFavorite)` |
| 查看详情 | `onNavigateToSongDetail` → `Screen.SongDetail(id)` |

菜单**从手指位置弹出**：长按用 `detectTapGestures` 拿到的 `Offset` 记进 `pressPoint`，
再由 `AtPointPopupPositionProvider` 把「锚点窗口坐标 + 手指偏移」算成弹层位置，并收进窗口边界；
从 ⋮ 打开时清空 `pressPoint`，退回「贴行尾」的默认定位。

菜单项复用 `songMenuItems(song, queueActions, onToggleFavorite)`，与歌曲页 / 资料库一致。
只有当前打开的那一行会构造菜单项（`moreMenuItems = if (expanded) ... else emptyList()`），
其余行不建弹层 —— 全库几百首时否则每行都会挂一个空 Popup。

### 20.1 跳到正在播放

首页右下角加了一颗 44dp 圆形按钮（新写的手绘矢量 `ic_jump_to_playing.xml` —— SF `scope` 的十字准星 + 圆环，`material` 底 + 0.5dp 描边）：

- **只在当前曲目滚出可视区时出现**：`derivedStateOf` 读 `listState.layoutInfo.visibleItemsInfo`
  的 key 是否包含 `song-<id>`，只在 true/false 翻转时才推一次，滚动过程不重组整页。
- 点击 `scrollToItem(indexBySongId[id])`（**瞬间落位，不做动画**：几百首时动画要滚一两秒）；
  下标表与索引条共用同一套累加规则
  （大标题占 1 格，其后每首歌 1 格）。
- 位置：`align(BottomEnd)` + `bottom = appleBottomBarClearance() + bottomPadding + 16dp`，
  和右侧居中的索引条不重叠。
