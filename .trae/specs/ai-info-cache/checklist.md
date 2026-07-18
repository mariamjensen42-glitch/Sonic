# Checklist

- [x] AiInfoCacheEntity 实体类正确定义，包含 type、cacheKey、title、content 字段
- [x] AiInfoCacheDao 包含按 cacheKey 查询、插入、删除方法
- [x] AppDatabase 正确注册 AiInfoCacheEntity
- [x] AiRepository 接口新增 getCachedSongInfo、getCachedArtistInfo、getCachedAlbumInfo 方法
- [x] AiRepositoryImpl 实现缓存优先逻辑：先查缓存，无缓存再调 API 并保存
- [x] AiInfoViewModel 正确使用缓存优先逻辑
- [x] 首次请求调用 API 并缓存成功
- [x] 再次请求直接从缓存读取，不调用 API
