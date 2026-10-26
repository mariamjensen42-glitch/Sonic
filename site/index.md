# Sonic

**现代化 Android 音乐播放器 —— Material 3 Expressive 设计、AI 音乐洞察、卡拉 OK 歌词、在线音源。**

Sonic 是一个功能完整、纯 Kotlin + Jetpack Compose 编写的本地音乐播放器。
界面按 Apple Music 7.0.0-beta 做了像素级复刻，同时保留 Material 3 Expressive 的设计语言；
播放内核是 Media3 ExoPlayer，后台播放由 MediaSessionService 前台服务保活。

<div class="grid cards" markdown>

-   :material-music-note: &nbsp; **播放**

    ---

    Media3 ExoPlayer 播放内核、后台播放、通知栏控制、队列管理、
    四档播放模式、倍速、定时关闭、淡入淡出。

-   :material-text-box-outline: &nbsp; **歌词**

    ---

    LRC / 增强 LRC / TTML / 逐字歌词全格式解析，实时卡拉 OK 高亮，
    在线歌词多源匹配，译文开关。

-   :material-brain: &nbsp; **AI 洞察**

    ---

    DeepSeek 驱动的专辑介绍与歌手背景，Markdown 富文本渲染，带缓存。

-   :material-cloud: &nbsp; **在线音源**

    ---

    洛雪协议音源脚本引擎，内置三套音源，网易云 / QQ / 酷狗 / 酷我 / 咪咕在线搜索。

</div>

---

## 文档导航

| 章节 | 内容 |
| --- | --- |
| [快速开始](getting-started.md) | 下载 APK、安装、首次扫描、DeepSeek API Key 配置 |
| [播放与队列](features/playback.md) | 播放内核、队列、四档模式、播放设置面板 |
| [歌词](features/lyrics.md) | 歌词格式、卡拉 OK、在线取词、译文 |
| [音乐库](features/library.md) | 扫描、歌单、收藏、文件夹、统计 |
| [AI 音乐洞察](features/ai-insights.md) | 专辑 / 歌手 AI 介绍与缓存策略 |
| [在线音源](features/online-sources.md) | 洛雪脚本、插件管理、在线搜索、元数据刮削 |
| [下载与离线](features/downloads.md) | 下载任务、断点续传、离线播放 |
| [均衡器与统计](features/tools.md) | 均衡器、听歌统计、桌面小部件 |
| [架构](architecture.md) | Clean Architecture + MVI 的模块划分与数据流 |

## 环境要求

| 项 | 要求 |
| --- | --- |
| 最低系统 | Android 11（API 30） |
| 目标系统 | Android 16（API 36） |
| 架构 | arm64-v8a / armeabi-v7a / x86_64 |
| 权限 | 通知、后台播放、`READ_MEDIA_AUDIO` |

## 开源许可

MIT License，详见 [LICENSE](https://github.com/mariamjensen42-glitch/Sonic/blob/master/LICENSE)。
