# 在线音乐（洛雪式脚本取链）

从 **GZ_Music 2.1.8**（`com.gzmusic.app`，APK SHA-256 `66e83c471a14ca4c2437d6234a72ff1cc09ff03c1315506ae3c8b3ce448a3202`）
复刻的**洛雪（LX）音源插件**子系统：内嵌 QuickJS 执行用户导入的 LX 脚本取链。

原应用还有一套官方数据服务（`api.guazi.fun` 上的首页/搜索/歌单/榜单/远程取链）。
**本工程不移植那部分**——没有账号体系，也没有服务端，只保留「脚本插件 + 本地取链」这条能独立工作的链路。

> 反向工程证据全部来自 REA（JADX 静态分析，APK 不执行）。用户提到的 `ev_381ab8a5…` /
> `ev_8d359239…` 属于上个会话的 retained evidence，本会话已不在 bundle 里；同样的结论从同一个
> APK 重新取证，新的 Evidence id 见文末。

## 分层

```
domain/online/
  model/      OnlineSource、MusicQuality、MusicInfo、PlaySource、OnlinePlugin（+插件/音源/
              lxkey 错误/导入结果/探测报告/取链结果）、MusicInfoPlayback
  repository/ OnlinePluginRepository（插件安装、排序、屏蔽、取链）
              OnlineSearchRepository（在线目录搜索：关键词 → 带平台 id 的 MusicInfo）
  usecase/    ResolveOnlinePlaySourceUseCase（按优先级遍历候选平台取链）
              SearchOnlineMusicUseCase（只搜已启用插件声明支持的平台）

data/online/
  lx/         LxJsRuntime（QuickJS）· LxHttpBridge（OkHttp + Cookie）· LxBootstrap（注入 JS）
              LxScriptRunner（启动 / 请求 / 隔离）· LxPluginManager（装 / 排序 / 屏蔽 / 取链）
              LxScriptMeta（脚本头解析与导入闸门）· LxJsUtils（crypto/zlib/buffer/text）
              LxJson · LxKeyIssues
  search/     OnlineSearchRepositoryImpl（各平台并行，单平台失败只丢该平台）
  store/      OnlineStore（DataStore + 脚本文件）

data/api/search/
  OnlineCatalogSearchService（网易云 / QQ / 酷狗公开搜索接口 → MusicInfo）

presentation/onlinesources/
  OnlineSourcesContract / OnlineSourcesViewModel / OnlineSourcesScreen
  （设置 → 在线音乐 → 在线音源）
presentation/search/
  SearchScreen 的「在线音乐」区块：搜到 → 取链 → 播放
```

> **为什么要自己写搜索层**：LX 自定义音源脚本只实现 `musicUrl`（给定平台歌曲 id → 播放地址），
> **不实现 search**。宿主不自己搜就没有 `songmid` / `hash` 交给脚本，音源装得再好也播不了。

## 脚本运行时（逐项对齐原实现）

| 项 | 值 | 来源 |
| --- | --- | --- |
| JS 引擎 | `com.dokar.quickjs`（`QuickJs.create(dispatcher)`） | `LxJsRuntime` |
| JS 内存上限 | **64 MiB** | `MAX_JS_MEMORY_BYTES = 67108864` |
| JS 栈上限 | **1 MiB** | `MAX_JS_STACK_BYTES = 1048576` |
| 初始化超时 | 60 s | `LxScriptRunner.INIT_TIMEOUT_MS` |
| 单次请求超时 | 15 s | `request(..., timeout=15000)` |
| 失控隔离 | 连续 **3** 次超时 → 隔离 **60 s** → 自动重建 | `QUARANTINE_*` |
| 脚本默认环境 | `lx.env = 'desktop'`、`lx.version = '2.11.0'` | `LxScriptRunner` |
| 脚本包装 | `(async function(){ … })();` | `LxBootstrap.wrapUserScript` |
| 响应体上限 | **8 MiB** | `LxHttpBridge.MAX_BODY_BYTES` |
| 导入上限 | **4 MiB** | `LxScriptMeta.MAX_SCRIPT_BYTES` |

