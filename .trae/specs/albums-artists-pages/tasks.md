# Tasks

- [x] Task 1: 创建 AlbumsViewModel 和 Contract
  - [x] 创建 AlbumsContract.kt，定义 UiState 和 UiIntent
  - [x] 创建 AlbumsViewModel.kt，注入 AlbumRepository，获取专辑列表

- [x] Task 2: 创建 AlbumsScreen 页面
  - [x] 使用 Scaffold 和 IOSTopAppBar
  - [x] 使用 IOSInsetGrouped 和 IOSListItem 显示专辑列表
  - [x] 处理点击事件，导航到专辑详情页

- [x] Task 3: 创建 ArtistsViewModel 和 Contract
  - [x] 创建 ArtistsContract.kt，定义 UiState 和 UiIntent
  - [x] 创建 ArtistsViewModel.kt，注入 ArtistRepository，获取歌手列表

- [x] Task 4: 创建 ArtistsScreen 页面
  - [x] 使用 Scaffold 和 IOSTopAppBar
  - [x] 使用 IOSInsetGrouped 和 IOSListItem 显示歌手列表
  - [x] 处理点击事件，导航到歌手详情页

- [x] Task 5: 更新导航配置
  - [x] 在 AppNavGraph.kt 中替换占位符为真实的 AlbumsScreen 和 ArtistsScreen

# Task Dependencies
- Task 2 依赖 Task 1
- Task 4 依赖 Task 3
- Task 5 依赖 Task 2 和 Task 4
