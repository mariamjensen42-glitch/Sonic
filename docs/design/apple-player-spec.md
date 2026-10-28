# Apple Music 播放页结构拆解

对标 `com.apple.android.music` 7.0.0-beta (1611)。所有数值由 `aapt2 dump xmltree` 与实际
`dimens.xml` 交叉得到，不是目测。

涉及布局：`fragment_player_main`、`constraint_player_media_controls`、`player_progress_seek_bar`、
`fragment_player_lyrics_sheet`、`lyrics_line_karaoke`、`lyrics_word_karaoke`。

---

## 1. 播放页根 `fragment_player_main`

```
RelativeLayout (match_parent)
└── ConstraintLayout
    ├── LyricsBackgroundLayerView        ← 整屏背景层，alpha 从 0 起（歌词态才浮现）
    │                                     background=@drawable/...，尺寸铺满
    ├── Guideline(horizontal)            ← 封面区域的水平基准线
    ├── Guideline(vertical, begin=32dp)  ← lyrics_line_padding_horiz
    ├── ViewSwitcher (alpha=0)           ← 封面页 / 歌词页切换，两个 include 指向**同一个** layout
    │   ├── include @layout/<player scene>   (id …7b3)
    │   └── include @layout/<player scene>   (id …7b4)
    ├── FrameLayout
    │   └── View (gravity=bottom, style=Divider)   ← 底部发丝线
    └── include @layout/mini_player      ← 收起态复用迷你播放器
```

两个 `include` 指向同一份布局，说明封面页与歌词页是**同一个场景的两份实例**，用 `ViewSwitcher`
做交叉淡入，而不是两套界面。这是 Apple 用 `TextureView` + 共享元素转场的基础。

---

## 2. 控制区 `constraint_player_media_controls`

```
ConstraintLayout (clickable)
├── include player_progress_seek_bar      ← 顶部：进度 + 时间
├── include <title/album row>             ← 曲名 / 专辑
├── ImageView  播放键   (wrap, padding=11dp)  constraint 居中，水平 bias 0.5
├── TintableImageView 上一首 (wrap, padding=11dp, marginEnd=40dp)
├── TintableImageView 下一首 (wrap, padding=11dp, marginStart=40dp)
├── ImageView  随机   (gravity 左下, marginBottom=14dp, marginEnd=40dp)
├── ImageView  循环   (gravity 右下, marginBottom=14dp, marginStart=40dp)
├── ImageView  无缝播放徽标（默认 gone）
├── ImageView  空间音频徽标（gone，16dp）
├── CustomMediaRouteButton  ← 投屏
├── LinearLayout 无损徽标 (height=32dp, paddingH=10dp)
├── CustomTextView 章节/来源标签 (style=Footnote 13sp)
└── CustomTextView 歌词提示行 (style=Caption2 11sp, marginTop=6dp)
```

关键尺寸（`dimens.xml` 实测）：

| 令牌 | 值 | 用途 |
|---|---|---|
| `control_buttons_distance_padding` | **40dp** | 上一首/播放/下一首之间 |
| `side_control_button_padding` | **11dp** | 每颗运输键的内边距（图标外扩） |
| `action_button_space_margin_bottom` | **14dp** | 随机/循环键距底边 |
| `player_thumbnail_height` | 60dp | 歌词页顶部的小封面 |
| `image_border_stroke_width` | 0.5dp | 小封面描边 |
| `badge_icon_size` | 16dp | 空间音频/无损徽标 |

---

## 3. 进度条 `player_progress_seek_bar`

```
LinearLayout(vertical, clickable)
├── MinimalSeekBar (paddingVertical=5dp)   ← 自绘细线，平时无滑块
└── ConstraintLayout
    ├── CustomTextView 已播 (enableMonospaceDigits=true, style=Caption1Emphasized 12sp)
    └── LinearLayout 无损徽标 (paddingH=7dp, paddingV=3.5dp, 文字 11sp)
```

两个细节值得抄：

- **等宽数字**（`enableMonospaceDigits`）—— 秒数跳动时不左右抖。
- 时间标签是 12sp 的 **Emphasized**（不是常规体），和正文拉开半档。

---

## 4. 歌词面板 `fragment_player_lyrics_sheet`

```
ConstraintLayout (background=?colorBackgroundFloating, marginTop=…)
├── LinearLayout 头部 (clickable, paddingStart=32dp, paddingEnd=16dp)
│   └── MaterialCardView (corner=5dp, stroke=0.5dp)
│       ├── TextureView 60dp         ← 小封面（共享元素）
│       └── PlayerTransitionImageView
├── ...曲名 / 艺人 / 无损徽标
└── FlexboxLayout 歌词主体
```

---

## 5. 逐词卡拉 OK

`lyrics_line_karaoke.xml`：

