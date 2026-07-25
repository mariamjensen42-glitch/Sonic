## Context

目前有四个详情页需要做封面主色提取作为背景渐变起点：

| 页面 | 现状 | 所需封面数据 |
|------|------|-------------|
| AlbumDetailScreen | ✅ 已实现 | `uiState.album?.albumArt` |
| ArtistDetailScreen | ❌ 固定 `AppBackground` | 艺术家歌曲列表中的封面 |
| PlaylistDetailScreen | ❌ 固定 `AppBackground` | 歌单首张歌曲封面 |
| PlayerScreen | ❌ 固定蓝色 `primary` | `uiState.currentSong?.albumArt` |

`rememberDominantColor` expect/actual 方案已在 AlbumDetailScreen 验证通过。Android 端使用 Coil + Palette 库提取，JVM 端（桌面）直接返回 fallback 色。

## Goals / Non-Goals

**Goals:**
- ArtistDetailScreen 使用艺术家歌曲封面提取主色
- PlaylistDetailScreen 使用歌单歌曲封面提取主色
- PlayerScreen 使用当前播放歌曲封面提取主色
- 保持代码简洁，尽量复用现有 `rememberDominantColor` 机制

**Non-Goals:**
- 不修改 DynamicTheme.kt（多色提取的脚手架，后续迭代）
- 不改变桌面端行为（JVM 端无 Palette 库，继续返回 fallback）
- 不做跨页面共享的颜色缓存
- 不修改 MiniPlayer（其背景色策略保持现状）

## Decisions

### 1. 提取源的选择

- **ArtistDetailScreen**: `uiState.songs.firstOrNull()?.albumArt` — 取艺术家歌曲列表的第一首有封面的歌。ViewModel 中 songs 已按专辑排序，第一首的代表性最高。若无 songs，整个列表为空时页面也无展示内容，fallback 即可。
- **PlaylistDetailScreen**: 同样取 `uiState.songs.firstOrNull()?.albumArt`，即歌单页面封面上展示的那首歌的封面。
- **PlayerScreen**: 取 `uiState.currentSong?.albumArt`，当前播放歌曲的封面，切换歌曲时自动重新提取。

**替代方案**: 由 ViewModel 提取后通过 state 下发。不采用此方案的原因是 ViewModel 中没有也不需要引入 Coil/Palette 依赖，expect/actual 机制在 Compose 层工作最自然。

### 2. 删除 gradientStartColor 参数

PlayerScreen 原有的 `gradientStartColor: Color = accentColor` 参数是外部注入颜色的入口，实现在 AppNavGraph 中从未传过任何值，因此始终是默认的 `primary`（蓝色）。改为内部使用 `rememberDominantColor` 后，该参数不再有用，将其删除。

**替代方案**: 保留参数但设为 `null`/可选。不必要的参数会增加调用方的理解负担，且当前没有其他调用方需要注入颜色。

### 3. 代码架构

采用与 AlbumDetailScreen 完全相同的模式：

```
val dominantColor = rememberDominantColor(imageUrl, AppBackground)
Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(dominantColor, Color.Black)))) {
    // content
}
```

不引入新的组件或 wrapper，保持最小改动。

## Risks / Trade-offs

- **JVM 端无提取**: 桌面端始终返回 fallback 色，背景永远是纯黑渐变。这不是问题 — 桌面端没有 Palette 库可用，且目前无桌面 UI 优化的需求。
- **Artist 封面不精确**: 艺术家的"代表性"封面难以定义，取第一首歌的封面未必是艺术家本人的照片。但项目中艺术家没有独立封面图字段，这已是当前数据模型下最佳方案。
- **提取延迟**: Palette 分析可能需要几百毫秒，期间使用 fallback 色。切换歌曲时玩家可能注意到背景短暂闪烁，但 AlbumDetailScreen 已证明此延迟在可接受范围内。
