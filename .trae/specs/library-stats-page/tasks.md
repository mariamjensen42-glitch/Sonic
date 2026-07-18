# Tasks

- [x] Task 1: 扩展数据模型添加bitrate字段
  - [x] SubTask 1.1: 在Song.kt (domain)添加bitrate字段
  - [x] SubTask 1.2: 在SongEntity.kt (data)添加bitrate字段
  - [x] SubTask 1.3: 更新SongEntityMapper映射逻辑
  - [x] SubTask 1.4: 更新数据库版本和迁移策略

- [x] Task 2: 更新音乐扫描器提取bitrate
  - [x] SubTask 2.1: 在MusicScanner中使用MediaMetadataRetriever提取bitrate
  - [x] SubTask 2.2: 处理bitrate提取失败的边界情况

- [x] Task 3: 创建音乐库统计功能模块
  - [x] SubTask 3.1: 创建LibraryStatsContract.kt定义UiState/Intent/Effect
  - [x] SubTask 3.2: 创建GetLibraryStatsUseCase获取统计数据
  - [x] SubTask 3.3: 创建LibraryStatsViewModel处理业务逻辑
  - [x] SubTask 3.4: 在SongRepository添加统计查询方法

- [x] Task 4: 创建UI组件
  - [x] SubTask 4.1: 创建PieChart组件（Canvas绘制饼图）
  - [x] SubTask 4.2: 创建StatItem组件（统计明细项）
  - [x] SubTask 4.3: 创建LibraryStatsScreen页面

- [x] Task 5: 集成导航和入口
  - [x] SubTask 5.1: 在Screen.kt添加LibraryStats路由
  - [x] SubTask 5.2: 在AppNavGraph.kt注册路由
  - [x] SubTask 5.3: 在SettingsScreen添加"音乐库统计"入口

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 3]
- [Task 5] depends on [Task 4]
