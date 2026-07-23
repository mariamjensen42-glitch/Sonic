# Phase 4: 很多页面都是scoffold布局的，topbar和内容的，应该写一个布局的复用组件，方便统一。 - Context

**Gathered:** 2026-07-24
**Status:** Ready for planning (replan)

<domain>
## Phase Boundary

Create a reusable `M3Scaffold` layout wrapper component that standardizes the Scaffold + TopAppBar + content pattern, then migrate existing screens that follow this pattern to use it. Pure UI abstraction — no logic, no data, no network.
</domain>

<decisions>
## Implementation Decisions

### System Bar Insets Handling
- **D-01:** M3Scaffold delegates insets entirely to Material3 Scaffold's built-in WindowInsets behavior. No extra `.statusBarsPadding()` or manual inset handling inside M3Scaffold. Screens that previously had manual `.statusBarsPadding()` (e.g., AiInfoScreen) must remove it to avoid double-padding. — **Reversibility:** reversible — local to M3Scaffold and its callers.

### the agent's Discretion
- **Component API design:** M3Scaffold should expose `containerColor`, `contentColor`, `topBar`, `bottomBar`, `modifier`, and `content`. Additional Scaffold params (e.g., `windowInsets`) are not exposed — callers who need non-standard behavior should use raw Scaffold directly. The current plan (04-01-PLAN.md) matches this approach.
- **Default containerColor:** Keep `SpotifyColors.Background` as default. SongsScreen passes explicit `MaterialTheme.colorScheme.surface` override via the `containerColor` parameter — this proves the override path works. Discard changing the default.
- **Excluded screens confirmed:** AlbumDetail, ArtistDetail, PlaylistDetail, Lyrics (float TopAppBar over gradient — fundamentally different pattern) and Home, Search, MyMusic, Player (custom headers — different layout structure) are explicitly excluded from this migration.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project Conventions
- `AGENTS.md` — Project rules: Compose Multiplatform, M3 prefix for reusable components, Clean Architecture + MVI
- `.planning/codebase/CONVENTIONS.md` — Code conventions: MVI pattern, M3 component naming, Compose patterns

### Existing Components (pattern references)
- `.planning/phases/04-scoffold-topbar/04-RESEARCH.md` — Phase research with screen-by-screen migration analysis
- `.planning/phases/04-scoffold-topbar/04-PATTERNS.md` — Pattern mapping with concrete code excerpts

### Relevant Code
- `shared/src/commonMain/kotlin/com/cycling/sonic/core/ui/components/` — Existing M3 components (M3ArtworkImage, etc.)
- `shared/src/commonMain/kotlin/com/cycling/sonic/core/ui/theme/SpotifyColors.kt` — Theme colors including `Background`
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- M3ArtworkImage: Existing M3-prefixed reusable component in `core/ui/components/` — follows the same pattern M3Scaffold should follow.
- SpotifyColors: Theme object with `Background`, `Surface`, `TextPrimary` etc. — used as default colors.

### Established Patterns
- M3 prefix convention: All reusable components in `core/ui/` get the `M3` prefix (per AGENTS.md).
- Material3 Scaffold with `@OptIn(ExperimentalMaterial3Api::class)` — M3Scaffold hides this from callers.

### Integration Points
- `core/ui/components/` — Where M3Scaffold.kt will be created.
- `presentation/albums/AlbumsScreen.kt`, `presentation/artists/ArtistsScreen.kt`, `presentation/songs/SongsScreen.kt`, `presentation/settings/SettingsScreen.kt` — Pattern A (Scaffold → M3Scaffold).
- `presentation/folder/FolderScreen.kt`, `presentation/ai/AiInfoScreen.kt` — Pattern C (Column → M3Scaffold).
</code_context>

<specifics>
## Specific Ideas

- Existing plans (04-01, 04-02, 04-03) should be replanned to incorporate this context. The current plan structure (Wave 1: tracer, Wave 2: remaining screens) is acceptable.
</specifics>

<deferred>
## Deferred Ideas

- HomeScreen, SearchScreen, MyMusicScreen (Pattern D — custom headers) — potential future migration, not in this phase.
- AlbumDetail, ArtistDetail, PlaylistDetail, Lyrics (Pattern B — floating TopAppBar over gradient) — fundamentally different layout, not applicable.

None — discussion stayed within phase scope.
</deferred>

---

*Phase: 4-很多页面都是scoffold布局的，topbar和内容的，应该写一个布局的复用组件，方便统一。*
*Context gathered: 2026-07-24*
