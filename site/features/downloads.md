# 下载与离线

## 下载任务

下载入口在歌曲菜单里（全局 `DownloadRequestProvider`），音质弹层挂在导航壳根部，
不随页面销毁 —— 在哪一页点的下载，弹层都在同一位置。

## 传输

`data/download/DownloadTransfer` 用 OkHttp 做流式下载：

- **Range 断点续传** —— 中断后接着下
- 进度实时回调，通知栏显示进度
- `dataSync` 类型前台服务 `DownloadService` 只负责保活与进度通知，**不持有状态**

真正的任务状态机在 `repository/DownloadRepositoryImpl` 里。

## 落盘顺序

这一步的顺序不能改，否则会出各种玄学问题：

```text
task-N.part
   ↓  改成真实容器后缀
.flac / .mp3 ...
   ↓  updateAudioTags + writeCoverArtwork
标签写好 + 封面内嵌
   ↓  MediaStoreWriter（IS_PENDING 流程）
扫描入库
```

两个关键点：

1. **先补后缀再写标签**。标签 writer 按扩展名挑实现，`.part` 会被判成「不支持」直接跳过，
   写进去的标签是空的。
2. **封面必须内嵌**。MediaStore 的专辑封面是扫描时从文件里生成的，
   外部单独存一张封面图没有用。

## 离线播放

下载完成后这首歌就属于本地曲库，扫描时会被正常识别，可以离线播。
