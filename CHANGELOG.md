# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]

## [1.0.1] - 2026-10-21

### Fixed
- 🎚 **播放设置面板里的「频谱条」开关点不动**：面板内容（倍速滑杆 + 两排倍速芯片 + 定时关闭 +
  新增的一行开关）已经比一屏高，最后一行被顶到屏幕底边，开关的点击区域落在可视区之外。
  面板改成 `verticalScroll` + `navigationBarsPadding`，副标题缩成一行，整行可点（开关只有 48dp 触点，
  贴边时很容易点空）。
- 🔇 **在线音源「只有网易云能播」**：取链成功但播放 404，根因有两层：
  - 音源脚本只负责**拼 URL**，从不访问它。长青音源（`yinyue.haitangw.net`）的 PHP 路径全部 404
    （连根路径都回一个假 200 的错误页），脚本却照样返回「成功」，排在它后面的幻音永远轮不到。
    新增 `PlayUrlProbe`：取链成功后先 `GET` + `Range: bytes=0-1` 探一次，2xx/206 才算成功，
    否则当作该插件失败、继续试下一个（`LxPluginManager.fetchPlaySource`）。
  - 幻音的 tx/kw/wy 走 `music-dl.sayqz.com`（已连不上）、mg 走 `api.xcvts.cn`（502 咪咕接口全挂）；
    星海只有 wy 可用（tx 要授权，kg/kw/mg 后端报错）。设备上换成 `1010🎵音源`（手机版，全链路 HTTPS）
    后，酷我实测可直接播放。
- 🖼 **在线搜索结果的封面缺失（网易云 / 酷我）**：
  - 网易云改用 `api/cloudsearch/pc`（`api/search/get/web` 只给 `album.picId`，拼封面要过一层加密），
    直接取 `al.picUrl` 并升到 https；
  - 酷我补读 `web_albumpic_short`，拼成 `https://img1.kuwo.cn/star/albumcover/{path}`；
  - 酷我接口本身只对约一半条目返回该字段（实测 5/10），所以再加一层**跨平台借封面**：
    同「歌名 + 首位歌手」的条目里只要有一条带封面，其余几条直接借用（`borrowMissingCovers`），
    零额外请求，匹配键含歌手所以不会串味。
- 🔇 **在线音源「搜到歌但一首也播不了」**：取链是成功的，播放却全挂 —— ExoPlayer 抛
  `CleartextNotPermittedException`（真机日志里 126 次），因为脚本返回的是 http 地址
  （`http://m704.music.126.net/…`、`http://yinyue.haitangw.net/…`），而 `targetSdk 36` 默认禁止明文。
  - 新增 `app/src/main/res/xml/network_security_config.xml`（`base-config cleartextTrafficPermitted="true"`）
    并在 `AndroidManifest` 里引用。域名随脚本而变、不可能枚举，只能整体放行；HTTPS 不受影响
    （系统不会把 https 降级成 http）。
  - 这不是「为了省事放开明文」：在线音源的播放地址本来就由用户导入的第三方脚本决定，
    宿主无法预知 CDN 域名，LX 桌面端同理。
- ⚡ **真机 release 性能实测：播放页 / 歌词页在 120 Hz 屏上常驻空转**（R8 release 包，10 s 窗口）：
  - **暂停在播放页仍有 124 fps / 0.94 个 CPU 核**，违反 ADR-0005 的「暂停即冻结」。根因是
    `LinearWavyProgressIndicator` 的波浪是常驻动画：`waveSpeed` 默认等于波长，于是它按屏幕刷新率
    无限重绘整页，与是否在播放无关（第三轮在迷你播放器里踩过同一个坑）。现在 `waveSpeed` 跟随
    `isPlaying`，暂停时传 `0.dp` —— 波浪冻结成静态波形（形状不变），播放中仍然会流动。
  - **歌词页的逐字高亮 `frameTick` 不读 `animator_duration_scale`**：系统「移除动画」时整页仍按
    120 fps 重绘（真机设 `animator_duration_scale=0` 实测 124 fps）；暂停时倒是 0 帧（没有行在读它），
    但 tick 仍在每个 vsync 空唤醒。现在只在播放中且动画缩放 > 0 时跑。
  - 其余页面巡检（同一台 release 包）：歌曲列表 / 专辑 / 歌手 / 设置 / 详情页静止都是 **0 fps**，
    滚动 P50 5–6 ms、超时帧 0–2.7 %；只剩两类一次性卡点：**进详情页转场 P90 19–38 ms**、
    **切歌换主题那一帧 P99 57 ms**（换主题 = 整个 App 重新组合 + 全量配色重算）。明细见
    `docs/benchmarking.md`「第五轮」。
  - 其余基线：冷启动 302–310 ms、温启动 50–55 ms、首页静止 0 fps、歌曲列表甩动 P50 7 ms / P99 16 ms
    （120 Hz 屏上 124 fps 却 61–95 % 帧超 deadline，说明「帧数高」不等于流畅）。数据、复现与验收
    命令见 `docs/benchmarking.md`「第五轮」，规则见 `docs/design/motion.md`。
