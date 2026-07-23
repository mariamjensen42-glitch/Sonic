# Phase 1 Context: Screens Migration

## Design Decisions

### AlbumDetailScreen Migration

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Package | `com.cycling.sonic.presentation.albumdetail` | Match existing pattern |
| DI | Koin `viewModel { params -> AlbumDetailViewModel(...) }` | Replace Hilt `@AssistedInject` |
| VM params | `parametersOf(albumId)` → `params.get()` | Match existing `ArtistDetailViewModel` pattern |
| Player nav | Emit `AlbumDetailEffect.NavigateToPlayer(songId)` → caller navigates | Match existing pattern |
| AddToPlaylist | Use `PlaylistPickerDialog` (to be migrated in Phase 2) or inline stub for now | Keep it working with existing PlaylistRepository interface |
| Favorite toggle | `songRepository.toggleFavorite()` | Already available in domain layer |

### PlaylistDetailScreen Migration

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Package | `com.cycling.sonic.presentation.playlistdetail` | Match existing pattern |
| DI | Koin `viewModel { params -> PlaylistDetailViewModel(...) }` | Replace Hilt |
| VM params | `parametersOf(playlistId)` → `params.get()` | Match existing pattern |
| Android deps | Remove `Context`, `ArtColorExtractor` | Not available in KMP commonMain; accent color styling deferred |
| Toast effects | Use `PlaylistDetailEffect.ShowToast(message: String)` | Replace `@StringRes Int` with string literal |
| Player nav | Emit `PlaylistDetailEffect.NavigateToPlayer(songId)` → caller navigates | Match existing pattern |

### NavGraph Wiring

| Decision | Choice | Rationale |
|----------|--------|-----------|
| AlbumDetail entry | Replace `// TODO: AlbumDetailScreen not yet ported` | Wire with `koinViewModel<AlbumDetailViewModel> { parametersOf(key.albumId) }` |
| PlaylistDetail entry | Replace `// TODO: PlaylistDetailScreen not yet ported` | Wire with `koinViewModel<PlaylistDetailViewModel> { parametersOf(key.playlistId) }` |

### Migration Approach

- **Copy + adapt**: Take backup Java files, manually port to Kotlin KMP
- **Import mapping**: `com.cycling.domain.*` → `com.cycling.sonic.domain.*`
- **Timber**: Keep Timber logs for Android; remove on common if needed (already in existing pattern)
- **No PlayerViewModel connection yet**: Navigation-based approach (click → navigate to Player screen)

## Phase Scope

- Only AlbumDetail + PlaylistDetail screens
- No shared components (PlaylistPickerDialog deferred to Phase 2)
- No new tests (test infrastructure deferred to Phase 3)
- The AddToPlaylistDialog in AlbumDetailScreen will use a simple inline dialog instead of the full PlaylistPickerDialog
