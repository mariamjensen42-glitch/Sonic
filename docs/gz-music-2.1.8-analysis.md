# 瓜子音乐 2.1.8 静态分析记录

> 目标：`E:\deepseekworkspace\GZ_Music-2.1.8-release.apk`
> 目的：为 Sonic 的功能决策提供对照，不追求完整逆向。
> 分析日期：2026-10-21

## 1. 方法与边界

| 手段 | 用途 |
|---|---|
| REA（`jadx-headless-mcp` 0.7.1 + JDK 18.0.2.1） | 类结构、方法反编译、字符串 |
| `aapt2 dump`（build-tools 36.0.0） | 二进制清单树、资源表 |
| DEX 字节扫描（正则） | 全量类名统计、中文字面量、内嵌 URL |
| ZIP 成员清单 | 原生库、assets |

**证据性质**：以下全部为**静态观测**，APK 从未执行。签名未校验，运行时行为未观测；反射与动态加载的调用链属于未知。方法签名中的类型名来自反编译器，可能是别名。

**引擎身份**：`jadx-headless-mcp 0.7.1`，jar SHA-256 `6e5eacf5…73ff75`，单 worker，堆 4 GB。

## 2. 身份与清单

| 项 | 值 |
|---|---|
| 包名 | `com.gzmusic.app` |
| 版本 | 2.1.8（versionCode 38） |
| 应用名 | 瓜子音乐 |
| SDK | minSdk 24 / targetSdk 36 / compileSdk 37 |
| APK SHA-256 | `66e83c471a14ca4c2437d6234a72ff1cc09ff03c1315506ae3c8b3ce448a3202` |
| 规模 | 11474 个类（含依赖）/ 917 个资源 / 2 个 DEX / 17 MB |

**自定义权限**：`com.gzmusic.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`（signature 级）。

**权限（17 条）**，其中对产品形态有指示意义的是：

- `MANAGE_EXTERNAL_STORAGE` — 全盘扫描，非 MediaStore-only
- `SYSTEM_ALERT_WINDOW` — 桌面歌词悬浮窗
- `FOREGROUND_SERVICE_MEDIA_PROJECTION` — 系统播放内录
- `RECORD_AUDIO` + `CAMERA` — 听歌识曲采集 / 扫码
- `PACKAGE_USAGE_STATS` — 前台应用感知
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — 后台保活

**组件**

| 组件 | 说明 |
|---|---|
| `MainActivity` | 唯一 Activity；App Links `https://guazi.fun/listen`、`https://guazi.fun/s`，`autoVerify` |
| `core.media.MusicService` | `MediaSessionService`，exported，FGS mediaPlayback |
| `core.media.PlaybackCaptureService` | FGS mediaProjection，内录 |
| `feature.widget.NowPlayingWidgetProvider` | 桌面小组件 |
| `androidx.core.content.FileProvider` | `com.gzmusic.app.fileprovider` |

**元数据**：`lyricon_module = true`、`lyricon_module_author = 瓜子音乐`、`lyricon_module_description = "瓜子音乐原生歌词：逐行、逐字、翻译与罗马音"`。即本应用同时充当 Lyricon 的歌词模块宿主/插件。

## 3. 构建形态

- Kotlin + Compose，Hilt（`*_Factory`、`*_HiltModules` 遍布 feature 包），Room，DataStore，Media3 ExoPlayer，OkHttp。
- 原生库（全 ABI）：`libquickjs.so`（LX 脚本运行时）、`libflacJNI.so`、`libdatastore_shared_counter.so`、`libandroidx.graphics.path.so`、`libimage_processing_util_jni.so`、`libsurface_util_jni.so`。
- R8 混淆：`feature/*`、`core/*` 包名保留，单个类名压缩为 `a`/`b`/`sn0` 等；DEX 内中文文案未混淆。
- `assets/` 只有 baseline profile，没有内置音源脚本或配置——**在线能力完全靠用户导入**.

