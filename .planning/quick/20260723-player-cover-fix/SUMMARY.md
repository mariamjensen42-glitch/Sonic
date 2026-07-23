---
status: complete
---

## Summary: PlayerScreen Cover Art Fix

**Status:** Complete ✓

### Root Cause
`PlayerScreen.kt` line 135: `if (currentSong?.albumArt != null) { AlbumArtPlaceholder() }` — condition was inverted.

### Fix
Replaced `AlbumArtPlaceholder()` with `AsyncImage(model = currentSong.albumArt, ...)` when albumArt is present.

### Files Modified
- `shared/.../player/PlayerScreen.kt` — 1 line fix (replaced placeholder with AsyncImage)
