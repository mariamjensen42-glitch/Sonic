# Requirements: Sonic — Backup Migration

**Defined:** 2026-07-23
**Core Value:** Users can browse, search, and play their local music library with a rich, Spotify-inspired experience

## v1 Requirements

### Screens Migration

- [ ] **MIGR-01**: Port AlbumDetailScreen (Contract + ViewModel + Screen) from `backup/presentation/.../albumdetail/` to `shared/src/commonMain/.../presentation/albumdetail/`
- [ ] **MIGR-02**: Port PlaylistDetailScreen (Contract + ViewModel + Screen) from `backup/presentation/.../playlistdetail/` to `shared/src/commonMain/.../presentation/playlistdetail/`
- [ ] **MIGR-08**: Wire AlbumDetail and PlaylistDetail entries in `AppNavGraph.kt`

### Shared Components Migration

- [ ] **MIGR-03a**: Port PieChart component from `backup/presentation/.../components/` to `shared/src/commonMain/.../presentation/components/`
- [ ] **MIGR-03b**: Port PlaylistPickerDialog component from `backup/presentation/.../components/` to `shared/src/commonMain/.../presentation/components/`
- [ ] **MIGR-03c**: Port SongComponents from `backup/presentation/.../components/` to `shared/src/commonMain/.../presentation/components/`

### Lyrics UI Components Migration

- [ ] **MIGR-04a**: Port Karaoke lyrics components (KaraokeBreathIndicator, KaraokeLineText, KaraokeLyricsLayout) from `backup/presentation/.../lyrics/components/`
- [ ] **MIGR-04b**: Port basic lyrics components (LyricsLayoutContainer, LyricsLineItem, SyncedLineText) from `backup/presentation/.../lyrics/components/`

### Test Migration

- [ ] **MIGR-05**: Port test infrastructure (TestDispatcherRule, SongFixture) from `backup/`
- [ ] **MIGR-06**: Port ViewModel tests (SearchViewModelTest, SongsViewModelTest) from `backup/`
- [ ] **MIGR-07**: Port domain tests (LrcParserTest, TimeUtilsTest, use case tests) from `backup/`

## v2 Requirements

### Desktop Data Layer

- **DESK-01**: Real data layer implementations for Desktop (Room, file scanner, player)
- **DESK-02**: Desktop audio playback

### Polish

- **POLISH-01**: Wire ArtColorExtractor into DynamicTheme
- **POLISH-02**: Room database migration strategy (replace fallbackToDestructiveMigration)
- **POLISH-03**: ProGuard/R8 optimization
- **POLISH-04**: CI pipeline setup

## Out of Scope

| Feature | Reason |
|---------|--------|
| Real desktop audio playback | Desktop data layer not implemented; deferred |
| CI pipeline | No infrastructure for CI yet |
| ProGuard/R8 optimization | Deferred until release build is needed |
| Database migration strategy | No production data; destructive migration acceptable for now |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| MIGR-01 | Phase 1 | Pending |
| MIGR-02 | Phase 1 | Pending |
| MIGR-08 | Phase 1 | Pending |
| MIGR-03a | Phase 2 | Pending |
| MIGR-03b | Phase 2 | Pending |
| MIGR-03c | Phase 2 | Pending |
| MIGR-04a | Phase 2 | Pending |
| MIGR-04b | Phase 2 | Pending |
| MIGR-05 | Phase 3 | Pending |
| MIGR-06 | Phase 3 | Pending |
| MIGR-07 | Phase 3 | Pending |

**Coverage:**
- v1 requirements: 11 total
- Mapped to phases: 11
- Unmapped: 0 ✓

---

*Requirements defined: 2026-07-23*
*Last updated: 2026-07-23 after initial definition*
