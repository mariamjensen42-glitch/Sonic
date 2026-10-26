# 快速开始

## 安装

从 [Releases](https://github.com/mariamjensen42-glitch/Sonic/releases/latest) 下载最新的
`app-release.apk`，在系统里允许「安装未知来源应用」后直接安装。

!!! tip "Release 是签名的正式包"
    Release APK 用仓库里的 `sonic-release.keystore` 签名，签名不会变，
    后续版本可以直接覆盖安装，不会丢数据。

## 首次授权

Sonic 启动后会依次申请三类权限：

| 权限 | 用途 | 拒绝的后果 |
| --- | --- | --- |
| `READ_MEDIA_AUDIO` | 扫描本地音乐 | 无法建立音乐库 |
| `POST_NOTIFICATIONS` | 通知栏播放控制 | 前台服务通知不可见，播放仍可用 |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 后台播放保活 | 退到后台后播放会被系统回收 |

## 扫描音乐库

首次启动会自动扫描 MediaStore 中所有音频文件。扫描规则：

- 按 MIME 类型 + 文件后缀双重判定音频（支持 MP3 / FLAC / OGG / MP4 / WAV / AIFF / WMA / DSF / DFF / Opus / AAC）。
- **排除录音**默认开启：按路径目录名与标签识别通话录音、闹钟、播客等并跳过（设置 → 音乐库可关）。
- 扫描完成后可在「音乐库」里调整排除目录。

## 配置 DeepSeek API Key

AI 音乐洞察是可选功能，需要自备 API Key：

```properties
# local.properties（仓库根，不入库）
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
```

打包时通过 `local.properties` 注入。装好的 App 里也可以在
**设置 → AI 设置** 手动填入 Key并存到 DataStore。

!!! warning "Key 只存在本地"
    API Key 存在应用的 DataStore 里，不会同步到任何服务器；
    所有 AI 请求直接从设备发往 DeepSeek 官方接口。

## 从源码构建

```bash
# 需要 JDK 17、Android SDK 37
git clone https://github.com/mariamjensen42-glitch/Sonic.git
cd Sonic

# Debug 包并安装到设备
./gradlew installDebug

# Release 包
./gradlew :app:assembleRelease
```

签名配置在仓库根的 `key.properties` + `sonic-release.keystore`（两者都不入库）。

更多开发约定见仓库里的 [`AGENTS.md`](https://github.com/mariamjensen42-glitch/Sonic/blob/master/AGENTS.md)。

## 下一步

- 想知道每个功能怎么用 → 看[功能](features/playback.md)章节
- 想参与开发 → 看[参与贡献](contributing.md)
- 想改这个文档站 → 看[文档站点](development/docs-site.md)
