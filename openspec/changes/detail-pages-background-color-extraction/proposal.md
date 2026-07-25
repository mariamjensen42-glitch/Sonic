## Why

当前详情页（ArtistDetail、PlaylistDetail、Player）的背景色没有从封面图片中提取主色，始终使用固定颜色或默认蓝色，而 AlbumDetail 已正确实现了封面主色提取。这导致不同详情页的品牌一致性差，UI 体验割裂。

## What Changes

1. **ArtistDetailScreen**: 将 `rememberDominantColor` 接入艺术家相关封面图，动态提取主色作为渐变背景起点
2. **PlaylistDetailScreen**: 将 `rememberDominantColor` 接入歌单歌曲的首张封面图，动态提取主色作为渐变背景起点
3. **PlayerScreen**: 将 `rememberDominantColor` 接入当前播放歌曲的封面图，动态提取主色作为渐变背景起点；移除 `gradientStartColor` 参数（不再需要外部传入）
4. **AppNavGraph**: 移除 PlayerScreen 的 `gradientStartColor` 相关过时参数

## Capabilities

### New Capabilities
- `artist-detail-dominant-color`: 在 ArtistDetailScreen 中从专辑封面提取主色
- `playlist-detail-dominant-color`: 在 PlaylistDetailScreen 中从歌单封面提取主色
- `player-dominant-color`: 在 PlayerScreen 中从当前播放歌曲封面提取主色

### Modified Capabilities
无

## Impact

- 修改 4 个文件：ArtistDetailScreen.kt、PlaylistDetailScreen.kt、PlayerScreen.kt、AppNavGraph.kt
- 依赖 `rememberDominantColor` expect/actual 机制（已在 AlbumDetailScreen 验证可用）
- Android 端通过 Coil + Palette 提取，JVM 端返回 fallback 色，不影响桌面端功能
