---
status: complete
---

## Summary: Home Screen Migration Completion

**Status:** Complete ✓

### Changes
- **QuickPlayItem**: Split into `QuickPlayGrid` + `QuickPlayItem` with 56dp album art thumbnail using AsyncImage
- **GreetingSection**: Subtitle now uses `stringResource(Res.string.home_greeting_subtitle)` instead of hardcoded name
- **DiscoverSection**: Added AsyncImage background for album art when available
- **SectionHeader**: "显示全部" → `stringResource(Res.string.home_view_all)`
- **ArtistCard**: "歌手" → `stringResource(Res.string.home_artist_label)`

### Files Modified
- `shared/.../home/HomeScreen.kt` — 4 fixes applied
