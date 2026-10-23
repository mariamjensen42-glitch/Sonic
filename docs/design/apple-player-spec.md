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
| 时间 12sp Emphasized + 等宽数字 | `labelMedium`（Material 字阶） | 换 `AppleType.caption1Emphasized` |
| 页脚三颗（歌词/随机/队列）均分 | `PageSelectorRow` 已均分，选中 100% / 其余 45% | 结构一致，颜色改 Apple 令牌 |
| 背景层：整屏 `LyricsBackgroundLayerView` | `SonDynamicBackdrop`（封面取色动态色面） | 概念等价，Apple 是模糊图层，Sonic 是色面 |
| 一行歌词 = Flexbox + 词级子项 | `KaraokeLineText` 已按词切分 | 结构一致，缺两端渐隐 |
| 歌词左右 32dp | 需核对 `LyricsLayoutCalculator` | 对齐 32dp |

---

## 7. 改造进度

| 步骤 | 状态 | 说明 |
|---|---|---|
| 几何对齐 | ✅ 已完成 | `transportGap` 32→40dp、`seekVerticalPadding` 5dp、`timeRowMargin` 10dp、时间行改 `AppleType.caption1Emphasized` |
| 歌词两端渐隐 | ✅ 已完成 | 新增 `Modifier.appleEdgeFade()`（离屏层 + `BlendMode.DstIn` 水平渐变），挂在 `KaraokeLineText` 的行容器上 |
| 场景切换动效 | ✅ 已对齐 | 原来是「淡入 + 上下位移」，改为**纯交叉淡入** —— 原版 `ViewSwitcher` 的 in/out 就是 `fade_in` / `fade_out`，位移是多余的重复表达 |
| 封面折叠 | 已具备 | `NowPlayingSurface` 本来就用 `SharedTransitionLayout` + 共享封面元素，封面在满宽大图与 48dp 顶部小图之间连续变形，无需重做 |
| 歌词区整屏铺满 | ✅ 已修复 | 原布局把歌词夹在歌曲栏**下方**，滚到边界的行被硬切成半行。改为 `Box` 叠放：歌词 `fillMaxSize`、顶栏浮在其上，行从栏后淡出；焦点行让出 `headerInset`（= 64dp），高亮行视觉位置不变 |
| 顶栏内歌词收口 | ✅ 已修复 | 顶部渐隐原先从屏幕顶端按视口高度 16% 算，顶栏（64dp）之内的歌词仍以半透明状态露在歌曲名 / 歌手名上，与标题叠字。改为以 `headerInset` 为界：栏内全透明，栏下沿再用 48dp 渐亮回来 |

> 注意：`appleEdgeFade` 每条歌词行都会开一个离屏层。歌词列表通常只组合 10~15 行，
> 这个开销可接受；如果后续出现掉帧，优先给非当前行关掉渐隐。
