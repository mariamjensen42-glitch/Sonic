# Checklist

## 数据层
- [ ] SongEntity 包含 isFavorite, playCount, lastPlayedAt 字段
- [ ] Song domain model 包含对应字段
- [ ] 数据库迁移策略正确实现
- [ ] SongDao 包含收藏歌曲查询方法
- [ ] SongDao 包含按播放次数排序查询方法
- [ ] SongDao 包含按最后播放时间排序查询方法
- [ ] SongRepository 接口定义完整
- [ ] SongRepositoryImpl 正确实现所有方法

## 用例层
- [ ] ToggleFavoriteUseCase 正确切换收藏状态
- [ ] GetFavoriteSongsUseCase 返回所有收藏歌曲
- [ ] GetMostPlayedSongsUseCase 返回按播放次数排序的歌曲
- [ ] GetRecentlyPlayedSongsUseCase 返回按播放时间排序的歌曲
- [ ] UpdatePlayStatsUseCase 正确更新播放统计

## 播放器集成
- [ ] PlayerManager 正确监听播放进度
- [ ] 播放进度超过 50% 时更新播放次数
- [ ] 最后播放时间正确记录

## UI 层 - 播放器
- [ ] PlayerScreen 显示收藏按钮
- [ ] 收藏按钮状态正确反映歌曲收藏状态
- [ ] 点击收藏按钮正确切换状态
- [ ] 收藏状态变化有视觉反馈（Toast 或 Snackbar）

## UI 层 - 首页
- [ ] HomeScreen 显示"喜欢的歌曲"入口
- [ ] HomeScreen 显示"最近播放"入口
- [ ] HomeScreen 显示"最常播放"入口
- [ ] 点击入口正确导航到对应页面

## UI 层 - 收藏页面
- [ ] FavoritesScreen 正确显示收藏歌曲列表
- [ ] 收藏页面支持播放全部歌曲
- [ ] 取消收藏后列表正确更新

## UI 层 - 最近播放页面
- [ ] RecentlyPlayedScreen 正确显示播放历史
- [ ] 歌曲按最后播放时间降序排列
- [ ] 点击歌曲可播放

## UI 层 - 最常播放页面
- [ ] MostPlayedScreen 正确显示最常播放歌曲
- [ ] 歌曲按播放次数降序排列
- [ ] 显示每首歌曲的播放次数

## 导航
- [ ] 收藏页面导航路由正确配置
- [ ] 最近播放页面导航路由正确配置
- [ ] 最常播放页面导航路由正确配置