- ⏳ **每个页面都闪一下 loading**：`SonLoadingState` 早就带 240ms 延迟（本地数据几乎瞬间到达，
  快了就什么都不画），但一半页面还在用裸 `LoadingIndicator` 整屏画转圈 —— 歌单 / 收藏 / 歌单与
  专辑曲目 / 歌曲详情 / 排除文件夹 / 音乐库统计 / 歌词页首屏，进一次闪一次，看着像卡顿。
  - 把这些「本地数据」页面全部换成 `SonLoadingState`（或行内用新的
    `rememberLoadingIndicatorVisible()` 自行搭载入 UI），快于阈值时整片区域就是空白，
    与下一帧的内容切换无感；真慢的情况（首次 MediaStore 全量查询、SAF 枚举）仍然按时出现。
  - 真正需要立刻反馈的地方保持原样：AI 生成、在线封面 / 元数据搜索、歌词候选、扫描中、
    保存中、歌词下载按钮。
  - 规则写进 `AGENTS.md` 的设计体系一节，避免以后新页面又用裸 `LoadingIndicator`。
- 🎤 **进歌词页时「很卡地滚到当前歌词」**：歌词列表原来是 `rememberLazyListState()` 从第 0 行
  开始，再靠 `animateScrollToItem` 动画滚到当前行 —— 已经放了几十行时，那段动画会边滚边把
  中间每一行都组合出来（音节测量、渐隐遮罩、模糊层），看起来就是卡着滑过去。
  - 现在列表直接用 `initialFirstVisibleItemIndex` 从**当前播放行**开始铺；
  - 首次对齐与大跨度（拖进度条、切歌，超过 2 行）改成 `scrollToItem` **瞬移**，只有逐行推进
    才保留 `SonMotionTiming.scrollSpec()` 的 300ms 动画；规则写进 `docs/design/motion.md`。
- 🎨 **动态色面 / 封面配色仍然是品牌紫，不像封面**
  - 取色器的两个门槛互相打架：`backdropOf` 自己设了「HCT 彩度 ≥ 12」，而灰封面阈值是 16、种子彩度
    最低只被抬到 12 —— 淡封面（奶油、灰蓝、莫兰迪色）于是「种子被判成灰 → 回品牌色相」且
    「backdrop 全被筛掉 → 回落配色方案 container」，整页都是品牌紫。现在改为**直接用 `androidx.palette`
    的 vibrant / muted / dominant 档位**按色相挑 2~3 个，不再自己设彩度门槛（彩度不足由
    `SonMeshGradient.palette` 统一抬高到 20~56）；灰封面阈值 16 → 8，低于取色器的 12，保证抬过彩度
    的种子一定保留封面色相。`SonTonalSchemeTest` 的阈值断言同步更新。
  - 封面位图改成走 `CoverArtworkLoader`（借专辑下的曲目 `ContentResolver.loadThumbnail` → 图片流 →
    音频内嵌封面），与通知栏 / 桌面小组件同一条已验证路径，不再用 Coil 的 `execute` 读 legacy
    albums 行（`openInputStream` 在 Android 10+ 抛 `no such column: _data`，内存缓存还可能是硬件位图，
    `Palette` 取色会抛异常）。小组件里重复的封面读取代码一并删掉。
  - 只要有封面，`backdrop` 就不会为空：Palette 一个 swatch 都给不出来时用种子色兜底；品牌 container
    只留给「真的没有封面」。`rememberArtworkTheme` 现在会打一行 INFO
    `seed=#...... backdrop=#......`，真机不对时先看它。决策与根因记录在 ADR-0005 的修订段。
