---
status: complete
---

## Summary: M3ArtworkImage Component Refactor

**Status:** Complete ✓

### Changes
- **M3ArtworkImage.kt** — Added `fallback: @Composable (() -> Unit)?` parameter for placeholder content when artworkUrl is null
- **AlbumDetailScreen.kt** — 4 AsyncImage → M3ArtworkImage (header cover, thumbnail, song list)
- **PlaylistDetailScreen.kt** — 3 AsyncImage → M3ArtworkImage (header cover, thumbnail, song list)
- **PlayerScreen.kt** — 1 AsyncImage → M3ArtworkImage (large cover art)
- **HomeScreen.kt** — 2 AsyncImage → M3ArtworkImage (QuickPlayItem, DiscoverSection)

### Result
- 0 raw AsyncImage usages outside the M3ArtworkImage component
- All album art in every screen now uses the same reusable component
