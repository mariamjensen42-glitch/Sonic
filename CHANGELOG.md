# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

### Added
- 🎵 **歌曲详情页歌词展示与手写编辑**
  - 详情页在头图之后、文件信息之前新增「歌词」区块：逐字（卡拉OK）与行级歌词都能显示，
    译文作为次级文字排在原词下方；超过 12 行折叠，超出部分用「展开全部 / 收起」按钮控制。
  - 加载中显示加载指示器；没有歌词时给出「暂无歌词」与「添加歌词」入口，空态不再只有一行灰字。
  - 编辑模式在流派之后新增多行歌词输入框：存的是原始文本（含 LRC 时间戳），往返一次不会
    把可跟随的歌词变成不可跟随的纯文本。留空并保存＝清除自定义歌词、恢复自动获取。
  - 歌词保存走数据库而非写文件：应用对媒体库只有读授权，在受分区存储约束的设备上写文件会
    静默失败，所以手写歌词落在自己的表里。
  - 新增 `LyricsSource.USER`（优先级高于一切自动来源：内嵌标签、旁车 `.lrc`、在线下载），
    `LyricsRepository.saveLyrics` / `clearLyrics`，`SaveLyricsUseCase`，`lyrics_override` 表
    （DB v4，`MIGRATION_3_4`，随 `songs` 级联删除，不会残留成另一首歌的歌词）。
  - 播放页歌词：用户手写的歌词不再显示「选择歌词」与「重新获取」按钮——两者都会被手写版覆盖，
    点了没反应比没有按钮更糟；要改就在写它的地方改。
- 🧪 新增 `SaveLyricsUseCaseTest`（存入/清除两条路径）、`LyricsRepositoryImplTest` 四组用例
  （手写歌词优先级、无时间戳也照常返回、按 songId 而非指纹落库、清除交还自动来源）。
- 🔊 **播放增强：把三个已持久化但从未生效的开关真正接上**
  - **音量均衡（ReplayGain）**：`AudioMetadataHelper.extractReplayGainDb` 扫文件头部
    128KB 找 `replaygain_track_gain`（兼容 ID3v2 的 UTF-16 TXXX、Vorbis comment 的单字节明文），
    `PlayerManager` 按 `10^(dB/20)` 换算成 `ExoPlayer.volume`。正增益受播放器 1.0 上限截断，
    安静的歌不处理、响的歌压下来 —— 正是均衡要的效果。读标签走 IO，起播不等它。
  - **淡入淡出**：`PlayerManager` 按 `audioFadeDurationMs` 分 10 步推音量。换曲 / 恢复播放淡入，
    暂停与睡眠定时器到点先淡出再暂停。基础音量（ReplayGain）与渐变系数分开相乘，
    淡出不会把均衡结果一起抹掉。
  - **记住播放位置**：关掉后既不写入进度，恢复时也从 0 开始（此前是无条件保存与恢复）。
  - 设置页「播放」分区新增三个开关 / 选择器；淡入淡出时长 0–2000ms，每 100ms 一档。
- 🎛️ **播放页快捷控制面板**：顶栏新增「播放设置」入口，底部面板里直接调倍速（滑杆 + 5 档快捷
  芯片）与定时关闭（5 档时长 + 剩余倒计时 + 取消）。此前这两项只能退回设置页改。
- ☑️ **音乐库多选批量操作**
  - 歌曲列表：顶栏「多选」进入，全选 / 取消全选，底部操作栏批量「加入歌单 / 收藏 / 取消收藏」。
  - 歌单详情：顶栏「多选」进入，批量「移出歌单 / 收藏 / 取消收藏」。
  - 多选态下单击为勾选、行内更多菜单收起，退出多选会清空已选。
  - 新增 `SongRepository.setFavoriteBatch`（一次 `UPDATE ... WHERE id IN`）、
    `PlaylistRepository.removeSongsFromPlaylist`；提示语带数量，用户能确认勾的几首都进去了。
- 🧪 新增 `AudioMetadataHelperTest` 钉住三种真实标签字节布局（Vorbis / ID3v2 UTF-16 / 无标签）。

### Changed
- `EngineState` 新增 `playWhenReady`。淡出需要区分「缓冲中」与「未开始播放」—— `isPlaying`
  在缓冲时也是 false，用它判断会让缓冲期间按下的暂停键变成重新播放。
