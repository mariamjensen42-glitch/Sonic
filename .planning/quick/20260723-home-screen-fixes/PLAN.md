---
quick_id: 0723-home
description: Fix Home screen — add missing album thumbnails, subtitle, discover backgrounds
---

Fix three issues in HomeScreen.kt:
1. QuickPlayItem missing album thumbnail (56dp AsyncImage)
2. GreetingSection missing subtitle (home_greeting_subtitle)
3. DiscoverSection missing album art background
4. Hardcoded strings → stringResource
