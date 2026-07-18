# Unit Testing Checklist

## 测试依赖配置
- [x] domain模块已添加mockk和kotlinx-coroutines-test依赖
- [x] data模块已添加mockk、kotlinx-coroutines-test和turbine依赖
- [x] presentation模块已添加mockk、kotlinx-coroutines-test和turbine依赖

## Domain层测试
- [x] LrcParserTest - 标准LRC歌词解析测试通过
- [x] LrcParserTest - 带翻译歌词解析测试通过
- [x] LrcParserTest - 边界情况测试通过
- [x] TimeUtilsTest - 时间解析测试通过
- [x] TimeUtilsTest - 时间格式化测试通过
- [x] GetLyricsUseCaseTest - 测试通过
- [x] GetAllSongsUseCaseTest - 测试通过
- [x] SearchSongsUseCaseTest - 测试通过
- [x] ToggleFavoriteUseCaseTest - 测试通过

## Data层测试
- [x] SongRepositoryImplTest - getAllSongs测试通过
- [x] SongRepositoryImplTest - getSongById测试通过
- [x] SongRepositoryImplTest - toggleFavorite测试通过
- [x] SongRepositoryImplTest - searchSongs测试通过
- [x] SearchHistoryRepositoryImplTest - 测试通过

## Presentation层测试
- [x] SongsViewModelTest - 初始化加载歌曲测试通过
- [x] SongsViewModelTest - 切换视图模式测试通过
- [x] SongsViewModelTest - 排序功能测试通过
- [x] SongsViewModelTest - 添加到播放列表测试通过
- [x] SearchViewModelTest - 搜索功能测试通过

## 测试基础设施
- [x] TestDispatcherRule已创建并可正常使用
- [x] 测试数据工厂类已创建
