# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

> **⛔ Highest-priority rule (overrides everything else in this file): never run `./gradlew` /
> `gradlew.bat`, `gradle`, or any assemble / build / compile / test / lint / install task.
> Builds are too slow and are the user's job; verify by reading code (read / grep / glob) only.**
> **⛔ 最高优先级规则：严禁执行 `./gradlew` / `.\gradlew.bat`、`gradle` 以及任何构建/编译/测试任务；只做静态检查。**

## Source of truth

`AGENTS.md` (repo root, auto-loaded) is the project's stated rulebook — module layout, package
names, MVI naming, dependency direction, commit style. Follow it, **except where it disagrees
with the code, in which case the code wins.** Known drift:

| `AGENTS.md` says | Reality |
| --- | --- |
| `core/ui/theme/` = theme objects | `Theme.kt` → `SonicTheme` (one entry point), backed by `Color.kt` + `Hct.kt` + `TonalPalette.kt` (HCT tonal mapping), `Typography.kt`, `Shape.kt`, `MotionTiming.kt`, `Spacing.kt`, `ArtColorExtractor.kt` (Palette profiles → seed + backdrop colours), `CoverArtworkLoader.kt` (cover bitmap: MediaStore thumbnail → image stream → embedded art), `SonColorField.kt` (static hero glow) and `SonDynamicBackdrop.kt` (flowing backdrop behind the Player and Lyrics screens, ADR-0005). There is no `SpotifyTheme`/`SpotifyColors`/`DesignTokens.kt` any more — the UI is Material 3 Expressive only. `core/ui/components/` holds **only** `MediaCards.kt` (`M3Artwork`, `M3MediaCard`, `M3ArtistCard`) plus `navigation/M3BottomNavigationBar`; the former `M3Button`/`M3Card`/`M3ListItem`/`M3Indicator`/`M3TextField`/`M3Chip`/`M3Navigation`/`M3Interaction` wrapper files were deleted — screens call `androidx.compose.material3` directly |
| iOS-style components (`IOSListItem`, `IOSButton`, `IOSInsetGrouped`) | Removed. Reusable components are `M3*` under `com.cycling.core.ui.components` |
| `MainViewModel` manages the theme | It only calls `playerManager.connect()` to bootstrap playback |
| DeepSeek key injected from `local.properties` via `BuildConfig` | No `buildConfigField` exists anywhere. The key is entered by the user and stored in DataStore |
| Release signing via `key.properties` | 两条路都认：**环境变量优先**（`SONIC_STORE_FILE` / `SONIC_STORE_PASSWORD` / `SONIC_KEY_ALIAS` / `SONIC_KEY_PASSWORD`，CI 用），其次仓库根的 `key.properties`（本地用，`storeFile` 相对 `:app`）。两者都没有时回落 **debug 签名**并打 warning，所以本地 `assembleRelease` 能出包，但产物不能发布 |
| `domain/.../lyrics/exporter/` exists | Deleted from the working tree |
| `spotify-clone-jetpack-compose-main/` reference project | Not present |
| Turbine asserts Flow behaviour | Declared in `:data` and `:presentation`, imported by no test |
| All UseCases are `@Singleton` with `operator fun invoke` | Only `GetLyricsUseCase` is `@Singleton`; `ScanMusicUseCase`, `SearchHistoryUseCase`, `SongQueryUseCase` expose named methods instead |
| Metadata reading/writing is mp3agic (MP3) | MP3 still uses mp3agic; FLAC / OGG-Vorbis / MP4(M4A) read+write go through `:taglib`, a vendored copy of the local SaltAudioTag `core` module (LGPL-2.1, `taglib/LICENSE`). Only `:data`'s `AudioMetadataHelper` touches it. Opus is read-only |
| Root `README.md` / `CONTRIBUTING.md` tech-stack versions | Two Kotlin/Compose generations behind — trust `gradle/libs.versions.toml` |

