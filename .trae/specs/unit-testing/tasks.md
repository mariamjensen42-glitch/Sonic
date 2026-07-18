# Tasks

- [x] Task 1: 配置测试依赖
  - [x] SubTask 1.1: 在domain模块添加测试依赖（mockk、kotlinx-coroutines-test）
  - [x] SubTask 1.2: 在data模块添加测试依赖（mockk、kotlinx-coroutines-test、turbine）
  - [x] SubTask 1.3: 在presentation模块添加测试依赖（mockk、kotlinx-coroutines-test、turbine）

- [x] Task 2: Domain层单元测试 - 歌词解析器
  - [x] SubTask 2.1: 创建LrcParserTest - 测试标准LRC解析
  - [x] SubTask 2.2: 创建LrcParserTest - 测试带翻译的歌词解析
  - [x] SubTask 2.3: 创建LrcParserTest - 测试边界情况（空输入、无效格式）
  - [x] SubTask 2.4: 创建TimeUtilsTest - 测试时间解析和格式化

- [x] Task 3: Domain层单元测试 - Use Cases
  - [x] SubTask 3.1: 创建GetLyricsUseCaseTest
  - [x] SubTask 3.2: 创建GetAllSongsUseCaseTest
  - [x] SubTask 3.3: 创建SearchSongsUseCaseTest
  - [x] SubTask 3.4: 创建ToggleFavoriteUseCaseTest

- [x] Task 4: Data层单元测试 - Repository
  - [x] SubTask 4.1: 创建SongRepositoryImplTest - 测试获取歌曲列表
  - [x] SubTask 4.2: 创建SongRepositoryImplTest - 测试收藏功能
  - [x] SubTask 4.3: 创建SongRepositoryImplTest - 测试搜索功能
  - [x] SubTask 4.4: 创建SearchHistoryRepositoryImplTest

- [x] Task 5: Presentation层单元测试 - ViewModels
  - [x] SubTask 5.1: 创建SongsViewModelTest - 测试加载歌曲
  - [x] SubTask 5.2: 创建SongsViewModelTest - 测试切换视图模式
  - [x] SubTask 5.3: 创建SongsViewModelTest - 测试排序功能
  - [x] SubTask 5.4: 创建SongsViewModelTest - 测试添加到播放列表
  - [x] SubTask 5.5: 创建SearchViewModelTest - 测试搜索功能

- [x] Task 6: 创建测试工具类和Fixtures
  - [x] SubTask 6.1: 创建TestDispatcherRule用于协程测试
  - [x] SubTask 6.2: 创建测试数据工厂类（SongFixture、PlaylistFixture等）

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1]
- [Task 4] depends on [Task 1]
- [Task 5] depends on [Task 1]
- [Task 5] depends on [Task 6]
