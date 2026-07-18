# Tasks

- [x] Task 1: 创建 SongsContract.kt
  - [x] SubTask 1.1: 定义 SongsUiState（songs, isLoading, viewMode, sortOrder, sortAscending）
  - [x] SubTask 1.2: 定义 SongsIntent（LoadSongs, ToggleViewMode, ChangeSortOrder, SongClick）
  - [x] SubTask 1.3: 定义 SongsEffect（NavigateToPlayer）
  - [x] SubTask 1.4: 定义 ViewMode 枚举（LIST, GRID）
  - [x] SubTask 1.5: 定义 SortOrder 枚举（TITLE, ARTIST, ALBUM, DURATION, DATE_ADDED）

- [x] Task 2: 创建 SongsViewModel.kt
  - [x] SubTask 2.1: 注入 SongRepository
  - [x] SubTask 2.2: 加载歌曲列表
  - [x] SubTask 2.3: 实现排序逻辑
  - [x] SubTask 2.4: 实现视图模式切换
  - [x] SubTask 2.5: 保存用户偏好（视图模式、排序方式）到 DataStore

- [x] Task 3: 创建 SongsScreen.kt
  - [x] SubTask 3.1: 实现列表视图（使用 SongListItem 组件）
  - [x] SubTask 3.2: 实现网格视图
  - [x] SubTask 3.3: 实现视图切换按钮
  - [x] SubTask 3.4: 实现排序选择菜单
  - [x] SubTask 3.5: iOS 风格 UI

- [x] Task 4: 更新导航配置
  - [x] SubTask 4.1: 在 AppNavGraph.kt 替换 PlaceholderScreen 为 SongsScreen

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 2, Task 3]
