# AI 音乐洞察

Sonic 用 **DeepSeek** 给专辑和歌手生成介绍文字。

## 能做什么

| 功能 | 入口 |
| --- | --- |
| 专辑介绍 | 专辑详情页 → AI 生成 |
| 歌手背景 | 歌手详情页 → AI 生成 |
| 富文本渲染 | 生成结果按 Markdown 渲染，标题 / 列表 / 粗体都正常显示 |

## 配置 API Key

两种方式，任选其一：

**方式一：构建时注入**（推荐给开发者）

```properties
# 仓库根的 local.properties
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
```

**方式二：App 内填写**

设置 → AI 设置 → 填入 Key。存在 DataStore（`ApiKeyStore`）里。

!!! warning "Key 只留在本机"
    Key 存进应用的 DataStore，不会同步到任何服务器。
    所有 AI 请求由设备直接发往 DeepSeek 官方接口。

## 缓存策略

`AiInfoCacheStore` 给 AI 结果做了缓存：

- 同一个专辑 / 歌手不会反复请求
- 缓存命中直接返回，不产生 API 账单
- 缓存有上限，由 `CacheJanitor` 定期清理（超上限按占用从大到小**整目录**清空）

## 离线可用

因为有缓存，AI 介绍在离线状态下依然能看 —— 只要之前生成过一次。