```
ConstraintLayout (paddingBottom=…, marginStart/End=lyrics_line_padding_horiz=32dp)
├── FullWidthAlphaGradientFlexboxLayout   ← 主行：词级 flex 排版
│   ├── alignItems=4 (baseline), flexWrap=1
│   ├── paddingTop / paddingStart/End
│   └── constraintWidth_default=2 (wrap)
└── FullWidthAlphaGradientFlexboxLayout   ← 第二行（翻译 / 音译 / 背景人声）
    └── constraintTop_toBottomOf=主行
```

要点：

1. **一行歌词是一个 Flexbox，不是 TextView**。每个词是独立子项，才能做逐词高亮与"词内换行"。
2. **`FullWidthAlphaGradientFlexboxLayout`** 是自定义控件：给整行做**两端渐隐**（alpha gradient），
   长行贴到屏幕边缘时自然淡出，不需要缩字号。
3. 主行与附加行（`x-bg` 背景人声 / `translation` / `pronunciation`）是**两个并列的 Flexbox**，
   垂直串联 —— 与 native 侧 `processSpanElements` / `processTranslationSpanElements` /
   `processPronunciationSpanElements` 三个解析入口一一对应。
4. `lyrics_line_padding_horiz = 32dp`：歌词左右各留 32dp，比正文页边距大一倍，
   因为歌词要"居中成一列"。

---

## 6. Sonic 现状对照

| Apple | Sonic 现状 | 差距 |
|---|---|---|
| 三颗运输键，间距 40dp | `TransportRow`：上一首/播放/下一首，间距 32dp | 间距改成 40dp |
| 运输键内边距 11dp | `transportSize` 56dp 固定 | 改为 padding 驱动的命中区 |
| 细线进度条，垂直内边距 5dp | `PlayerSeekBar` 自绘 4dp 线 + 触摸 28dp | 视觉已接近，需对齐 5dp 与时间行 10dp |
| 时间 12sp Emphasized + 等宽数字 | `AppleType.caption1Emphasized`，中间空着 | ✅ 已换成 Apple 字阶；中间位补上音质徽标（见 §9） |
| 页脚三颗（歌词/随机/队列）均分 | `PageSelectorRow` 已均分，选中 100% / 其余 45% | 结构一致，颜色改 Apple 令牌 |
| 背景层：整屏 `LyricsBackgroundLayerView` | `SonDynamicBackdrop`（封面取色动态色面） | 概念等价，Apple 是模糊图层，Sonic 是色面 |
| 一行歌词 = Flexbox + 词级子项 | `KaraokeLineText` 已按词切分 | 结构一致，两端渐隐不复刻（见 §7） |
| 歌词左右 32dp | 需核对 `LyricsLayoutCalculator` | 对齐 32dp |

---

## 7. 改造进度

| 步骤 | 状态 | 说明 |
|---|---|---|
| 几何对齐 | ✅ 已完成 | `transportGap` 32→40dp、`seekVerticalPadding` 5dp、`timeRowMargin` 10dp、时间行改 `AppleType.caption1Emphasized` |
| 歌词两端渐隐 | ⛔ 已移除 | 曾挂过 `Modifier.appleEdgeFade()`，但行容器本身有 16dp 横向 padding，渐隐区（24dp）正好压在首尾字形上：每行第一个字母被吃掉一半、长行右端发灰。原版之所以需要渐隐，是因为它的 Flexbox 贴边排字；Sonic 的歌词会换行且不贴边，渐隐只会伤可读性 |
| 场景切换动效 | ✅ 已对齐 | 原来是「淡入 + 上下位移」，改为**纯交叉淡入** —— 原版 `ViewSwitcher` 的 in/out 就是 `fade_in` / `fade_out`，位移是多余的重复表达 |
| 封面折叠 | 已具备 | `NowPlayingSurface` 本来就用 `SharedTransitionLayout` + 共享封面元素，封面在满宽大图与 48dp 顶部小图之间连续变形，无需重做 |
| 歌词区整屏铺满 | ✅ 已修复 | 原布局把歌词夹在歌曲栏**下方**，滚到边界的行被硬切成半行。改为 `Box` 叠放：歌词 `fillMaxSize`、顶栏浮在其上，行从栏后淡出；焦点行让出 `headerInset`（= 64dp），高亮行视觉位置不变 |
| 顶栏内歌词收口 | ✅ 已修复 | 顶部渐隐原先从屏幕顶端按视口高度 16% 算，顶栏（64dp）之内的歌词仍以半透明状态露在歌曲名 / 歌手名上，与标题叠字。改为以 `headerInset` 为界：栏内全透明，栏下沿再用 48dp 渐亮回来 |
| 播放页下拉收起 | ✅ 已重做（第四版） | 收起只有**一个变换**：`PlayerMorph.shell` = 等比缩到 0.62 + 下沉 0.67 屏高 + 尾段淡出，三项全部线性跟着 `progress`。下沉量由「顶边 1:1 跟手」反推：`0.86 − (1 − 0.62)/2`。透明度只在最后 16% 才降，圆角脱离屏幕边缘后到 28dp；手势位移按 `scaleAt` 补回缩放坐标系。前几版的两条路（矩形 morph 到迷你胶囊、按卡片重排内容 + `RemeasureToBounds`）都已删除：前者进度走满画面几乎不动，后者只有某一小段看得见 |
| 封面共享元素 | ✅ 已做 | `PlayerArtworkTransition`（CompositionLocal 收几何）+ `PlayerArtworkFlight`（根坐标里唯一的那张图：按屏宽见方排版一次，之后只改 `graphicsLayer`）。迷你条封面与播放页封面各自在飞行期间让开，中途只剩这一份：`dive` 之前跟着整页的缩放 / 下沉走，之后插值进迷你条的缩略图；圆角 28dp → 5dp、投影同步归零 |