- 🧹 **移除自定义组件包装层**，页面直接使用 `androidx.compose.material3` 原生组件
  - 删掉 8 个与 Material API 一比一对应、纯转发参数的文件：`M3Button`、`M3Card`、`M3Chip`、
    `M3Indicator`、`M3Interaction`、`M3ListItem`、`M3Navigation`、`M3TextField`；
    并删除 `SonComponentShapes`（官方形状标尺已覆盖全部组件形状，不需要第二份清单）。
  - `core/ui/components/` 从 8 个文件收敛为 1 个 `MediaCards.kt`，只保留 Material 未提供的
    3 个组合：`M3Artwork`（封面加载）、`M3MediaCard`、`M3ArtistCard`。
  - 21 个页面／业务组件改为直接用 `Button` / `Card` / `ListItem` / `TopAppBar` /
    `LoadingIndicator` / `OutlinedTextField` / `Chip` 等原生组件，尺寸、形状、内容边距与
    排版全部交给官方默认值。
- 🔧 **修正依赖版本不匹配（重要）**：此前 UI 代码按 `material3 1.5.0-alpha29` 编写，但项目实际
  解析的是 BOM 钉定的 **1.4.0**——1.4.0 缺少 `LoadingIndicator`、`Linear/CircularWavyProgressIndicator`、
  `ListItem(onClick)`、`ButtonShapes` 等 9 个 API，其中 3 个被 30 处调用使用，代码无法编译。
  现按决策显式钉 `material3 = 1.5.0-alpha29`（覆盖 BOM 约束），并把 `composeBom` 升到
  `2026.09.00` 以保持 Compose 其余构件同一世代。
- ⬆️ **`compileSdk` 36 → 37（`compileSdkMinor = 1`）**：上述依赖的 AAR 元数据要求
  `minCompileSdk = 37`，四个模块统一上调；`targetSdk` 保持 36（不改运行时行为），
  `gradle.properties` 的 `android.suppressUnsupportedCompileSdk` 同步为 37。

- 🎨 **全量 UI 重构为 Material 3 Expressive**，彻底移除原 Spotify 风格，无旧样式残留
  - **配色**：改为品牌种子色 / 系统动态取色 / 封面种子色三条来源，全部经自研 HCT 色彩空间
    （`Hct.kt`）生成 5 条色调板并按官方 Lightness 映射表装配 `ColorScheme`（`TonalPalette.kt`）。
    Expressive 色料方案（高彩度 + 大色相偏移）为默认；播放时整套配色随当前封面实时切换。
  - **排版**：完整 Expressive 字阶，标准 15 档 + 强调（Emphasized）15 档，含字体族、字号、
    字重与行高的官方 token 值。
  - **形状**：八档圆角标尺（4/8/12/16/20/28/32/48）；按钮按压采用官方
    **形状变形**（胶囊 → 收窄圆角）而非缩放，由 Material 组件内部实现。
  - **动效**：接入 `MotionScheme.expressive()`，并暴露官方弹簧 token、时长与缓动曲线
    （`SonMotion`），页面不再出现裸 `tween`/`spring` 参数。
  - **组件**：重建按钮族（5 种尺寸 × 5 种变体 + 图标按钮 + FAB + 分段）、卡片、列表项、
    应用栏、指示器（形变加载指示器 / 波浪进度）、文本输入、芯片。
  - **导航**：底部导航使用 Material 标准的 `NavigationBar` + `NavigationBarItem`（三个目标等宽、
    图标与文字常显，指示器胶囊与涟漪均为官方默认）；图标体系整体迁移到 Material Symbols，
    删除全部 16 个自绘 vector drawable。
- 📝 同步 `AGENTS.md` / `CLAUDE.md`，新增「设计体系」强制规则，防止旧风格回归。

### Removed
- `SpotifyTheme` / `SpotifyColors` / `DesignTokens.kt` / `SpotifyBottomNavBar` 及其全部引用
- `:presentation` 下 16 个自绘图标 drawable 与 `:app` 下 3 个 `ic_spotify_*` 图标
- 全局 `indication = null`（已恢复 Material 默认状态层与涟漪反馈）
- 播放器/详情页的「三个原始颜色」临时取色方案，由统一色调映射取代

## [1.0.0] - 2025-02-21

### Added
- 🎵 音乐播放器核心功能
  - 支持本地音乐扫描和播放
  - 支持歌词显示（LRC、TTML 格式）
  - 支持播放队列管理
  - 支持后台播放和媒体通知控制
- 📁 音乐库管理
  - 按歌曲、专辑、艺术家分类浏览
  - 播放列表创建和管理
  - 收藏功能
  - 最近播放和最多播放记录
- 🔍 搜索功能
  - 全局搜索歌曲、专辑、艺术家
  - 搜索历史记录
- 🎨 UI/UX
  - iOS 风格界面设计
  - 深色/浅色主题支持
  - 卡拉OK式歌词显示
- 🤖 AI 功能
  - DeepSeek AI 集成
  - 歌曲信息智能展示
- 📊 统计功能
  - 音乐库统计
  - 播放次数统计

### Technical
- Clean Architecture + MVI 模式
- Jetpack Compose UI
- Hilt 依赖注入
- Room 数据库
- Media3 ExoPlayer