- 🌊 **动态背景「没有动」**：色相对了之后，真机上的色面看起来是完全静止的。用 `adb exec-out
  screencap` 相隔 12 秒取两帧对比，色面区域像素差最大只有个位数（同期 `dumpsys gfxinfo` 帧数
  正常、进度条波形在动，说明帧循环是活的）—— 问题在数值：相位走完 37.5% 的周期，采样坐标只
  挪了约 2 个纹素，双线性放大到全屏后根本看不出来。
  - 周期 32s → 16s（`SonMotionTiming.BACKDROP_CYCLE_MS`），域扭曲振幅 0.05 → 0.22、细节层
    0.022 → 0.08，并新增整体漂移 0.05（乘边缘衰减，最外圈不动），离屏位图 48² → 56²。
  - 改值前先用离线逐像素重算校准：旧参数一秒的变化平均 0.8/765（不可见），新参数约 8/765、
    三秒 20~26/765。`SonMeshGradientTest` 新增「一秒内动得出来」用例钉住这条下限，
    以后调慢周期或调小振幅会直接变红。决策记录见 ADR-0005 的「修订 2」。
- 🔎 **在线歌词「只出网易云」**
  - AMLL 搜索改成**只按曲名请求**、在本地校验歌手。实测服务端的 `artistName` 过滤不可靠：
    `musicName=Lemon&artistName=米津玄師` 返回 0 条，而 `artistName=米津` 能返回同一条
    `artistNames = ["米津玄師"]` 的文档 —— 服务端 AND 会把它自己有的歌词挡掉。本地校验按
    `feat.` / `ft.` / `with` / `&` / `、` 等切分歌手名、忽略大小写与标点，只放行同歌手的结果；
    同名不同歌手的歌词仍然不会串（`LyricsMatchTest` 覆盖这两条）。
  - 歌词缓存会一直赢到 TTL 到期，所以新增 AMLL 之后，已经放过的歌仍然命中旧的网易云行 ——
    新来源对它们等于不存在。新增 `LyricsCacheGenerationStore`（DataStore `lyrics_cache_meta`）
    与 `LYRICS_CACHE_GENERATION`：代号落后时把 `lyrics_cache` 整表清一次，下一轮播放按当前
    来源顺序重查；用户手写歌词在 `lyrics_override`，不受影响。
  - 单测补 `LyricsRepositoryImplTest` 的「代号落后只清一次 / 代号已是最新不动缓存」。
- 🐛 **换封面后仍显示上一张封面**：自定义封面文件固定叫 `covers/<songId>.jpg`，每次更换只是覆盖同一个
  文件，而 Coil 的内存缓存以 URI 为键 —— 于是先应用网络封面、再选本地图片（或反过来）时，
  界面继续显示上一次的图，看起来像「选了本地图片，却更新成网络封面」。
  现在每次更换都写一个新文件 `covers/<songId>-<时间戳>.jpg`，数据库里的 `customAlbumArt` 指向它，
  URI 一变缓存自然失效；旧文件保留 7 天后在下次更换时清理（给仍在引用旧 URI 的通知栏留出时间），
  「恢复默认封面」仍立即删除全部文件。
  `coverFileName` / `isCoverFileForSong` 抽成纯函数，由 `CoverFilesTest` 钉住命名与旧文件兼容规则。

### Added
- 🏅 **播放页封面左下角的「小金标」（`HiResBadgeOverlay`）**：复刻 Salt Player 的自定义小金标，
  四档内置样式（Hi-Res Audio / Hi-Res 无线 / 圆形 HR 小金标 / 关闭）+ 自定义图片。标随封面尺寸走，
  空态占位图不贴。
  - 自定义图片按 Salt 的做法**拷进应用私有目录**（`files/hi_res_badge/`）再存 `file://` 路径，
    不再依赖 SAF 的临时授权 —— 存 `content://` 会在授权失效后变成一块空白；换图时旧文件一并删掉。
  - 纯装饰：不读位深/采样率 —— 标上去的是用户的选择，不是音质结论。金色/银色对（`hiResBadgeGold`
    等）进 `SonExtendedColors`，因为压在封面上的这枚标志在两种主题下必须是同一枚。
