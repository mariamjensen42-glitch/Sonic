# Tasks

- [x] Task 1: 扩展数据模型和数据库
  - [x] SubTask 1.1: 在 SongEntity 中添加 isFavorite, playCount, lastPlayedAt 字段
  - [x] SubTask 1.2: 在 Song domain model 中添加对应字段
  - [x] SubTask 1.3: 升级 AppDatabase 版本并添加迁移策略
  - [x] SubTask 1.4: 在 SongDao 中添加收藏和统计相关查询方法

- [x] Task 2: 实现收藏功能仓库层
  - [x] SubTask 2.1: 在 SongRepository 接口添加收藏相关方法
  - [x] SubTask 2.2: 在 SongRepositoryImpl 实现收藏逻辑
  - [x] SubTask 2.3: 创建 ToggleFavoriteUseCase
  - [x] SubTask 2.4: 创建 GetFavoriteSongsUseCase

- [x] Task 3: 实现播放统计和历史记录仓库层
  - [x] SubTask 3.1: 在 SongRepository 接口添加统计和历史相关方法
  - [x] SubTask 3.2: 在 SongRepositoryImpl 实现统计逻辑
  - [x] SubTask 3.3: 创建 GetMostPlayedSongsUseCase
  - [x] SubTask 3.4: 创建 GetRecentlyPlayedSongsUseCase
  - [x] SubTask 3.5: 创建 UpdatePlayStatsUseCase

- [x] Task 4: 集成播放统计到 PlayerManager
  - [x] SubTask 4.1: 在 PlayerManager 中监听播放进度
  - [x] SubTask 4.2: 当播放进度超过 50% 时更新播放次数
  - [x] SubTask 4.3: 记录最后播放时间

- [x] Task 5: 播放器界面添加收藏功能
  - [x] SubTask 5.1: 在 PlayerContract 添加收藏相关 Intent 和 State
  - [x] SubTask 5.2: 在 PlayerViewModel 处理收藏逻辑
  - [x] SubTask 5.3: 在 PlayerScreen 添加收藏按钮 UI
  - [x] SubTask 5.4: 实现收藏状态切换和视觉反馈

- [x] Task 6: 首页添加收藏和播放入口
  - [x] SubTask 6.1: 在 HomeContract 添加收藏歌曲和最近播放状态
  - [x] SubTask 6.2: 在 HomeViewModel 加载收藏歌曲和最近播放数据
  - [x] SubTask 6.3: 在 HomeScreen 添加"喜欢的歌曲"快速访问入口
  - [x] SubTask 6.4: 在 HomeScreen 添加"最近播放"快速访问入口
  - [x] SubTask 6.5: 在 HomeScreen 添加"最常播放"快速访问入口

- [x] Task 7: 创建收藏歌曲页面
  - [x] SubTask 7.1: 创建 FavoritesContract 定义 UI 状态和 Intent
  - [x] SubTask 7.2: 创建 FavoritesViewModel 处理业务逻辑
  - [x] SubTask 7.3: 创建 FavoritesScreen 显示收藏歌曲列表
  - [x] SubTask 7.4: 添加导航路由

- [x] Task 8: 创建最近播放页面
  - [x] SubTask 8.1: 创建 RecentlyPlayedContract 定义 UI 状态和 Intent
  - [x] SubTask 8.2: 创建 RecentlyPlayedViewModel 处理业务逻辑
  - [x] SubTask 8.3: 创建 RecentlyPlayedScreen 显示播放历史
  - [x] SubTask 8.4: 添加导航路由

- [x] Task 9: 创建最常播放页面
  - [x] SubTask 9.1: 创建 MostPlayedContract 定义 UI 状态和 Intent
  - [x] SubTask 9.2: 创建 MostPlayedViewModel 处理业务逻辑
  - [x] SubTask 9.3: 创建 MostPlayedScreen 显示最常播放歌曲
  - [x] SubTask 9.4: 添加导航路由

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 3]
- [Task 5] depends on [Task 2]
- [Task 6] depends on [Task 2, Task 3]
- [Task 7] depends on [Task 2, Task 6]
- [Task 8] depends on [Task 3, Task 6]
- [Task 9] depends on [Task 3, Task 6]
