## ADDED Requirements

### Requirement: Player screen SHALL extract dominant color from currently playing song's album art

PlayerScreen 的渐变背景起点色 SHALL 从当前播放歌曲的封面图中动态提取，并在切换歌曲时自动更新。

#### Scenario: A song with album art is playing
- **WHEN** `uiState.currentSong?.albumArt` 不为 null
- **THEN** 系统 SHALL 使用 `rememberDominantColor` 从当前播放歌曲的封面图提取主色
- **THEN** 提取的主色 SHALL 作为 `Brush.verticalGradient(listOf(dominantColor, Color.Black))` 的起点色

#### Scenario: Song changes while player is open
- **WHEN** `uiState.currentSong` 切换到另一首歌曲
- **THEN** 系统 SHALL 自动提取新歌曲封面图的主色并更新背景渐变

#### Scenario: Current song has no album art
- **WHEN** `uiState.currentSong?.albumArt` 为 null
- **THEN** 系统 SHALL 使用 `SpotifyColors.Background`（#0B0B0F）作为 fallback 颜色

#### Scenario: Color extraction is still in progress
- **WHEN** 封面图尚未加载完成或 Palette 分析未就绪
- **THEN** 系统 SHALL 使用 `SpotifyColors.Background` 作为过渡色