- 🎤 **歌词界面增强（字号 / 居中对齐 / 辉光 / 模糊）**：对齐 Salt Player 的效果形态。
  - 入口在**歌词页顶栏的设置按钮**（底部弹出面板），不再放设置页：这几项都是"看着这一句觉得不对"
    时才想起来调的，跳回设置页再回来会打断正在看的位置、也要重新滚动到那一行。
  - 字号改成 **12–48sp 连续滑杆**（拖动只改本地值，松手才写 DataStore；面板被划走时补一次提交）；
    行高按主题 token 的比例跟着变，只放大字号会把行挤在一起。伴奏行按两者原始字号的比值跟随，
    比例关系不变。
  - **居中对齐**：布局计算里多一种 `isLineCentered`（`(可用宽 - 行长) / 2`），缩放原点同步挪到中间，
    逐字高亮的坐标也走同一套计算，所以居中后卡拉 OK 进度仍然对得上。
  - 模糊开关**在 Android 12 以下置灰并说明原因**：`Modifier.blur` 走 RenderEffect，低版本没有这条路径。
  - 四项都只改样式与绘制，不改歌词数据：切档位不需要重新解析、重新测量整份歌词。
- 🎚 **播放页底部新增频谱条（`BeatSpectrum`）**：节拍分析（`BeatState` 的 level/pulse/beat）驱动一排
  36 根柱子，贴在屏幕底边、落在 `bottomBarClearance` 的留白里，不改动上面任何一块版式。
  - 为什么不放在封面旁边：原先封面背后的两圈同心环半径取到封面边长的 1.10–1.24×，而封面已经铺满
    可用宽度，环被屏幕两侧裁掉、内侧又被封面盖住 —— 等于看不见。频谱条独立成一条，语义清楚也不抢封面。
  - 沿用「常驻动画自己收口」的约束：`beat` 是 lambda，只在 `Canvas` 绘制阶段读快照；暂停 / 静音时
    整条不画（`BeatState.isSilent` 早退），柱形分布用 `sin(i)` 计算，绘制路径里不分配对象。
  - 播放页「播放设置」面板里加了开关（`playerSpectrumEnabled`，默认开、存 DataStore）：它是这一屏
    新增的唯一自发重绘来源，觉得费电可以当场关掉；关掉后播放页只剩进度条那处波浪在动。
- 🔎 **在线音乐终于有播放入口：搜索页直接搜、点一条就播**
  - 之前只有「在线音源」的设置管理面：脚本能导入、能测试，但全 App 没有任何地方能把在线歌曲播出来
    （`ResolveOnlinePlaySourceUseCase` / `MusicInfo.toSong` 写好了却零引用）。
  - 根因是**搜索层缺失**：LX 音源脚本协议只有 `musicUrl`（给定平台歌曲 id 换播放地址），**没有 search**，
    宿主不自己搜就没有 `songmid` / `hash` 可交给脚本。
  - 新增 `OnlineSearchRepository`（domain）+ `OnlineCatalogSearchService`（data）：并行打
    网易云 `api/search/get/web`、QQ `client_search_cp`、酷狗 `song_search_v2`、酷我 `r.s`、
    咪咕 `search_all.do`，产出带平台 id 的 `MusicInfo`（网易云 `song_id`、QQ `songmid`、
    酷狗 `FileHash`、酷我 `rid`、咪咕 `contentId`+`copyrightId`）。单平台失败只丢那一个平台。
  - 两个接口坑：酷狗带上 `tagtype=0` 会回 `status=1` 但 `lists` 恒为空；酷我标着 `rformat=json`
    却回单引号伪 JSON，要先归一化引号再解码。
  - 搜索页新增「在线音乐」区块：只搜**已启用插件声明支持**的平台（插件没声明 `kg` 就别展示酷狗结果），
    点击 → `ResolveOnlinePlaySourceUseCase` 取链（固定 320k，插件自行降级）→ `MusicInfo.toSong` →
    既有播放器，行尾有取链转圈。空态区分「没装音源」与「没搜到」。
  - 每页每平台 10 条（5 个平台 = 50 条），底部「加载更多」翻下一页并追加；翻页按 `平台:id` 去重
    （接口偶尔会在相邻页重复同一条，重复 key 会让 `LazyColumn` 直接崩）。
  - 已知边界不变：取链返回的请求头（Referer/Cookie）还没进 `QueueItem`，要带头的源仍需后续补
    `ResolvingDataSource`。
