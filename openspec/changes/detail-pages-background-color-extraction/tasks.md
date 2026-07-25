## 1. ArtistDetailScreen - 接入封面主色提取

- [x] 1.1 导入 `rememberDominantColor` 和 `AppBackground`（已存在）
- [x] 1.2 将 `var dominantColor by remember { mutableStateOf(AppBackground) }` 替换为 `val dominantColor = rememberDominantColor(uiState.songs.firstOrNull()?.albumArt, AppBackground)`
- [x] 1.3 验证：无歌曲时 fallback 到 AppBackground，有歌曲时动态提取主色

## 2. PlaylistDetailScreen - 接入封面主色提取

- [x] 2.1 导入 `rememberDominantColor`
- [x] 2.2 将 `var dominantColor by remember { mutableStateOf(AppBackground) }` 替换为 `val dominantColor = rememberDominantColor(uiState.songs.firstOrNull()?.albumArt, AppBackground)`
- [x] 2.3 验证：空歌单时 fallback，有歌曲时提取主色

## 3. PlayerScreen - 接入封面主色提取

- [x] 3.1 导入 `rememberDominantColor` 和 `SpotifyColors.Background`
- [x] 3.2 添加 `val dominantColor = rememberDominantColor(uiState.currentSong?.albumArt, SpotifyColors.Background)`
- [x] 3.3 将 `gradientStartColor` 引用替换为 `dominantColor`
- [x] 3.4 删除了 `gradientStartColor` 参数，保留 `accentColor`（用于队列UI装饰）

## 4. AppNavGraph - 清理过时参数

- [x] 4.1 PlayerScreen 调用处未传 `gradientStartColor`，无变更