| `AGENTS.md` says Compose BOM only | `composeBom = "2026.09.00"`, but **`androidx.compose.material3` is explicitly pinned to `1.5.0-alpha29`** in the catalog, overriding the BOM's constraint (the BOM pins material3 1.4.0, which lacks `LoadingIndicator`, `WavyProgressIndicator`, `ListItem(onClick)`, `ButtonShapes`). The pin is deliberate — see the `composeMaterial3` comment in `gradle/libs.versions.toml`. Consequence: `androidx.compose.foundation:foundation` is dragged to `1.13.0-alpha01` by material3's own requirement |

## Commands

> **The commands below are for the user only; the agent must not run any build/compile/test task.**

```bash
./gradlew build                 # full build (Windows: gradlew.bat)
./gradlew :app:assembleDebug    # debug APK only
./gradlew installDebug          # build + install on device/emulator
./gradlew test                  # every module's JVM unit tests
./gradlew lint                  # Android Lint
./gradlew :domain:test          # one module (also :data:, :presentation:, :taglib: — :app: has no tests)
```

Single test class or method — the test modules (`:domain`, `:data`, `:presentation`, `:taglib`) are
Android libraries, so the task is `testDebugUnitTest`, and `--tests` matches the **literal Kotlin
name, spaces included**, hence the quoting:

```bash
./gradlew :domain:testDebugUnitTest --tests "com.cycling.domain.lyrics.parser.LrcParserTest"
./gradlew :domain:testDebugUnitTest --tests "com.cycling.domain.lyrics.parser.LrcParserTest.parse standard LRC lyrics correctly"
```

Instrumented tests exist only in `:data` and need a device:

```bash
./gradlew :data:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.cycling.data.local.database.DatabaseMigrationTest
```

Performance benchmarks live in `:benchmark` (AndroidX Macrobenchmark, `com.android.test`,
`targetProjectPath = ":app"`). They run against `:app`'s `benchmark` build type (release-shaped,
debug-signed, plus a `profileable` in `app/src/benchmark/AndroidManifest.xml`), need a device, and
the emulator refuses to run them unless you suppress `EMULATOR`:

```bash
./gradlew :benchmark:connectedBenchmarkAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR,LOW-BATTERY
```

`docs/benchmarking.md` explains what each test measures.

Release (and the `benchmark` variant that inherits it) now run **R8 with resource shrinking**
(`isMinifyEnabled = true` / `isShrinkResources = true`), so `app/proguard-rules.pro` is live — the
rules in it must stay minimal, because every `-keep` gives back part of what R8 buys: the APK went
from 41.2 MB to 7.1 MB and cold start from 484 ms to 282 ms on the reference device. One build
config is still inert: `android.yml` and `release.yml` both trigger on `v*` tags, so tagging a
release runs the publish workflow twice.

## Architecture

### Playback: one `ExoPlayer`, two owners

The single non-obvious design in this repo. `PlayerModule` provides **one `@Singleton ExoPlayer`**
that is injected into *both* `PlayerService` and `ExoPlayerPlaybackEngine`.
`PlayerManager` (which implements `PlayerRepository`, the domain interface) never binds to or
talks to the service — it drives that same ExoPlayer instance directly. `PlayerService` is a
`MediaSessionService` that exists only to own the `MediaSession` behind the notification,
lock screen, Bluetooth and Android Auto; it holds no queue logic. This works because the service
declares no `android:process`. The engine's KDoc rejects `MediaController` deliberately: it can't
reach ExoPlayer-only calls (`setSkipSilenceEnabled`, `setAudioSessionId`) and would add a Binder
transaction per volume frame. `PlaybackServiceController` (`start()`) exists so `PlayerManager`
never needs a `Context`.

Threading: every ExoPlayer call is funnelled through the engine's `runOnMain`, and `PlayerManager`
runs entirely on `@PlayerDispatcher` (`Dispatchers.Main.immediate`); only DB writes hop to IO.
Because engine calls are *posted*, they may not have taken effect when the call returns — read
`engine.state`, not the player. `PlayerManager` holds no Android types and is unit-tested against
a `FakePlaybackEngine`.

State: `PlayerRepository.playerState: StateFlow<PlayerState>` is **derived**, not stored — it
combines `engine.state` with `engine.events`, plus a 1-second progress loop, because
`EngineState` deliberately excludes position (so `playbackPosition` can be ~1s stale).

