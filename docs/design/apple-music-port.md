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

**统一做法**：字阶与语义色用一张映射表批量替换（`bodyLarge→body`、`onSurfaceVariant→secondaryLabel`、
`primary→key`、`error→systemRed` …）。`MaterialTheme.shapes.*` 与 `motionScheme` 保留 —— 前者几何与
iOS 圆角接近、后者是 Material 独有的动效 token，替换没有收益。

**仍可改进**：统计页图表（自绘 Canvas）的刻度与网格仍是 M3 观感；`MaterialTheme.shapes` 尚未收敛到
`AppleDims`。
