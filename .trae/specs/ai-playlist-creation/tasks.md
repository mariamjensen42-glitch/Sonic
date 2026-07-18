# Tasks

- [x] Task 1: 扩展 AiRepository 接口，添加主题歌单创建方法
  - [x] SubTask 1.1: 在 AiRepository 接口添加 `generatePlaylistByTheme` 方法
  - [x] SubTask 1.2: 在 AiRepositoryImpl 实现主题歌单生成逻辑
  - [x] SubTask 1.3: 构建 AI prompt，让 AI 分析歌曲与主题的匹配度

- [x] Task 2: 扩展 PlaylistRepository，添加批量添加歌曲方法
  - [x] SubTask 2.1: 在 PlaylistRepository 接口添加 `addSongsToPlaylist` 方法
  - [x] SubTask 2.2: 在 PlaylistRepositoryImpl 实现批量添加逻辑
  - [x] SubTask 2.3: 在 PlaylistDao 添加批量插入方法

- [x] Task 3: 创建 AI 歌单创建对话框 UI 组件
  - [x] SubTask 3.1: 创建 AiPlaylistCreationDialog 组件
  - [x] SubTask 3.2: 实现模式选择（随机/主题）
  - [x] SubTask 3.3: 实现歌曲数量选择器
  - [x] SubTask 3.4: 实现主题输入框
  - [x] SubTask 3.5: 实现歌单预览界面

- [x] Task 4: 扩展 PlaylistsViewModel 处理 AI 创建逻辑
  - [x] SubTask 4.1: 添加 AI 创建相关的 UiState 字段
  - [x] SubTask 4.2: 添加 AI 创建相关的 Intent
  - [x] SubTask 4.3: 实现随机创建歌单逻辑
  - [x] SubTask 4.4: 实现主题创建歌单逻辑
  - [x] SubTask 4.5: 实现歌单预览和确认逻辑

- [x] Task 5: 集成 AI 创建入口到播放列表页面
  - [x] SubTask 5.1: 在 PlaylistsScreen 添加 AI 创建按钮
  - [x] SubTask 5.2: 连接对话框与 ViewModel
  - [x] SubTask 5.3: 处理 API Key 未配置情况

# Task Dependencies
- [Task 3] depends on [Task 4]
- [Task 5] depends on [Task 3]
- [Task 4] depends on [Task 1, Task 2]
