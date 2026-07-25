## ADDED Requirements

### Requirement: Playlist detail page SHALL extract dominant color from first song's album art

PlaylistDetailScreen 的渐变背景起点色 SHALL 从歌单内歌曲的封面图中提取，而非使用固定颜色。

#### Scenario: Playlist has songs with album art
- **WHEN** PlaylistDetailScreen 加载且有歌曲
- **THEN** 系统 SHALL 使用 `rememberDominantColor` 从 `uiState.songs.firstOrNull()?.albumArt` 提取主色
- **THEN** 提取的主色 SHALL 作为 `Brush.verticalGradient(listOf(dominantColor, Color.Black))` 的起点色

#### Scenario: Playlist is empty or songs have no album art
- **WHEN** 歌单为空或所有歌曲均无封面图
- **THEN** 系统 SHALL 使用 `AppBackground`（默认 #0B0B0F）作为 fallback 颜色

#### Scenario: Color extraction is still in progress
- **WHEN** 封面图尚未加载完成或 Palette 分析未就绪
- **THEN** 系统 SHALL 使用 `AppBackground` 作为过渡色