- 🎵 **在线歌词新增 QQ 音乐 / 酷狗音乐来源**
  - `LyricsProvider.QQ_MUSIC`（`data/api/lyrics/QQMusicLyricsProvider.kt`）：`client_search_cp` 搜索
    （复用封面 provider 的 DTO，补上 `songmid`）→ `fcg_query_lyric_new.fcg?songmid=` 取歌词。返回的
    是整行 LRC，且可能带一份同时间戳的翻译 LRC —— `mergeQqTranslation` 按时间戳把译文插到原文行下面，
    而不是把两份文档拼在一起（LRC 解析器只把「紧跟原文、时间戳相同」的行当译文）。
  - `LyricsProvider.KUGOU`（`data/api/lyrics/KugouLyricsProvider.kt`）：`krcs.kugou.com/search`
    按关键词直接返回候选（含 `id` + `accesskey`，不需要先查歌曲 hash）→ `lyrics.kugou.com/download`
    取 base64 LRC。两个来源都按本地时长做容差筛选，翻唱/错版本会被挡掉。
  - 两者都只提供**整行**歌词：酷狗的 KRC、QQ 的 QRC 是加密容器，为一个次优文档单独写解析器不值得；
    整行缓存 1 天后会重新检查，届时仍可被逐字来源顶掉。
  - 默认来源顺序变为 `AMLL → 网易云 → QQ 音乐 → 酷狗 → LRCLIB`，设置页可拖动；`LYRICS_CACHE_GENERATION`
    2 → 3，装上新版本后会自动清一次歌词缓存，让已放过的歌按新顺序重查。
  - 单测 `LyricsProvidersTest` 钉住真实响应形态：时长筛选、base64 解码、QQ 的 JSONP 包装 / 非明文
    LRC、非零 `code` 表示「没有」、译文插入位置。
- 🌊 **播放页 / 歌词页：封面派生的动态色面**
  - 新增主题层 `SonDynamicBackdrop`（`core/ui/theme/SonDynamicBackdrop.kt`）：4×4 控制点网格渐变，
    颜色取自**封面上真实存在的颜色**（`ArtColorExtractor` 直接用 Palette 的 vibrant / muted /
    dominant 档位，挑色相彼此相差 ≥32° 的 2~3 档，经 `LocalSonBackdropColors` 注入），只保留色相、
    明度归一到背景可用区间后混到 `surface`；16 秒一轮的域扭曲让它缓慢流动。
  - 不用配色方案里的 container 角色：`SonTonalScheme.Expressive` 会把 secondary / tertiary 的色相
    旋转 45°~120°，那两档颜色不在封面上，铺成背景会「不像封面」。没有封面或封面接近灰阶时才回退到
    container 角色。
  - 不依赖 shader / 模糊 / WebView：56×56 离屏位图 + GPU 双线性放大，minSdk 30 与更高版本行为
    一致；每帧只算约 3k 个像素，帧率上限 30fps。
  - 播放页色面在 hero 段最浓、运输控件区间收干净；歌词页整屏铺开并压暗 22%，保证正文对比度。
    其余页面仍是 ADR-0004 的静态 hero 光晕。
  - 暂停即冻结（不再请求帧）；系统关闭动画时停在第一帧；流动周期与帧间隔登记进 `SonMotionTiming`
    与 `docs/design/motion.md`。决策与被否方案见 `docs/adr/0005-dynamic-backdrop.md`，
    `SonMeshGradientTest` 钉住「不透明、循环无接缝、明暗分离、**色相来自封面颜色**」。
