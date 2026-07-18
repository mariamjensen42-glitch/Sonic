# Tasks

- [x] Task 1: 优化 LyricsViewModel 播放位置更新
  - [x] SubTask 1.1: 添加 isPlaying、duration、lastUpdateTime 字段
  - [x] SubTask 1.2: 从 PlayerRepository 获取完整播放状态

- [x] Task 2: 优化 LyricsScreen 位置传递方式
  - [x] SubTask 2.1: 使用 remember 创建 animatedPositionState
  - [x] SubTask 2.2: 使用 LaunchedEffect + awaitFrame 实现帧同步更新
  - [x] SubTask 2.3: 将 currentPositionProvider lambda 传递给 KaraokeLyricsView

# Task Dependencies
- [Task 2] depends on [Task 1]
