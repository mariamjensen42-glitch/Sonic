## ADDED Requirements

### Requirement: Artist detail page SHALL extract dominant color from artist cover

ArtistDetailScreen 的渐变背景起点色 SHALL 从该艺术家的代表性封面图片中提取，而非使用固定颜色。

#### Scenario: Artist has songs with album art
- **WHEN** ArtistDetailScreen 加载且有歌曲列表
- **THEN** 系统 SHALL 使用 `rememberDominantColor` 从「该艺术家歌曲中最新/最有代表性的封面图」提取主色
- **THEN** 提取的主色 SHALL 作为 `Brush.verticalGradient(listOf(dominantColor, Color.Black))` 的起点色

#### Scenario: Artist has no songs or no album art
- **WHEN** 艺术家无歌曲或所有歌曲均无封面图
- **THEN** 系统 SHALL 使用 `AppBackground`（默认 #0B0B0F）作为 fallback 颜色

#### Scenario: Color extraction is still in progress
- **WHEN** 封面图尚未加载完成或 Palette 分析未就绪
- **THEN** 系统 SHALL 使用 `AppBackground` 作为过渡色，不产生闪烁