## 4. 功能全景（`com.gzmusic.app.feature`，845 个类）

| 包 | 类 | 要点 |
|---|---|---|
| `player` | 584 | 播放页、逐字歌词、CD/黑胶视觉、队列、音质/版本切换、睡眠定时 |
| `library` | 66 | 本地库、百度网盘、远程库、排序/筛选、封面匹配、文件夹管理 |
| `tablet` | 14 | NavigationRail、双栏、平板专用详情页 |
| `settings` | 15 | 缓存、均衡器、音质、桌面歌词、混播排除、设置内搜索 |
| `home` | 14 | 发现页：榜单、新专辑、心情电台、盲盒分类、公告条 |
| `user` | 13 | 登录、资料、云管理、赞助、试用、邀请 |
| `search` | 10 | 热搜、平台切换、跨平台聚合 |
| `downloads` | 7 | 下载与音质选择 |
| `playlists` / `playlist` / `playlistshare` | 17 / 8 / 4 | 歌单导入、排序、汽水转换、分享卡 |
| `listentogether` | 8 | 一起听房间（邀请/加入/同步效果） |
| `car` | 8 | 车机浏览页 |
| `navidrome` | 6 | Navidrome / Subsonic |
| `webdav` | 5 | WebDAV 扫描与浏览 |
| `recognize` | 5 | 听歌识曲 |
| `stats` / `leaderboard` / `toplist` | 4 / 5 / 6 | 听歌统计、排行榜、榜单 |
| `desktoplyrics` | 4 | 桌面歌词悬浮层 |
| `guess` / `shuffle` / `history` / `favorites` | 5 / 3 / 4 / 4 | 猜你喜欢、随机漫游、历史、收藏 |
| `artist` / `about` / `update` / `onboarding` / `invite` / `qrlogin` / `lx` / `widget` | 各 1–4 | 零散入口 |

`core` 下：`data`（437，`remote` 430 + `local` 7）、`lx`（229，LX 脚本运行时）、`media`（4）、`network`（1）。

**判断**：这是「本地 + 远程库 + 用户导入脚本 + 自家数据服务」四栖的外壳型播放器，重心在**在线取链与歌词**，本地扫描只是其中一源。

## 5. 深挖点

### 5.1 缓存管理（`feature.settings.CacheManagerSheetKt`）

类内含 6 个 composable：

| 方法 | 参数 | 作用 |
|---|---|---|
| `CacheManagerSheet` | (4 个 size 状态, boolean, …) | 面板主体 |
| `CacheTypeCard` | (name, `Long?`, canClear, selected, …) | 每类缓存一张卡，`Long?` 为 null 表示统计中 |
| `AutoCleanCard` | (state, enabled, …) | 自动清理开关 |
| `CacheLimitDialog` | (type, limitBytes, …) | 容量上限选择 |
| `CacheClearConfirmDialog` | (title, desc, …) | 清理前二次确认 |
| `HeaderButton` | (icon, text, enabled, …) | 头部「全部清理」 |

`CacheLimitDialog` 反编译原文给出淘汰语义（可直接引用）：

```java
GlassDialogKt.GlassDialog(..., type.c + "容量上限", null,
    type == sn0.IMAGE
        ? "重启应用后生效，超过上限会淘汰旧图片。"
        : "超过上限自动清理最久未使用的缓存。", ...)
```

- 图片缓存：上限交给 Coil/磁盘缓存自身，**改配置需重启**
- 其它类型：达到上限后按 LRU 淘汰
- 自动清理：运行期每小时检查；关闭期间到期的量，下次启动补清（来自文案字符串）
- 清理范围排除「整曲离线缓存」，避免影响离线播放

类字段：`long a`（推测为某类默认上限）、`int b`。**未确认**：缓存类型枚举 `sn0` 的三个取值——它不是顶层类，jadx 按名定位不到。

