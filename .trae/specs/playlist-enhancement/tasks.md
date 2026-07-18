# Tasks

- [x] Task 1: 创建播放列表选择弹窗组件
  - [x] SubTask 1.1: 创建 PlaylistPickerDialog.kt 组件
  - [x] SubTask 1.2: 支持显示播放列表列表
  - [x] SubTask 1.3: 支持新建播放列表功能

- [x] Task 2: 歌曲列表添加"添加到播放列表"功能
  - [x] SubTask 2.1: 修改 SongListItem 组件添加更多按钮
  - [x] SubTask 2.2: 更新 SongsContract.kt 添加相关 Intent 和 Effect
  - [x] SubTask 2.3: 更新 SongsViewModel.kt 处理添加逻辑
  - [x] SubTask 2.4: 更新 SongsScreen.kt 集成弹窗

- [x] Task 3: 播放列表重命名功能
  - [x] SubTask 3.1: 更新 PlaylistsContract.kt 添加重命名 Intent
  - [x] SubTask 3.2: 更新 PlaylistsViewModel.kt 处理重命名逻辑
  - [x] SubTask 3.3: 更新 PlaylistsScreen.kt 添加重命名 UI

- [x] Task 4: 播放列表详情页移除歌曲功能
  - [x] SubTask 4.1: 更新 PlaylistDetailScreen.kt 添加歌曲操作菜单
  - [x] SubTask 4.2: 确保移除功能正常工作（ViewModel 已实现）

# Task Dependencies
- [Task 2] 依赖 [Task 1]（需要弹窗组件）
- [Task 3] 和 [Task 4] 可独立执行
- [Task 1] 可独立执行
