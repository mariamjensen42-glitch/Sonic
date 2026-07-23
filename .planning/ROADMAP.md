# Roadmap: Sonic — Backup Migration

**Created:** 2026-07-23
**Phases:** 3
**Requirements mapped:** 11/11 ✓

## Phase 1: Screens Migration

**Goal:** Port AlbumDetailScreen and PlaylistDetailScreen from backup, wire into navigation
**Mode:** mvp

**Requirements:**
- MIGR-01 — Port AlbumDetailScreen
- MIGR-02 — Port PlaylistDetailScreen
- MIGR-08 — Wire entries in AppNavGraph

**Success Criteria:**
1. User can navigate to AlbumDetailScreen from album list and see album info
2. User can navigate to PlaylistDetailScreen from playlist list and see playlist songs
3. Album detail shows album art, title, artist, song list with play actions
4. Playlist detail shows playlist name, song count, song list with play/remove actions
5. Back navigation works correctly from both screens
6. Screens compile and render on both Android and Desktop targets

**Plans:**
1. Port AlbumDetailContract + AlbumDetailViewModel + AlbumDetailScreen
2. Port PlaylistDetailContract + PlaylistDetailViewModel + PlaylistDetailScreen
3. Wire both screens in AppNavGraph (replace TODO stubs)

---

## Phase 2: Components Migration

**Goal:** Port shared UI components and lyrics UI components from backup
**Mode:** mvp

**Requirements:**
- MIGR-03a — PieChart
- MIGR-03b — PlaylistPickerDialog
- MIGR-03c — SongComponents
- MIGR-04a — Karaoke lyrics components
- MIGR-04b — Basic lyrics components

**Success Criteria:**
1. PieChart renders correctly in library stats view
2. PlaylistPickerDialog opens, shows playlists, allows adding songs
3. SongComponents render song items with consistent styling
4. Karaoke lyrics display with syllable-level highlighting during playback
5. Basic lyrics components render synced lines correctly
6. All components compile on both Android and Desktop targets

**Plans:**
1. Port shared components (PieChart, PlaylistPickerDialog, SongComponents)
2. Port karaoke lyrics components (BreathIndicator, LineText, LyricsLayout)
3. Port basic lyrics components (LayoutContainer, LineItem, SyncedLineText)

---

## Phase 3: Tests Migration

**Goal:** Port test infrastructure and test cases from backup
**Mode:** mvp

**Requirements:**
- MIGR-05 — TestDispatcherRule, SongFixture
- MIGR-06 — ViewModel tests (SearchViewModelTest, SongsViewModelTest)
- MIGR-07 — Domain tests (LrcParserTest, TimeUtilsTest, use case tests)

**Success Criteria:**
1. TestDispatcherRule compiles and correctly sets Main dispatcher for tests
2. SongFixture creates test Song instances with default values
3. LrcParserTest passes: LRC parsing works for standard formats
4. TimeUtilsTest passes: time conversion utilities work
5. Use case tests pass (GetAllSongsUseCaseTest, GetLyricsUseCaseTest, ToggleFavoriteUseCaseTest)
6. SearchViewModelTest passes: search queries, history tracking work
7. SongsViewModelTest passes: song list operations work
8. `./gradlew test` succeeds with all migrated tests passing

**Plans:**
1. Port test infrastructure (TestDispatcherRule, SongFixture)
2. Port domain tests (LrcParserTest, TimeUtilsTest, use case tests)
3. Port ViewModel tests (SearchViewModelTest, SongsViewModelTest)

---

## Summary

| Phase | Name | Requirements | Plans |
|-------|------|-------------|-------|
| 1 | Screens Migration | MIGR-01, MIGR-02, MIGR-08 | 3 |
| 2 | Components Migration | MIGR-03a, MIGR-03b, MIGR-03c, MIGR-04a, MIGR-04b | 3 |
| 3 | Tests Migration | MIGR-05, MIGR-06, MIGR-07 | 3 |

**Total: 3 phases, 11 requirements, 9 plans**
