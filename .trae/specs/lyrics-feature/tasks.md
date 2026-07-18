# Tasks

- [x] Task 1: 创建歌词领域模型和仓库接口
  - [x] SubTask 1.1: 在 domain/model 创建 Lyrics.kt 数据类
  - [x] SubTask 1.2: 在 domain/repository 创建 LyricsRepository.kt 接口

- [x] Task 2: 实现歌词获取服务
  - [x] SubTask 2.1: 添加嵌入式歌词提取库依赖（jaudiotagger）
  - [x] SubTask 2.2: 创建 EmbeddedLyricsHelper.kt 用于提取嵌入式歌词
  - [x] SubTask 2.3: 创建 LyricsFileHelper.kt 用于查找和读取独立歌词文件
  - [x] SubTask 2.4: 创建 LyricsRepositoryImpl.kt 实现歌词仓库（支持优先级）

- [x] Task 3: 创建歌词用例
  - [x] SubTask 3.1: 创建 GetLyricsUseCase

- [x] Task 4: 创建歌词页面
  - [x] SubTask 4.1: 创建 LyricsScreen.kt 歌词页面
  - [x] SubTask 4.2: 创建 LyricsContract.kt（UiState、Intent）
  - [x] SubTask 4.3: 创建 LyricsViewModel.kt
  - [x] SubTask 4.4: 在导航中添加歌词页面路由

- [x] Task 5: 修改播放器添加歌词入口
  - [x] SubTask 5.1: 在 PlayerScreen 添加歌词按钮
  - [x] SubTask 5.2: 点击歌词按钮导航到歌词页面

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1, Task 2]
- [Task 4] depends on [Task 3]
- [Task 5] depends on [Task 4]
