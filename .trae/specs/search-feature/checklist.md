# 搜索功能检查清单

## 数据层
- [ ] SearchHistoryEntity 实体类定义正确，包含必要字段（id、query、timestamp）
- [ ] SearchHistoryDao 包含插入、查询、删除方法
- [ ] SearchHistoryRepository 接口和实现类正确实现

## 仓库扩展
- [ ] AlbumRepository 包含 searchAlbums 方法
- [ ] ArtistRepository 包含 searchArtists 方法
- [ ] AlbumDao 包含搜索 SQL 查询
- [ ] 搜索方法返回 Flow 类型

## 用例层
- [ ] SearchAllUseCase 返回歌曲、专辑、歌手的合并搜索结果
- [ ] GetSearchHistoryUseCase 返回历史记录列表
- [ ] SaveSearchHistoryUseCase 正确保存搜索关键词
- [ ] ClearSearchHistoryUseCase 正确清除所有历史
- [ ] GetSearchSuggestionsUseCase 提供搜索建议

## UI 层
- [ ] SearchContract 定义完整的 UiState、Intent、Effect
- [ ] SearchViewModel 正确处理所有 Intent
- [ ] SearchScreen 包含搜索框、历史记录、搜索结果展示
- [ ] 搜索结果支持分类切换（歌曲/专辑/歌手）
- [ ] 点击歌曲可播放
- [ ] 点击专辑可导航到详情页
- [ ] 点击歌手可导航到详情页
- [ ] 搜索历史显示正确，支持点击搜索和清除

## 导航集成
- [ ] Screen.kt 包含 Search 路由定义
- [ ] AppNavGraph.kt 包含搜索页面 composable
- [ ] HomeScreen 包含搜索入口按钮
- [ ] 搜索页面可正常导航

## UI 风格
- [ ] 遵循 iOS 风格设计
- [ ] 使用项目现有的 IOS 组件
- [ ] 搜索框样式符合 iOS 风格