- 🎤 **新增 AMLL 逐词歌词库（官方 TTML HTTP API）**
  - 新来源 `LyricsProvider.AMLL`（`data/api/lyrics/AmllLyricsProvider.kt`）：先 `GET /v1/lyrics/search`
    按曲名找文档（歌手在本地校验，原因见 Fixed 里的实测），再 `GET /v1/lyrics/get?id=` 下载整份
    TTML。文档是社区逐字对齐的 Apple Music 形态 TTML，现有的 `TTMLParser` 直接能解析（含
    `x-translation` 翻译、`x-bg` 伴唱），没有新增解析器。
  - 不做时长容差匹配：AMLL 的 `duration` 是「歌词末尾时间戳」而不是音轨长度，拿它跟本地时长比对会把
    长尾奏的歌整首误杀。改成用专辑名给同一首歌的多个上架版本排序（同一张专辑优先），只排序不丢弃 ——
    单曲版与专辑版常常是同一段录音，硬过滤会白丢歌词。
  - 默认来源优先级改为 `AMLL → 网易云音乐 → LRCLIB`（AMLL 命中时质量最高，未命中只多一次索引查询）；
    已存过优先级的用户在末尾追加新来源，不覆盖用户自己排好的顺序，设置页里仍可上下移动。
  - 播放只取排在最前的一份（搜索 + 一次下载）；歌词版本选择面板最多列 3 份，按贡献者 GitHub 用户名区分。
  - `LyricsDtoTest` / `LyricsMatchTest` / `SettingsConversionsTest` 覆盖新旧响应形态、专辑排序与
    「新增来源追加」；`TTMLParserTest` 补了 AMLL 实际返回的紧凑 TTML（标签间无缩进、英文音节间空格是
    裸文本节点）作为兼容性护栏。
- 🏷️ **多格式标签编辑：不再只支持 MP3**
  - 新增 `:taglib` 模块：把本地 SaltAudioTag 工程的 `core`（LGPL-2.1，`taglib/LICENSE`）整包搬入，
    读写 FLAC（VorbisComment）、OGG-Vorbis、MP4/M4A（ilst），Opus 只读。`:data` 的
    `AudioMetadataHelper` 通过它补齐原先空缺的容器；MP3 仍走 mp3agic（ID3v2 + USLT 歌词 + ReplayGain）。
  - 写标签前先读出现有整份元数据，只替换用户改动的键，其余（ReplayGain、歌词、封面注释、未知标签）
    原样写回，图片单独传递保留；结果先写缓存临时文件再覆盖原文件，避免写一半损坏音频。
  - 顺手修了 `:taglib` 里两处会影响互操作的细节：Vorbis 的 `TRACKTOTAL` / `DISCTOTAL` 不再被折进
    `TRACKNUMBER`（否则 `3/12` 读不回来），MP4 的 `trkn` / `disk` 改成 iTunes 的二进制原子
    （上游写成 UTF-8 文本，只有它自己的 reader 能读）。
  - OGG 的封面在 VorbisComment 里是 `METADATA_BLOCK_PICTURE` 注释，`:taglib` 的读取映射表里补上
    `METADATA_BLOCK_PICTURE` / `COVERART` 透传，否则改一次标签就会把封面抹掉。
  - 编辑页的提示与保存提示改为按容器判断（`AudioMetadataRepository.isTagWriteSupported`）：
    可写格式写文件、失败或不可写格式落 `song_metadata` 覆盖表；`.m4b` 映射成 `m4a` 交给标签库。
  - `kotlinx-io 0.7.0`（`:taglib` 的字节流/临时文件抽象）；`AudioMetadataMappingTest` 钉住键名映射、
    「只改编辑字段」的合并规则与年份/音轨号归一化。
