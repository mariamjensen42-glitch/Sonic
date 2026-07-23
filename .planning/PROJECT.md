# Sonic

## What This Is

Sonic is a Kotlin Multiplatform (KMP) music player built with Compose Multiplatform + Material 3. It uses Clean Architecture with MVI pattern and AI-powered music insights via DeepSeek. The app targets Android and Desktop platforms from a shared codebase.

## Core Value

Users can browse, search, and play their local music library with a rich, Spotify-inspired experience — including synchronized lyrics, AI-driven artist/album insights, and full background playback on Android.

## Requirements

### Validated

- ✓ KMP project structure with `shared/` `androidApp/` `desktopApp` modules — existing
- ✓ Clean Architecture with domain layer in `commonMain` — existing
- ✓ MVI pattern (UiState/Intent/Effect) across all features — existing
- ✓ Navigation 3 with `@Serializable` type-safe routes — existing
- ✓ Koin dependency injection — existing
- ✓ Room 3 database with DAOs and entities — existing (androidMain)
- ✓ DataStore Preferences for key-value storage — existing (androidMain)
- ✓ Media3 ExoPlayer + MediaSessionService for background playback — existing (androidMain)
- ✓ MediaStore audio scanning — existing (androidMain)
- ✓ DeepSeek AI integration for music info — existing (androidMain)
- ✓ Core screens: Home, Search, Library/MyMusic, Songs, Albums, Artists, Player, Lyrics, Folder, Settings, ArtistDetail, SongDetail, AiInfo — existing
- ✓ Lyrics subsystem: LRC, Enhanced LRC, TTML, LyricifySyllable parsers — existing
- ✓ Spotify-inspired dark theme — existing
- ✓ Coil 3 image loading — existing
- ✓ Test dependencies declared (MockK, Turbine, coroutines-test) — existing
- ✓ Backup reference code in `backup/` — existing

### Active

- [ ] **MIGR-01**: Port AlbumDetailScreen from `backup/` `com.cycling.presentation.albumdetail` to `shared/src/commonMain`
- [ ] **MIGR-02**: Port PlaylistDetailScreen from `backup/` `com.cycling.presentation.playlistdetail` to `shared/src/commonMain`
- [ ] **MIGR-03**: Port shared components (PieChart, PlaylistPickerDialog, SongComponents) from `backup/` `com.cycling.presentation.components`
- [ ] **MIGR-04**: Port lyrics UI components (KaraokeBreathIndicator, KaraokeLineText, KaraokeLyricsLayout, LyricsLayoutContainer, LyricsLineItem, SyncedLineText) from `backup/` `com.cycling.presentation.lyrics.components`
- [ ] **MIGR-05**: Port test infrastructure (TestDispatcherRule, SongFixture) from `backup/`
- [ ] **MIGR-06**: Port ViewModel tests (SearchViewModelTest, SongsViewModelTest) from `backup/`
- [ ] **MIGR-07**: Port domain tests (LrcParserTest, TimeUtilsTest, use case tests) from `backup/`
- [ ] **MIGR-08**: Wire AlbumDetail and PlaylistDetail entries in AppNavGraph
- [ ] **MIGR-09**: Port resource files (drawables, strings.xml) from `backup/` to CMP compose resources

### Out of Scope

- Desktop data layer implementations — deferred until Android data layer stabilizes
- ProGuard/R8 optimization — deferred
- CI pipeline setup — deferred
- Real desktop audio playback — deferred

## Context

The project was initially built from a Compose Multiplatform template and has been substantially re-architected. The `backup/` directory contains the original Android-only implementation with Java code in a multi-module structure (domain/data/presentation/app). Much of the domain layer and data layer have already been ported to the KMP Kotlin structure in `shared/`. Several presentation screens, components, and tests remain to be ported.

Known technical debt from codebase analysis (see `.planning/codebase/CONCERNS.md`):
- AlbumDetailScreen and PlaylistDetailScreen navigation entries are stubbed with TODOs
- No meaningful test coverage (3 placeholder tests only)
- All `commonMain` repository interfaces backed by stubs (real impls in `androidMain`)
- Desktop has no data layer — non-functional for real playback
- Room DB uses `fallbackToDestructiveMigration()` — no migration strategy
- PlayerViewModel is a singleton (intentional but risky)
- DynamicTheme always returns default colors (ArtColorExtractor not wired)

## Constraints

- **Language**: Kotlin 2.3.0 only, no Java
- **UI**: Compose Multiplatform + Material 3, no XML views
- **Architecture**: Clean Architecture + MVI (must follow existing patterns)
- **Package**: `com.cycling.sonic`
- **DI**: Koin (existing pattern)
- **JDK**: 17 (jvmToolchain(17))
- **Android**: compileSdk/targetSdk 36, minSdk 30

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| KMP over Android-only | Cross-platform music player from shared codebase | ✓ Good |
| Clean Architecture + MVI | Clear separation of concerns, testability | ✓ Good |
| Stub repositories in commonMain | Enables compilation without platform deps | ✓ Good |
| PlayerViewModel as singleton | Shared player state across screens | ⚠️ Revisit |
| fallbackToDestructiveMigration | Quick iteration, no prod data yet | ⚠️ Revisit |

---

*Last updated: 2026-07-23 after initialization*