> 注意：`KaraokeLineText` 不再开离屏层。之前每行一个 `appleEdgeFade` 离屏层，
> 顺带把首尾字形擦掉一半，已随渐隐一起删掉。

---

## 8. 队列页的四档模式胶囊

队列页顶部原来只有两颗无底色的图标按钮（随机 / 循环）+ 右侧「当前 / 总数」，
版式照 `MeloXQueuePanel.QueueModeControlsSurface` 改成四颗等宽胶囊。

| 档位 | 图形 | 绑定的真实开关 |
|---|---|---|
| 随机 | `SonGlyph.Shuffle` | `PlayerIntent.ToggleShuffleMode`（播放器内） |
| 循环 | `SonGlyph.Repeat` / `RepeatOne` | `PlayerIntent.ToggleRepeatMode`（播放器内） |
| 智能续播 | `SonGlyph.Infinity` | `SettingsRepository.autoContinueEnabled`（跨会话偏好） |
| 淡入淡出 | `SonGlyph.AutoMix` | `SettingsRepository.audioFadeDurationMs`：0 = 关，开则回默认 200ms |

几何：高 48dp（`PlayerSceneSpec.selectorHeight`）、圆角 22dp、间距 12dp、四等分（`weight(1f)`）。
选中档白 70% 底 + 黑 62% 图标，未选中白 12% 底 + 白 86% 图标，按下时短促加深 ——
队列页永远压在封面色面上，所以这排只做浅色档，不跟明暗主题走。

「当前 / 总数」从原来的行尾挪到胶囊下面右对齐：四颗胶囊把整行宽度占满，再挤一个计数会
把胶囊压成小方块。

后两档写的是**设置**而不是只改内存里的播放器开关：它们本来就是「跨会话偏好」，
只改内存的话，退出播放页再进来（或重启）就退回设置页里那个值，按钮看起来像自己弹回去了。

图标一律走 XML 矢量资源（`res/drawable`），造型一律按 Apple Music：前两档复用
`ic_flamingo_shuffle` / `ic_flamingo_repeat`(+`_one`)，后两档是 `ic_mode_infinity.xml`（∞）
与 `ic_mode_automix.xml`（◉ 圆环 + 中心点）—— 对应 Apple Music 队列条的 Autoplay 与 AutoMix。

现有 Flamingo 1.0.0 那份抽取里没有这两颗（当时只抽了播放页用到的二十来个），所以按 SF 的
描边语言（只描边、2.0 圆头、viewport 收到包围盒）手写成 XML，与 `ic_flamingo_*` 同一套约定：
尺寸写 intrinsic dp、颜色留给 `Icon` 的 tint。拿到新抽取资源后**同名覆盖**这两个文件即可，
调用点（`R.drawable.ic_mode_*`）不用动。

不走 Material Symbols 子集字体（12KB）：它没有这两个码位，往枚举里加只会渲染成豆腐块。

---

## 9. 时间行中间的音质徽标

Apple Music 在进度条下面一行是「已播 · 音质徽标 · 剩余」，Sonic 原来只有左右两个时间，
中间是空的。现在补上：

| 档位 | 徽标 | 判定 |
|---|---|---|
| Hi-Res Lossless | `ic_flamingo_lossless` + 「Hi-Res 无损」 | 无损容器且 `bitrate > 1411` |
| Lossless | `ic_flamingo_lossless` + 「无损」 | 无损容器且 `bitrate ≤ 1411` |
| 有损 | 不显示 | 与 Apple 一致：mp3 / aac 那一格留白 |

- 无损容器按 mime 与后缀判定（flac / wav / aiff / ape / wv / tta / dsf / dff）。
  `m4a` **不算** —— AAC 也用这个容器，放进来会把有损误判成无损。
- 阈值沿用设置页统计 SQL 的 `1411`（CD 码率），两边口径一致，不会出现「统计说 HR、
  播放页说无损」这种自相矛盾。
- 时间行从 `Row(SpaceBetween)` 改成 `Box` + 三处 `align`：左右两个时间的字符串长度不一样，
  `SpaceBetween` 会把中间那颗挤偏。行高 16 → 18dp，给 18dp 的图标留出上下余量。
- 图标不单独进无障碍树（`contentDescription = null`）：旁边的文字已经说了同一件事。