### 5.2 歌单导入：三条互不相干的通路

**A. LX（洛雪）备份 / 单歌单 JSON —— 纯本地解析**

`core.lx.LxPlaylistParser.parse(byte[])` 反编译原文：

```java
String text = decodeText(bArr);                                  // 编码嗅探（GBK/UTF-8）
Object data = json.parse(text).get("data");
if (data.has("lists")) return parseBackup(data);                  // 完整备份，多歌单
if (data.has("list") || data.has("name")) return parsePart(data); // 单歌单
// 否则 "暂不支持的 LX 歌单文件结构"
```

配套能力：`MAX_DECOMPRESSED_BYTES`（防解压炸弹）、`INTERVAL_REGEX` + `parseIntervalMs`（备份里的源更新间隔）、`LxTrack` / `LxUserPlaylist` 内部模型、`firstQualityHash`（洛雪备份的音质 hash）。

落库 `core.lx.LxPlaylistImporter`：

- `importAsNewPlaylist(name, songs)` / `importIntoPlaylist(playlistId, songs)` / 内部 `importSongs(playlistId, songs, createdNew, playlistName)`
- 算法（由 smali 还原，顺序确定）：
  1. 空列表直接返回零结果
  2. 取目标歌单**已有 songId 集合**
  3. 逐曲过滤：`title` / `source` / `sourceId` 任一为空则跳过（不计重复）
  4. 批内去重键 `"${source}_${sourceId}"`
  5. `songRepository` 落库得到 `songId`；已在歌单内则计入重复
  6. 批量加入后**重读歌单并回写曲目数与 updatedAt**
  7. 日志：`导入完成: N 新增=X 重复=Y 目标=Z`
- 结果类型（已确认字段）：`LxPlaylistImportResult(playlistId: Long, playlistName: String, createdNew: Boolean, addedCount: Int, duplicateCount: Int)`

**B. 分享链接导入 —— 依赖自家服务端**

`feature.playlistshare.SharePlaylistImportHostKt`：`PlaylistShareDeepLinkHost` 处理 `guazi.fun/s/…` 深链 → 服务端返回 `SharePlaylistView` 快照（`SharePlaylistTrack` 列表）→ `ShareContentPreviewDialog` 预览 → `track: SharePlaylistTrack → SearchSong` 后进入导入。剪贴板自动识别同一套逻辑，可在设置里关闭。

**C. 汽水歌单转换 —— 同样依赖服务端**

`SodaConvertUiKt` / `SodaConvertDetailUiKt` + `AccountImportUiKt`：逐曲匹配到网易云 / QQ / 酷狗 / 酷我，匹配不到的保留汽水原曲，可整单恢复。

### 5.3 在线音源运行时（`core.lx`，229 个类）

关键类型：`LxPluginManager`、`LxJsRuntime`、`LxNativeHost`、`LxHttpBridge`、`LxScriptRunner`、`RemoteFirstScriptRunner`、`DataPluginManager`、`DataPluginVerifier`、`LxScriptGate`、`ScriptAccessGate`、`LxPluginPriority`、`LxPluginSortMode`、`LxTestReport`、`PluginLogStore`。

指示的设计取向：

- 脚本跑在 QuickJS（`libquickjs.so`），宿主通过 `LxHttpBridge` 代理网络、`LxNativeHost` 暴露原生方法
- **脚本准入有闸门**：`LxScriptGate` / `ScriptAccessGate` / `DataPluginVerifier`，配合导入时要求用户手输「我同意」的确认流程
- 插件按更新时间/优先级排序，支持多插件同平台、自动屏蔽失效平台（`LxKeyIssues`）
- 插件可同步到云端（`云端插件` 面板），属赞助权益

## 6. 服务端接口（DEX 内嵌）