宿主 ↔ 脚本通道（与 `LxJsRuntime.handleHost` 一致）：

- `__lxConsole` → 日志；`__lxEvent` → `inited` / `updateAlert`；`__lxReturn` → 请求结果；`__lxUtils` → `lx.utils.*`
- 绑定函数：`lxHttpRequest(url, options)`（async）、`__lxSleep(ms)`（async）、`lxHostInvoke(channel, payload)`（sync）

`LxBootstrap` 的 JS 文本是从 DEX 字符串里原样取出的 8454 字符（`LxBootstrap.JS`），
只改了一处：`LxNativeHost.invoke(...)` → `lxHostInvoke(...)`（少一层 ObjectBinding 反射绑定），
`lx` 全局 API、console、`setTimeout/setInterval/queueMicrotask`、`atob/btoa` 都保持不变。

### 脚本头（`LxScriptMeta`）

```js
/**
 * @name 示例音源
 * @description 支持 wy/tx
 * @version 1.0.0
 * @author someone
 * @homepage https://example.com
 * @x-pro-only true      // 赞助专属（由外部约定解释，本工程不做授权体系）
 * @lxkey =              // 配置项声明（只有 name，没有值）
 */
```

导入闸门：空 → `EMPTY`；> 4 MiB → `TOO_LARGE`；前 200 字符以 `<` 开头 → `NOT_JS`；
不含 LX 标记（`window.lx` / `globalThis.lx` / `lx.on|send|request`）→ `NO_LX_MARKER`。
`NO_LX_MARKER` 的脚本（压缩/混淆）仍允许导入，标记为 `opaque` 并在设置页提示。

### 平台与音质

- LX 平台键：`wy` 网易云、`tx` QQ、`kg` 酷狗、`kw` 酷我、`mg` 咪咕、`sd` 汽水音乐
  （与 `LxModelsKt.LX_MUSIC_PLATFORM_KEYS` 相同）。
- 音质档位（高→低）：`master → atmos_plus → atmos → jymaster → sky → jyeffect → flac24bit
  → hires → flac → 320k → 128k`；`flac24bit/atmos/atmos_plus/master/jyeffect/sky/jymaster`
  只有插件可能提供（`MusicQuality.pluginOnly`）。
- **档位降级**：`MusicQuality.ladder(available, preferred, limit=4)`，命中不了就沿 rank 向下再试，
  最多 4 档；插件专属档位不参与降级。

## 取链流程（`LxPluginManager.fetchPlaySource`）

```
candidates = [歌曲自身平台, 其它候选平台...]（FALLBACK 时全部，PLUGIN_ONLY 只第一个）
for 每个候选平台:
    遍历启用插件（按排序模式、跳过手动/自动屏蔽平台、且脚本声明支持该平台 musicUrl）:
        对该插件声明的档位做降级尝试 → 成功返回
        lxkey 额度/到期/停用/无效 → 立即终止（换平台也没用）
全部失败 → Failure(message, keyIssue)
```

- **自动屏蔽**：`applyAutoBlockDetection(pluginId, probe)` —— 探测失败且在 LX 平台键集合内、
  又没被手动屏蔽的平台写进 `autoBlockedPlatforms`；探测恢复的自动移除。
- **取链后探测**：脚本只拼 URL、不访问它，后端挂了照样回「成功」。`PlayUrlProbe` 用
  `GET` + `Range: bytes=0-1` 探一次（2xx/206 才算可播），不可播就当作该插件失败、继续试下一个。
  没有这一步时，一个后端已 404 的音源会挡住后面所有可用音源（实测：长青全 404、幻音两个后端全挂，
  结果只剩星海的网易云能播）。
- **排序模式**：`MANUAL`（手动）/ `PLATFORM_COUNT` / `SPEED`（启动耗时）/ `AVAILABLE`（无错误优先）。
- **优先级**：`FALLBACK`（本平台失败后继续试候选平台）或 `PLUGIN_ONLY`（只用本平台）。

## 授权 / lxkey