- 🔎 **歌曲详情页：从在线目录补全元数据**
  - 编辑模式新增「获取在线信息」：复用封面的在线搜索（网易云 → QQ → 酷狗 → iTunes，命中即止），
    把命中的 标题 / 艺术家 / 专辑 / 专辑艺术家 / 年份 / 流派 / 音轨号 / 碟号 填进编辑器；
    只填来源确实给出的字段，不覆盖用户已经填好的内容，且仍需点页面「保存」才落库
    （与歌词版本选择、LRC 元数据写回同一条规则）。
  - `CoverCandidate` 扩展为「在线目录命中」：除封面 URL 外增加 `durationMs` / `year` / `genre` /
    `trackNumber` / `discNumber` / `albumArtist`；四个来源按各自返回补齐可用字段 ——
    NetEase 用 `/api/song/detail` 的 `publishTime` / `no` / `disc` / `album.artist`，
    QQ 用 `pubtime`，iTunes 用 `releaseDate` / `primaryGenreName` / `trackNumber` / `discNumber`，
    酷狗搜索接口没有这些字段就留空。
  - 封面面板与元数据面板共用同一批搜索结果与加载状态（`onlineCandidates` / `isLoadingOnlineCandidates`），
    一次搜索两处可用；元数据列表展示来源、年份、音轨、流派，便于判断该不该套用。
- 🏷️ **歌曲详情页：全字段元数据编辑 + 封面管理（阶段一）**
  - 编辑模式新增 专辑艺术家 / 音轨号（`3/12`）/ 碟号（`1/2`）/ 作曲 / 出版商 / 版权 / 备注
    输入框，连同原有的标题、艺术家、专辑、年份、流派，详情页基本覆盖 ID3 的主要标签字段。
  - 编辑结果写两处：先落 `song_metadata` 覆盖表（DB v7，`MIGRATION_6_7`，随 `songs` 级联删除），
    再对 MP3 尽力写回 ID3v2 标签。原因是应用对媒体库只有读授权、写标签可能失败，而且重扫会把
    `songs` 里扫描来的列改回去 —— 只写文件或只写数据库都会让用户的修改在某条路径上丢失。
  - 非 MP3（FLAC/OGG/M4A）只写数据库并在保存后提示「文件标签未写入」，不再像以前那样
    静默假装成功；`AudioMetadata`/`TagUpdate` 扩展到 12 个字段，`AudioMetadataHelper`
    读写 `TPE2`/`TRCK`/`TPOS`/`TCOM`/`TPUB`/`TCOP`/`COMM`，未知流派名跳过 genre 写入
    而不是让整次保存失败。
  - 删除 `:data` 里与 `:domain` 重复的 `TagUpdate`/`AudioMetadataResult`，仓库直接复用领域模型。
  - **封面管理**：编辑模式点「更换封面」打开面板，可 ①从相册选图（系统照片选择器）
    ②在线搜索封面 ③恢复默认封面。在线来源依次尝试 网易云音乐 → QQ 音乐 → 酷狗音乐 → iTunes，
    命中即止；候选列表显示来源、专辑、艺人与「最佳匹配」。
  - 选中的封面被下载到应用私有目录 `files/covers/<songId>.jpg`（解码校验 + 压缩到 640px JPEG），
    `songs.customAlbumArt` 指向它，播放页/通知/列表统一经 `albumArt` 合并值取到，重扫不会丢失；
    「恢复默认封面」删掉私有文件并把列置空，回落到 MediaStore 的封面。
  - 新增 `CoverRepository` / `CoverArtProvider`（domain）与四个 provider 实现，
    纯解析函数有单测（`CoverProvidersTest`、`CoverMatchTest`）。
- 🎛️ **歌曲详情页：获取歌词 / 选择版本 / 歌词信息**
  - 编辑模式的歌词输入框上方新增「获取歌词」：拉取在线来源的版本列表（与播放页版本选择器同源），
    点选一个版本填入编辑器，保存后才落库 —— 编辑页上的东西在点保存之前都不该生效。
  - 同一行新增「歌词信息」：查看来源（手写 / 内嵌标签 / 旁车文件 / 在线下载）、在线提供方、
    时间轴（逐字 / 行级 / 无）、行数、格式与有无译文；查看模式的歌词标题右侧也有同一入口。
  - 信息面板在编辑模式下可修改 LRC 头部元数据（`[ti:]` / `[ar:]` / `[al:]` / `[offset:]` / `[by:]`），
    「写回歌词」把它们写进编辑器里的歌词文本（`[length:]`、`[tool:]` 与其他未知标签原样保留）；
    另有「填入歌曲标签」把 `[ti:]` / `[ar:]` / `[al:]` 一键填进标题 / 歌手 / 专辑表单，空值不覆盖已填内容。
  - `LrcMetadataHelper` 新增 `parse(content)` 与幂等的 `applyAttributes(content, attributes)`；
    `Attributes` 增加 `by`；`Lyrics` 增加 `provider`，仓库在缓存命中与在线下载时填充，
    使「在线下载」能说清是网易云还是 LRCLIB。
  - 在线歌词关闭时「获取歌词」直接提示去设置里开启，而不是弹一个空列表。
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
- ➖ **播放页进度条去掉波浪，改成静态直线**：原来用 `LinearWavyProgressIndicator`，播放中波浪按
  屏幕刷新率无限重绘整页。现在与迷你播放器一致，换回静态 `LinearProgressIndicator`，
  只在 `progress` 变化时重绘；拖拽 / 点按 seek 的手势逻辑不变。规则同步写进 `AGENTS.md` 与
  `docs/design/motion.md`（常驻进度一律静态，波浪只留给扫描这类短生命周期界面）。