The queue lives inside **ExoPlayer's timeline**, not a parallel list; `EngineState` mirrors it as
`queueIds: List<Long>` + `currentIndex`. Since a `MediaItem` can only carry an id, `PlayerManager`
keeps a private `songLookup: MutableMap<Long, Song>` to resolve ids back to `Song`s. **This map is
a live bug surface** — the UI queue is rebuilt from it on every engine change, so any `Song`
mutation that misses it silently reverts (this is why `setSongFavorite` updates the map first).

Restart persistence is `PlaybackStateStore` (DataStore `player_state`): queue ids, index,
position, repeat, shuffle, `wasPlaying`. Shuffle *order* is not persisted (media3 regenerates it),
so `setShuffleEnabled` must be applied **before** `setQueue`.

### Navigation 3, and the one ViewModel that outlives it

`presentation/src/main/java/com/cycling/presentation/navigation/` is worth reading before touching
any screen. `NavigationState` holds **not one backstack but a map of them**, one per top-level tab:

```kotlin
val backStacks: Map<NavKey, NavBackStack<NavKey>>   // one per top-level route
var topLevelRoute: MutableState<NavKey>             // the active tab
```

`Navigator` is a plain class (`remember { Navigator(state) }` in `MainActivity`) — not Hilt, not a
ViewModel. Its whole rule: `navigate(key)` switches tab if `key in backStacks.keys`, otherwise
**pushes onto the current tab's stack**; `goBack()` pops, or at a tab root jumps to Home.
`LibraryDestination` / `SettingsDestination` are only sub-types of `Screen` for namespacing —
they are not nested graphs.

`NavigationState.toEntries()` applies `rememberSaveableStateHolderNavEntryDecorator` +
`rememberViewModelStoreNavEntryDecorator`, so each `entry<…>` block gets **its own ViewModelStore**:
`hiltViewModel()` called inside an entry is scoped to that entry and cleared when it is popped.
Entries needing a route argument use a Hilt assisted factory —
`hiltViewModel<T, T.Factory>(creationCallback = { it.create(key) })`.

Two consequences a newcomer trips on:

- **`PlayerViewModel` is app-global.** `MainActivity` creates it once at the Activity level, above
  every entry-scoped `ViewModelStore`, and threads it manually through `MainScreen → AppNavGraph`
  into each entry. `PlayerScreen`/`LyricsScreen` therefore take raw `uiState` + `onIntent`, not a
  `viewModel`; other screens take `viewModel: XxxViewModel = hiltViewModel()`. Sources that start
  playback call `playerViewModel.handleIntent(PlayerIntent.PlaySong(song, queue))` and then
  `navigator.navigate(Screen.Player)` from the entry lambda.
- **Navigation is driven by the Screen, not the ViewModel.** A ViewModel emits only a
  `XxxEffect` (e.g. `NavigateToAlbumDetail(id)`); the entry lambda in `AppNavGraph` supplies the
  callback that calls `navigator.navigate(...)`. The Player's single `ShowToast` effect is
  collected at the **host** level in `AppNavGraph`, not in `PlayerScreen`.