脚本自身（或脚本背后的服务）把 lxkey 的错误以中文文案回传，`LxKeyIssues.classify(message)` 归类为
`QUOTA / EXPIRED / DISABLED / INVALID / NOT_ALLOWED / PLATFORM / QUALITY`；
`QUOTA/EXPIRED/DISABLED/INVALID` 视为致命错误，立即停止重试（换插件/换平台都没意义）。
本工程没有账号体系，所以「谁能用哪些平台」完全由脚本及其 lxkey 决定，宿主只负责把错误翻成可读提示。

## 播放

`MusicInfo.toSong(playSource)` 把在线歌曲转成 `Song`（`path = url`，合成负数 id），
可直接交给既有 `PlayerRepository.playSong(song, queue)` —— ExoPlayer 的 `MediaItem.uri`
本身就是 `QueueItem.uri`。

> **明文流量必须放行**：脚本返回的播放地址大量是 `http://`（网易云 `m704.music.126.net`、
> 长青 `yinyue.haitangw.net` …），`targetSdk 36` 默认禁止，ExoPlayer 会直接
> `CleartextNotPermittedException`，表现为「搜到歌但一首也播不了」。
> 见 `app/src/main/res/xml/network_security_config.xml`（域名随脚本而变，无法枚举，只能整体放行）。

已知边界：取链返回的请求头（Referer/Cookie）目前没有进 `QueueItem`，
需要带头的源要后续给 `QueueItem` 加 headers；在线歌曲不在 Room 里，播放次数/收藏不会有副作用。

入口在搜索页（`SearchScreen` 的「在线音乐」区块）：

```
关键词 → SearchOnlineMusicUseCase（只搜已启用插件声明支持的平台）
       → OnlineCatalogSearchService（网易云 / QQ / 酷狗 / 酷我 / 咪咕 并行，每页每平台 10 条）
       → 用户点一条 → ResolveOnlinePlaySourceUseCase（固定 320k，插件自行降级）
       → MusicInfo.toSong(playSource) → PlayerRepository.playSong
```

- 翻页：底部「加载更多」请求下一页并**追加**（`page` 参数在 `OnlineCatalogSearchService` 里换算成
  各平台的分页参数：网易云 `offset`、QQ `p`、酷狗 `page`、酷我 `pn`（0 起）、咪咕 `pageNo`），
  追加时按 `平台:id` 去重。

- 五个平台都能搜：网易云 / QQ / 酷狗 / 酷我 / 咪咕；只有汽水（SODA）没有公开搜索接口
  （见 `OnlineSearchRepositoryImpl.SEARCHABLE_SOURCES`）。
- 各平台的 id 口径不同，取链必须原样交给脚本：网易云 `song_id`、QQ `songmid`、酷狗文件 `FileHash`、
  酷我 `rid`（`MUSIC_` 后的数字）、咪咕 `contentId` + `copyrightId`。
- 封面：网易云取 `al.picUrl`（cloudsearch）、QQ 用 `albummid`、酷狗用 `Image` 模板、咪咕用 `imgItems`、
  酷我用 `web_albumpic_short` —— **酷我这个字段只有约一半条目有值**，所以仓库层还会按
  「歌名 + 首位歌手」从其它平台的同款结果借封面（`borrowMissingCovers`），不发额外请求。
- 酷狗的 `song_search_v2` **不能带 `tagtype=0`**：带了仍然回 `status=1`，但 `lists` 恒为空
  （2026-10-21 实测）。酷我的 `r.s` 标着 `rformat=json` 却回单引号伪 JSON，要先归一化引号。

## 依赖

`io.github.dokar3:quickjs-kt-android:1.0.15`（AAR 自带 `arm64-v8a` / `armeabi-v7a` / `x86` / `x86_64` 的 `libquickjs.so`）。
R8 规则已加 `-keep class com.dokar.quickjs.**`（JNI 按「类名 + 方法名」解析符号，不能被重命名）。

## 本轮范围边界

