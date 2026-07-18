# Tasks

- [x] Task 1: 创建文件夹数据层
  - [x] SubTask 1.1: 创建 FolderItem 数据模型 (domain层)
  - [x] SubTask 1.2: 在 SongRepository 添加文件夹查询方法
  - [x] SubTask 1.3: 在 SongDao 添加按路径前缀查询方法
  - [x] SubTask 1.4: 在 SongRepositoryImpl 实现文件夹查询逻辑

- [x] Task 2: 创建文件夹浏览页面
  - [x] SubTask 2.1: 创建 FolderContract (UiState, Intent, Effect)
  - [x] SubTask 2.2: 创建 FolderViewModel
  - [x] SubTask 2.3: 创建 FolderScreen UI 组件
  - [x] SubTask 2.4: 添加文件夹导航入口到主页

- [x] Task 3: 集成导航和播放
  - [x] SubTask 3.1: 在 Screen.kt 添加 Folder 路由
  - [x] SubTask 3.2: 在 AppNavGraph 添加文件夹导航
  - [x] SubTask 3.3: 实现文件夹内歌曲播放功能

# Task Dependencies
- [Task 2] 依赖 [Task 1]
- [Task 3] 依赖 [Task 2]