- 🔍 **歌词当前行放大更明显**：聚焦 / 非聚焦的缩放从 `1.0 : 0.98` 调到 `1.04 : 0.92`，
  体量差从 2% 拉到 13%，正在唱的那一句一眼就能挑出来；缩放原点仍随对齐方向，居中与靠右不跑位。
- 🌊 **歌词去掉逐字上浮的「波浪」动效**：普通行原本从下方 4px 浮到基线（700ms），逐字行按
  `DipAndRise` 曲线让每个字先下沉再升起，两者叠起来就是一句话里的字形上下起伏。现在字形固定在
  排版位置，只保留卡拉 OK 刷色推进与逐字放大 / 辉光，滚动与高亮对齐不变。
  `NewtonPolynomialInterpolationEasing` 里因此失去使用者的 `DipAndRise` 一并删除。
- 🎨 **应用主题改为跟随系统壁纸，不再按封面换肤**：`SonicTheme` 的 `dynamicColor` 默认打开
  （Android 12+ 走 `dynamicLight/DarkColorScheme`，低版本回落品牌种子色 `SonicSeed`），
  主界面、专辑/歌单详情、音乐库首页都跟着壁纸走。
  - 封面只保留一个职责：给播放页与歌词页的**动态色面**提供颜色。`rememberArtworkTheme`
    相应瘦身成 `rememberArtworkBackdrop`，只产出 swatch、不再生成整套配色方案 ——
    也就省掉了每次切歌一遍上千次的 CAM16 迭代。
  - 移除两处"整页按封面重建主题"的嵌套 `SonicTheme`（`CollectionDetailScreen` / `MyMusicScreen`）：
    详情页换一张封面就换一整套配色，既没意义，也和壁纸主题打架。
- 📝 **播放页标题单行 + 跑马灯，整屏改为不滚动**：标题原来允许 2 行并省略号，长标题会把版式
  撑高、把运输控件顶出屏幕。现在标题固定一行（`maxLines = 1` + `softWrap = false`），
  放不下时用 `Modifier.basicMarquee()` 横向跑马灯；页面去掉 `verticalScroll`，封面改成
  `weight(1f) + aspectRatio(1f)` 吃掉剩余高度 —— 屏幕矮时封面自己缩小，控件永远在屏幕内。
- 🎛 **播放页运输控件去掉浮动胶囊**：原来是官方 `HorizontalFloatingToolbar` 的胶囊容器，
  在封面派生的动态色面上等于又盖一张不透明面板，色面被切成上下两段；现在只留一排图标
  （shuffle → prev → play → next → repeat，`SpaceEvenly` 拉开），色面一路铺到屏幕底部。
  96dp `LargeFloatingActionButton` 保留 —— 按压形变（28dp 圆角 → 圆）是这一屏唯一的实心形状，
  也是它唯一的 expressive moment；真机截图见用户反馈（2026-10-20）。
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
- `BeatReactiveArtwork`（封面背后的两圈同心环）：半径取到封面边长的 1.10–1.24×，而封面铺满可用宽度，
  环的左右被屏幕裁掉、内侧被封面盖住，实际一帧都看不见。播放页封面改回直接使用 `M3Artwork`，
  律动统一由底部 `BeatSpectrum` 表达。
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