| 未做 | 原因 / 后续 |
| --- | --- |
| 官方数据服务（首页/搜索/歌单/榜单/远程取链） | 明确不移植：没有账号与服务端，只保留本地脚本链路。 |
| 歌单导入解析（苹果音乐 / Spotify / 百度网盘） | 原实现是独立链路（`LxPlaylistParser` + 服务端 parse），本轮不做。 |
| 在线歌曲进播放队列时的请求头 | `QueueItem` 目前只有 `uri`；需要给 `QueueItem` 加 `headers` 并配 `ResolvingDataSource`。 |
| `lx.utils.qrc.*`（QQ 逐字歌词） | QRC 解密器未移植，调用返回 `__error`；只影响用到 QRC 歌词的脚本。 |
| 在线歌曲的「完整浏览」UI | 已有搜索页的在线区块（搜 → 点 → 播）；首页推荐 / 榜单 / 歌单导入仍未做。 |
| 酷我 / 咪咕 / 汽水的搜索 | 酷我、咪咕已接（`search.kuwo.cn/r.s`、`app.c.nf.migu.cn`）；汽水没有公开接口。 |

## 构建验证（未由 Agent 执行）

按仓库规则，本轮没有运行 Gradle。用户侧建议依次：

```bash
./gradlew :data:assembleDebug        # 先确认新依赖与 Kotlin 编译
./gradlew :presentation:assembleDebug
./gradlew :app:assembleDebug
```

运行路径：设置 → 在线音乐 → 在线音源 → 导入 LX 脚本 → 列表里「测试」看 `inited` 声明。
单元测试（纯逻辑，无需设备）：`:domain:testDebugUnitTest --tests "*MusicQualityTest"`、
`:data:testDebugUnitTest --tests "*LxScriptMetaTest"`。

## 类 ↔ Evidence（REA / JADX）

| 复刻产物 | 原类 | Evidence |
| --- | --- | --- |
| `LxHttpBridge` | `com.gzmusic.app.core.lx.LxHttpBridge` | `ev_4ffa326d72917ad68de80e191705d8a865ba8aba957366f0767df09a485d7348`（类）、`ev_eba31e8a977da9ce98a17102ab0a2a4c1c4d3f8a4c31fdc4df4734ac3620b58b`（request） |
| `LxJsRuntime` | `…core.lx.LxJsRuntime` | `ev_5c9e0197b84c14abdb1df95b53c34ae6a50922f87f347ba7742d68839acf616e`（类）、`ev_bd253442291b057895df4fe19c28df908895e87da80a45b40d747e652472e041`（init） |
| `LxBootstrap` | `…core.lx.LxBootstrap` | `ev_d731f9fcc5781d55c5a99eb8cb202fb41b2a80beb718c0707590c5f6bf6d8151` |
| `LxNativeHost` | `…core.lx.LxNativeHost` | `ev_e587b7f472eb2b6f80bc86ec4c79dd958381dab4e717754ddca9ecff89dea196`、`ev_9daf199b8b276280f1bb8ffad02481140c92e4ce409ebc2836f2d245a2c1bcb0` |
| `LxScriptRunner` | `…core.lx.LxScriptRunner` | `ev_270c73227538d0429b2b159a4594fb40747fec2753fad0994c6287f73ea6a2fd` |
| `OnlinePlugin` | `…core.lx.LxPlugin` | `ev_056b08bb998b98da85cafb6290d4f4855236bd0b5a8243ccc0aabdd3fca0f34f` |
| `MusicQuality` / `MusicInfo` | `LxMaps` / `LxMusicInfoBuilder` | `ev_757a500c76dc20c80f936f0cf23b0394104dfebaeb0bc5db8c08e0ae2ffe5576`（`gzmusic.app.core` 类清单） |
| `LxScriptMeta` | `…core.lx.LxScriptMeta` | 同上类清单 + `ev_08d1632ef466a264781e4f6fae853eac1f8356f6214e9a8cc2fcb0507c5d0ad5` |
| QuickJS 绑定 | `com.dokar.quickjs.*` | `ev_c9984b28be9db625f997967e2a6c24b12790215fc20be07d859fd7fac256f2fd` |
| 包身份（`com.gzmusic.app` 2.1.8 / code 38） | — | `ev_d772386f39ad9478f831f77dcaa6f534d58b773bbe96c89e456843357c67c13b` |