| 域名/路径 | 用途 |
|---|---|
| `api.guazi.fun/lyrics-admin/gzm7/private/music/{search,lyric,amll/search,amll/lyric}` | 在线歌词与歌词搜索 |
| `api.guazi.fun` | 首页数据、榜单、歌单、搜索额度 |
| `apple.guazi.fun` | 动态封面（Apple Music）匹配 |
| `spotify.guazi.fun` | Spotify 侧数据 |
| `guazi.fun/listen?room=`、`guazi.fun/s/` | 一起听房间、歌单分享深链 |
| `guazi.fun/api/m7/health` | 数据服务健康检查 |
| `guazi.fun/android-upgrade.html`、`/sponsor.html` | 应用内更新、赞助页 |

官方数据服务**不提供取链与歌词**——这两件事分别由用户脚本和歌词服务承担，是有意的责任切分。

## 7. 与 Sonic 的能力差

| 能力 | Sonic | GZ | 备注 |
|---|---|---|---|
| 本地库 / 扫描 | ✅ | ✅ | GZ 额外要全盘存储权限 |
| 收藏 / 歌单 / 统计 / 均衡器 / 睡眠定时 / 倍速 / ReplayGain | ✅ | ✅ | 已对齐 |
| 在线取链 | ✅ LX 脚本 | ✅ LX 脚本 | Sonic 无插件准入闸门与云端同步 |
| 在线曲目持久化 | ✅（2026-10-21 起） | ✅ | Sonic 用 `songs` 表 + 可空平台键 |
| 缓存管理面板 | ✅（基础） | ✅ | GZ 多：容量上限、自动清理、二次确认 |
| 洛雪备份导入 | ❌ | ✅ | 需 `LxPlaylistParser` 对位实现 |
| 分享链接导入 / 汽水转换 | ❌ | ✅ | 依赖 GZ 自家服务端，不可移植 |
| 桌面歌词 / 锁屏歌词 | ❌ | ✅ | 最大单点缺口 |
| 平板自适应 | ❌ | ✅ | |
| 远程库（WebDAV / Navidrome） | ❌ | ✅ | 纯客户端协议，可移植 |
| 听歌识曲 / 一起听 / 下载 | ❌ | ✅ | 均需服务端或新权限 |
| 动态封面 | ❌ | ✅ | 依赖 `apple.guazi.fun` |

## 8. 未解与未知

- 缓存类型枚举 `sn0` 的三个取值及各自默认上限（类名不可按名定位）
- `AutoCleanCard` 的开关是存本地还是随账号同步
- 「一起听」的传输层（WebSocket / 自建长连）未验证
- 插件「云端同步」的协议与鉴权（`__LX_PRO_TOKEN__` 模板参数暗示服务端验签）
- 动态封面与 Apple Music 的匹配算法
- 全部运行时行为、签名真实性

## 附录：证据索引

| Evidence ID | 内容 |
|---|---|
| `ev_19afad8f…75c376` | `inspect_android_package` 完整记录（清单、SDK、类/资源计数） |
| `ev_e405835e…a45b3b` | 上述记录的 summary 视图 |
| `ev_fa0cd2db…2eb8f2a` | `CacheManagerSheetKt` 类结构 |
| `ev_61de3b3f…2dc796fd` | `CacheLimitDialog` 方法体（含淘汰文案） |
| `ev_8ce7c3d3…b4b5c9aa` | `PlaylistImport` 类名检索 |
| `ev_e3e6226b…d1814e6` | `LxPlaylistParser` 类结构 |
| `ev_7b715df2…f0b670` | `LxPlaylistParser.parse` 方法体 |
| `ev_88511618…2c2b3221` | `LxPlaylistImporter` 类结构 |
| `ev_772511e4…737acfbf` | `LxPlaylistImporter.importSongs` 方法体（超长，已落盘） |
| `ev_53b79f82…0e077206` | `LxPlaylistImportResult` 字段确认 |
| `ev_a0d332a7…74ecbf1b` | `SharePlaylistImportHostKt` 类结构 |