The bottom bar is an **overlay**, not a `Scaffold` bottomBar: `MainActivity` `Box`-stacks
`AppNavGraph` and `M3BottomNavigationBar` (Material's standard `NavigationBar` + `NavigationBarItem` — *not* `ShortNavigationBar`, which is the flexible-expressive variant and hides non-selected labels), and `NavigationState.recomputeBottomBarVisibility()`
hides the bar whenever a non-tab screen is on top. Screens take an explicit `bottomPadding: Dp`
to leave room for it.

Also note the naming mismatch: the `Screen.Library` entry renders `MyMusicScreen`.

### Lyrics

All lyric times are **`Int` milliseconds**. The lyrics tree is polymorphic:
`ISyncedLine` (`start`/`end`/`duration`) is implemented by line-level `SyncedLine` and
word-level `KaraokeLine` (→ `KaraokeSyllable`, plus `isAccompaniment` for `x-bg` backing vocals
and `KaraokeAlignment` for duet voice). `SyncedLyrics.getCurrentAllHighlightLineIndicesByTime()`
returns a **list** because lines may legitimately overlap.

Parser selection is a **name-keyed registry plus a regex format guesser**, not a factory or chain:

- `AutoParser` registers `"LRC"`, `"ENHANCED_LRC"`, `"TTML"`, `"LYRICIFY_SYLLABLE"`,
  `"WORD_BY_WORD_LRC"`, `"YRC"` and exposes the only public entry point.
- `LyricsFormatGuesser.registerFormat` **prepends**, so registration order is reversed into probe
  priority: `YRC → LYRICIFY_SYLLABLE → ENHANCED_LRC → WORD_BY_WORD_LRC → LRC → TTML`. This ordering
  is load-bearing — `WORD_BY_WORD_LRC`'s two-tag detector must beat plain `LRC`'s single-tag one.
- `YRC` is NetEase's word-by-word syntax, `[start,duration](start,duration,0)word…` in absolute
  milliseconds; unlike `WORD_BY_WORD_LRC` its tags carry their own durations. Its detector demands a
  syllable tag, because Lyricify Lines' `[start,end]` line tag looks the same.
- No format matches → an **empty `SyncedLyrics`, silently**. Unmatched input is not an error.

A translation reaches the parsers as **the line directly below the line it translates**, carrying
that line's tag. Every parser pairs adjacent lines with an equal start into original + translation
(`YrcParser` additionally requires the follower to be syllable-free, since yrc lines may overlap);
this is also how a provider hands over two separate documents as one blob.

Text is sourced by `LyricsRepositoryImpl` in order: a sidecar file next to the audio
(`<name>.lrc`, then `.ttml`, then `.xml`) → embedded ID3 `lyrics` via mp3agic (**MP3 only**) → the
`lyrics_cache` row for the track's `LyricsQuery.cacheKey` → online providers, in the order given by
`lyricsProviderOrder` and only when `onlineLyricsEnabled`. `LyricsSource.ONLINE` covers the cached
and the freshly downloaded case alike. `refreshLyrics` (the lyrics screen's fetch button) deletes the
`lyrics_cache` row first and then runs the same order, so it bypasses a downloaded copy and a
remembered miss but still loses to a sidecar file or an embedded tag.

A source only counts when it yields **timed** lines. An embedded USLT frame — unsynchronised text,
which mp3agic happily returns — is not an answer, so that track still reaches the network; the same
goes for an `.lrc` with no timestamps. A lookup that finds nothing anywhere writes a blank
`lyrics_cache` row, and a blank row suppresses repeat lookups for `SETTLED_TTL_MS` (7 days) so a
lyric-less track does not cost two HTTP round trips on every play.

Cache rows do not all last the same. A row the player can follow **word by word** — one whose parsed
lines contain a `KaraokeLine` — is kept the full `SETTLED_TTL_MS`, because no provider can beat it.
A row that is only **line-level** is kept `LINE_LEVEL_TTL_MS` (1 day) instead: providers list the
same recording many times over and the entry closest in length does not always carry the
word-by-word document, so re-looking has a real chance of upgrading it. That short TTL is also the
only way a row written before the provider learned to look further ever gets replaced.

A cache row remembers *which* provider answered but not *whether a higher-priority provider has
been asked*, so adding a provider (or changing the default order) would leave every already-played
track on the old provider until its TTL ran out — the new source would effectively not exist for
them. `LyricsCacheGenerationStore` (DataStore `lyrics_cache_meta`) holds a generation counter;
`LyricsRepositoryImpl.ensureCacheGeneration()` clears `lyrics_cache` once when the stored value is
behind `LYRICS_CACHE_GENERATION` (bump that constant whenever the provider set or default order
changes). User-written lyrics live in `lyrics_override` and are untouched; a version the user picked
by hand is a `lyrics_cache` row and is cleared along with the rest, which is the accepted cost of
re-resolving with the current sources.

Providers are `OnlineLyricsProvider` implementations under `data/api/lyrics/`, bound as a Hilt
`Set` by `LyricsModule`: `AmllLyricsProvider` (`/v1/lyrics/search`, then `/v1/lyrics/get?id=`, both on
`https://api.amll.dev`), `NetEaseLyricsProvider` (search, then `/api/song/lyric`),
`QQMusicLyricsProvider` (`client_search_cp`, then `fcg_query_lyric_new.fcg`),
`KugouLyricsProvider` (`krcs.kugou.com/search`, then `lyrics.kugou.com/download`) and
`LrclibLyricsProvider` (`/api/get`, falling back to `/api/search`). All of them are handed a
`LyricsQuery` — title, artist, album, duration — which is why `LyricsRepository.getLyrics` takes a
whole `Song` rather than a path. A provider that returns nothing, throws, or returns content the
parser cannot read falls through to the next in the list; only a successful, parseable result is
cached, under `title|artist|duration-in-seconds`. Unsynced `plainLyrics` and instrumentals are
deliberately rejected (the player can only render a `SyncedLyrics`). NetEase's lyric endpoint returns
up to four documents — `lrc`, word-by-word `yrc`, and a translation for each (`tlyric`, `ytlrc`) —
which `mergeNetEaseLyrics` interleaves into one blob; `yrc`/`ytlrc` win when the track has them, so
most tracks get a timed translation and some also get word-by-word highlighting.

QQ Music and Kugou are **line-level only** and are the same Chinese catalogues the cover providers
already use, but on different hosts. Both are unofficial web endpoints: Kugou's search takes the
keywords and answers with uploaded lyric documents (`id` + `accesskey`), and the download serves a
base64-encoded LRC; QQ's search is the `client_search_cp` call whose DTO the cover provider already
has (extended with `songmid`), and `fcg_query_lyric_new.fcg` serves a plain or base64 LRC plus an
optional translation LRC on the same timestamps. That translation is interleaved with
`mergeQqTranslation` rather than concatenated, because the LRC parser pairs adjacent equal-start
lines into original + translation. Both need the duration tolerance to separate covers from the real
recording, since the keyword search returns whatever matches the words. Their word-by-word formats
(KRC / QRC) are encrypted containers and are not implemented — a second-best document does not earn
its own parser, and line-level rows are re-checked the next day anyway.

AMLL is the community-curated TTML DB behind the Apple-Music-like lyric players: `/v1/lyrics/search`
never carries the lyric body, so the provider finds a document by **title only** and then verifies
the artist locally before downloading it by id. The title-only search is deliberate: the API's own
`artistName` parameter is unreliable — `musicName=Lemon&artistName=米津玄師` answers empty while
`artistName=米津` finds the document whose `artistNames` is exactly that name, so a server-side AND
silently drops lyrics the library has. `selectAmllCandidates` therefore splits both sides on
collaboration separators (`feat.`, `&`, `、` …), strips punctuation and matches names, and drops any
submission that does not name the local artist — a common title is shared by many recordings, and
another artist's lyrics are worse than none. The document is plain Apple Music-style TTML, so
`TTMLParser` reads it as-is — word by word, with `x-translation` and `x-bg` when the contributors
supplied them. Two consequences: no new parser was needed, and no duration matching is possible,
because AMLL's `duration` is the last lyric timestamp rather than the track length — comparing it to
MediaStore's duration would reject correctly-timed documents for any song with an instrumental
outro. `rankAmllCandidates` therefore only *orders* submissions by album (same release first) and
never filters on album; unconditional album filtering would drop a single whose album tag differs
from the album it also appears on. Playback downloads only the top submission; the version picker
lists up to `MAX_LYRIC_CHOICES`, labelled with the contributors' GitHub usernames, which is what
separates two timings of the same recording.

NetEase publishes one recording many times over — an album cut, a compilation, a remaster — and the
uploads do not all carry `yrc`, so resolving a single song id by length lands on an entry with only
`lrc` as often as not. `candidatesByDurationDistance` therefore returns every entry within
`DURATION_TOLERANCE_MS` ordered by how close it is (stable, so ties keep NetEase's ranking), and the
provider walks up to `MAX_LYRIC_LOOKUPS` of them, returning the first that answers word by word and
keeping the first line-level answer as a fallback. The closest entry is usually also the one with
`yrc`, so the common case is still one request. `bestDurationMatch` is that list's `firstOrNull()`,
and is what LRCLIB — which has no word-by-word documents at all — still uses.

### Library scanning: why it no longer eats user data

Recent work fixed rescans wiping playlists, favourites and play counts. The fix rests on
**exactly one write path** — `data/.../local/scanner/SongLibraryWriter.kt` — which both
`MusicScanner` and `SongRepositoryImpl.refreshSongs` route through, inside one transaction:

1. A **hand-written** `INSERT … ON CONFLICT(id) DO UPDATE SET` (`SongDao.upsertScannedSong`)
   whose `DO UPDATE SET` omits every user-owned column: `isFavorite`, `playCount`, `lastPlayedAt`,
   `rating`, `customAlbumArt`, `dateAdded`.
2. Pruning only files that genuinely disappeared, in chunks of 500 (Android 11's SQLite caps
   `IN (…)` at 999 variables).

Two rules follow. Never reintroduce `deleteAllSongs()` into the scan path — `playlist_songs.songId`
has `ON DELETE CASCADE`, so a delete-all reinsert silently empties playlists and resets user
columns. And never replace the raw upsert with `@Insert(REPLACE)`/`@Upsert`: INSERT OR REPLACE
deletes-then-inserts, which fires that same cascade. `SongLibraryWriterTest` and
`DatabaseMigrationTest` (instrumented) lock both behaviours in.

### Persistence map

`AppDatabase` is at `version = 2`, `exportSchema = true`, with the generated schema checked in at
`data/schemas/com.cycling.data.local.database.AppDatabase/2.json`. Migration is one hand-written
`MIGRATION_1_2` plus `fallbackToDestructiveMigrationOnDowngrade()` — destructive **on downgrade
only**. There is no `1.json` because v1 shipped with schema export off, so `DatabaseMigrationTest`
drives `MIGRATION_1_2` directly.

Seven entities: `songs`, `playlists`, `playlist_songs`, `song_state` (resume position, lyrics
offset), `lyrics_cache`, `ai_info_cache`, `search_history`. There are **no `@TypeConverter`s and
no FTS** — every column is a primitive and search is plain `LIKE`. Upserts for hot single columns
(`SongStateDao`) are hand-written for the same reason as the scan path.

Not everything lives in Room. **Albums and artists are read live from MediaStore** with an
in-memory cache in `AlbumRepositoryImpl`/`ArtistRepositoryImpl`, so they cannot be observed as
Flows and go stale until process restart. AI blurbs are cached in `ai_info_cache` with **no TTL**
(a hit is served forever). Writes are spread across five separate DataStore files:
`player_state`, `ai_settings` (API key), `sonic_settings` (theme, playback speed, skip-silence,
audio effects, sleep timer, lyrics providers), `songs_preferences` (list/grid, sort order), and
`excluded_folders`.

`SettingsRepository` deliberately exposes **one `Flow` per setting rather than a
`Flow<AppSettings>` snapshot** — a single snapshot would re-emit the equalizer state on every
theme change and recompose every screen. Don't "simplify" it into one. Audio effects are stored as
flat scalar keys because `:domain` has no serialization plugin, so the one list (EQ band levels)
is a comma-separated string; every read clamps or falls back, because a bad preference must never
reach ExoPlayer. Watch the band-count assumption: it is device-dependent.

## Testing

JUnit 4 + MockK, `runTest { advanceUntilIdle() }`, backticked behaviour-describing method names,
test trees mirroring `main`. `TestDispatcherRule` is **duplicated** in
`data/src/test/java/com/cycling/data/util/` and `presentation/src/test/java/com/cycling/presentation/util/`
because fixtures don't cross modules — the same reason `SongFixture` (in `:domain`) is not reused
by the presentation tests, which build songs with private inline helpers. `FakePlaybackEngine`
stands in for ExoPlayer in `PlayerManagerTest` and deliberately does not model shuffle ordering.

Coverage is uneven: `:app` has **no tests at all**, `:domain` and `:presentation` have no
instrumented tests, and only parser/player/repository/ViewModel layers are covered. Turbine is on
the test classpath but unused — assert Flows by collecting inside `runTest`.

## Gotchas

- The scanner skips audio under 30 seconds (`MediaStoreHelper.MIN_DURATION`).
- The foreground service has two independent requirements, and missing either gives
  `ForegroundServiceDidNotStartInTimeException` five seconds after any
  `startForegroundService()`: `PlayerService.onCreate` must call `addSession(mediaSession)` (the
  base class otherwise never learns the session exists, because this app never binds a
  `MediaController`), and `PlayerService.start()` must only be called on the way into `play()`
  (media3 only promotes an *engaged* player). That second rule is why every path funnels through
  `PlaybackServiceController.start()` and why a cold launch with nothing to play starts no service.
- **Album art is not a file, and media3's stock loader cannot read it.** `Song.albumArt` is a
  `MediaStore.Audio.Albums` row (`content://media/external/audio/albums/<albumId>`); opening one
  throws `SQLiteException: no such column: _data` out of MediaProvider on Android 10+, so
  `MediaSession`'s default `BitmapLoader` fails and the notification and system media control came
  up blank. `MediaStoreBitmapLoader` fixes it by going through `ContentResolver.loadThumbnail()` on
  a track of the album, and is installed with `MediaSession.Builder.setBitmapLoader`. Note SystemUI
  still resolves that URI itself and logs `MediaDataManager: Unable to load bitmap`; the cover
  renders anyway (verified on device), so treat the log line as noise, not a regression.
  `customAlbumArt` exists in the schema but no UI writes it, so in practice `albumArt` is always
  the MediaStore URI — don't assume a user-settable override is in play.
- **Cover-derived colours go through `CoverArtworkLoader`, not Coil.** `rememberArtworkBackdrop`
  (which feeds only `LocalSonBackdropColors` — the player/lyrics dynamic backdrop; the app theme
  itself follows the system wallpaper) used to load the cover with `SingletonImageLoader.execute`;
  that never produced a colour on device — Coil's memory
  cache may hand back a hardware bitmap (`Palette` throws on those) and the legacy albums row is not
  readable with `openInputStream` at all. The loader now mirrors the notification/widget path
  (album → a track of it → `ContentResolver.loadThumbnail`, then image stream, then embedded art)
  and `ArtColorExtractor` takes `androidx.palette`'s vibrant/muted/dominant profiles **without a
  chroma gate** — a chroma gate empties the list for pale covers, and the backdrop then falls back
  to the scheme containers, which look brand-purple rather than like the cover. Keep
  `SonTonalScheme.GRAYSCALE_CHROMA_THRESHOLD` below `ArtColorExtractor`'s `MIN_SEED_CHROMA` (12):
  the extractor lifts a muted seed's chroma to 12, so a higher threshold would send every pale cover
  back to the brand hue. `rememberArtworkBackdrop` logs `backdrop=#......` at INFO — read
  that line first when the page does not look like its cover.

- `Song.albumArt` is the *effective* artwork (user cover if set, else MediaStore's);
  `hasCustomAlbumArt` says which.
- `Screen.Library` renders `MyMusicScreen`. `SettingsUiState/Intent/Effect` live inside
  `SettingsViewModel.kt` with no `SettingsContract.kt`; `AiInfoScreen` creates its own ViewModel
  internally. The contract-file convention is not universal — check before assuming a file exists.
- Icons come exclusively from Material Symbols Rounded through `M3SymbolIcon` in
  `core/ui/components` (`filled = true` for emphasis/selected, `false` for rest); codepoints live in
  `SymbolIconName`. The `material-icons-extended` dependency and every `Icons.*` call are gone, and
  the font is a subset built by `scripts/subset-fonts.py` — add the codepoint there when adding an
  icon. The hand-drawn `:presentation` vector drawables and the `ic_spotify_*` app drawables were
  deleted during the Material 3 Expressive migration; the only remaining drawables are the launcher
  icon (`ic_launcher_background` / `ic_launcher_foreground`).
- Latin type is Roboto Flex (static regular/medium/bold subsets built by `scripts/subset-fonts.py`);
  CJK falls back to the system font, by ADR-0002. `SonFontFamily.Brand`/`Plain` are the only
  families screens may use.
