# Tasks

- [x] Task 1: 实现"下一首播放"数据层功能
  - [x] SubTask 1.1: 在 PlayerRepository 接口添加 playNext(song: Song) 方法
  - [x] SubTask 1.2: 在 PlayerManager 添加 playNext 方法实现，将歌曲插入到 currentIndex + 1 位置
  - [x] SubTask 1.3: 在 PlayerRepositoryImpl 实现 playNext 方法

- [x] Task 2: 实现队列拖拽排序数据层功能
  - [x] SubTask 2.1: 在 PlayerRepository 接口添加 moveQueueItem(fromIndex: Int, toIndex: Int) 方法
  - [x] SubTask 2.2: 在 PlayerManager 添加 moveQueueItem 方法实现，处理索引更新逻辑
  - [x] SubTask 2.3: 在 PlayerRepositoryImpl 实现 moveQueueItem 方法

- [x] Task 3: 更新 Presentation 层 Intent 和 ViewModel
  - [x] SubTask 3.1: 在 PlayerContract 添加 PlayNext 和 MoveQueueItem Intent
  - [x] SubTask 3.2: 在 PlayerViewModel 添加 Intent 处理逻辑

- [x] Task 4: 实现播放队列拖拽排序 UI
  - [x] SubTask 4.1: 创建可拖拽的 QueueSongItem 组件，添加拖拽手柄图标
  - [x] SubTask 4.2: 实现拖拽状态管理和动画效果
  - [x] SubTask 4.3: 在 PlayerScreen 的队列列表中集成拖拽功能

- [x] Task 5: 添加"下一首播放"菜单入口
  - [x] SubTask 5.1: 在歌曲列表组件的更多菜单中添加"下一首播放"选项
  - [x] SubTask 5.2: 处理菜单点击，发送 PlayNext Intent
  - [x] SubTask 5.3: 添加 Toast 提示反馈

# Task Dependencies
- Task 2 依赖 Task 1（数据层方法需要先定义）
- Task 3 依赖 Task 1 和 Task 2（Intent 需要调用数据层方法）
- Task 4 依赖 Task 3（UI 需要调用 ViewModel）
- Task 5 依赖 Task 3（菜单需要发送 Intent）
- Task 4 和 Task 5 可并行
