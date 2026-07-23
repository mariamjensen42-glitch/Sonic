---
quick_id: 0723-player
description: Fix PlayerScreen inverted album art condition
---

Fix PlayerScreen.kt where the album art condition was inverted:
when `currentSong?.albumArt != null`, it showed `AlbumArtPlaceholder()`
instead of `AsyncImage`. Swap the real image and placeholder.
