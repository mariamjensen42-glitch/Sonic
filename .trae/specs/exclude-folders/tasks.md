# Tasks

- [x] Task 1: 创建排除文件夹数据模型和仓库接口
  - [x] SubTask 1.1: 在 domain/model 创建 ExcludedFolder.kt 数据模型
  - [x] SubTask 1.2: 在 domain/repository 创建 ExcludedFolderRepository.kt 接口

- [x] Task 2: 实现排除文件夹仓库
  - [x] SubTask 2.1: 在 data/repository 创建 ExcludedFolderRepositoryImpl.kt，使用 DataStore 存储排除文件夹列表

- [x] Task 3: 修改扫描逻辑支持排除文件夹
  - [x] SubTask 3.1: 修改 MediaStoreHelper.queryAllSongs() 方法，接收排除路径参数并过滤
  - [x] SubTask 3.2: 修改 MusicScanner.scanMusic() 方法，加载并传递排除路径

- [x] Task 4: 创建排除文件夹管理页面
  - [x] SubTask 4.1: 创建 ExcludeFoldersContract.kt (UiState, Intent, Effect)
  - [x] SubTask 4.2: 创建 ExcludeFoldersViewModel.kt
  - [x] SubTask 4.3: 创建 ExcludeFoldersScreen.kt (iOS风格列表)

- [x] Task 5: 集成到设置页面和导航
  - [x] SubTask 5.1: 在 SettingsScreen 添加"排除文件夹"入口
  - [x] SubTask 5.2: 在 AppNavGraph 添加 ExcludeFolders 路由
  - [x] SubTask 5.3: 在 Screen.kt 添加 ExcludeFolders 目的地

# Task Dependencies
- [Task 3] depends on [Task 1, Task 2]
- [Task 4] depends on [Task 1]
- [Task 5] depends on [Task 4]
