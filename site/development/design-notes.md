# 设计笔记

Sonic 的设计决策有大量「为什么这么做」的记录，散落在仓库的 `docs/` 目录里。
这些是**内部设计文档**，不放进站点正文，但公开可读 —— 它们解释了实现里的每个取舍从哪来。

## Apple Music 复刻层

| 文档 | 内容 |
| --- | --- |
| [`docs/design/apple-music-port.md`](https://github.com/mariamjensen42-glitch/Sonic/blob/master/docs/design/apple-music-port.md) | Apple Music 7.0.0-beta 的像素级复刻：色板溯源表、字阶、尺寸、图标来源 |
| [`docs/design/apple-player-spec.md`](https://github.com/mariamjensen42-glitch/Sonic/blob/master/docs/design/apple-player-spec.md) | 播放页布局树 + 实测令牌 + 卡拉 OK 结构 + 与 Sonic 的差距表 |
| [`docs/design/apple-music-preview.html`](https://github.com/mariamjensen42-glitch/Sonic/blob/master/docs/design/apple-music-preview.html) | 静态预览页，浏览器直接打开看效果 |

## 元数据与刮削

[`docs/metadata-saltplayer.md`](https://github.com/mariamjensen42-glitch/Sonic/blob/master/docs/metadata-saltplayer.md)
记录了元数据刮削的字段清单、匹配规则与来源平台差异。

## 图标体系

全站图标只用 XML 矢量资源，来源是 Flamingo 1.0.0 应用自有的 **65 个**图标，
全部抽取后转成 Android vector drawable。

造型只认 Apple Music 那一套 —— Material Icons / Material Symbols 的图形一律不要，
即使功能对得上。

!!! tip "验证 vector 资源不能只看 XML 合不合法"
    合法 XML 里也能混进非路径字符，运行到 `painterResource` 才抛
    `IllegalArgumentException: Unknown command for: X`。
    正确做法是用路径解析器把每条 `android:pathData` 走一遍，
    并断言字符集只含 `MmLlHhVvCcSsQqTtAaZz0-9 .,+-eE`。

## 仓库里的其它文档

| 文件 | 用途 |
| --- | --- |
| [`AGENTS.md`](https://github.com/mariamjensen42-glitch/Sonic/blob/master/AGENTS.md) | 项目的技术栈、代码风格、结构与强制规则，**改代码前必读** |
| [`CLAUDE.md`](https://github.com/mariamjensen42-glitch/Sonic/blob/master/CLAUDE.md) | 与 AGENTS.md 同源的 AI 协作说明 |
| [`CONTEXT.md`](https://github.com/mariamjensen42-glitch/Sonic/blob/master/CONTEXT.md) | 术语表 |
| [`CHANGELOG.md`](https://github.com/mariamjensen42-glitch/Sonic/blob/master/CHANGELOG.md) | 更新日志 |
