---
phase: 02-components-migration
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/KaraokeBreathingDots.kt
  - shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/KaraokeLineText.kt
  - shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/KaraokeLyricsView.kt
  - shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/LyricsLayoutCalculator.kt
  - shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/LyricsLineItem.kt
  - shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/SyncedLineText.kt
autonomous: true
requirements:
  - MIGR-04a
  - MIGR-04b
user_setup: []

must_haves:
  truths:
    - KaraokeLineText renders Canvas-based syllable highlighting with gradient progress brushes
    - KaraokeLyricsView displays full-screen synchronized karaoke lyrics with scrolling
    - KaraokeBreathingDots shows animated breathing indicator during lyric pauses
    - LyricsLayoutCalculator correctly measures and wraps syllable layouts
    - LyricsLineItem renders per-line text with accompaniment and active states
    - SyncedLineText renders synchronized line text with proper timing
    - All components compile on JVM target (shared module)
  artifacts:
    - All 6 lyrics component files updated in commonMain
  key_links:
    - LyricsScreen.kt ↔ KaraokeLyricsView for full-screen lyrics display
    - PlayerViewModel → GetLyricsUseCase → SyncedLineText rendering
    - KaraokeLineText ↔ KaraokeSyllable/KaraokeLine domain models

---

<objective>
Backfill lyrics UI components from backup to their full implementations.

The current shared/src/.../presentation/lyrics/components/ files are simplified stubs. The backup at backup/presentation/.../lyrics/components/ has the full implementations with Canvas-based karaoke rendering, progress gradient brushes, syllable animations, and proper layout calculation.

**Adaptation mapping:**
- Package: `com.cycling.presentation.lyrics.components.*` → `com.cycling.sonic.presentation.lyrics.components.*`
- Domain imports: `com.cycling.domain.lyrics.*` → `com.cycling.sonic.domain.lyrics.*`
- Utils imports: `com.cycling.presentation.lyrics.utils.*` → `com.cycling.sonic.presentation.lyrics.utils.*`
- All imported utilities (LayerPaint, Bounce, DipAndRise, Swell, isPunctuation, isRtl) already exist in current project
- No Android-only APIs to remove (Canvas, Brush, Paint are Compose Multiplatform compatible)
</objective>

<execution_context>
@.planning/codebase/STRUCTURE.md
@.planning/codebase/CONVENTIONS.md
@.planning/phases/02/02-CONTEXT.md
@shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/utils/Brush.kt
@shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/utils/easing/Bounce.kt
@shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/utils/String.kt
</execution_context>

<tasks>

<task type="tracer">
  <name>Task 1: Backfill karaoke lyrics components</name>
  <files>
    shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/KaraokeLineText.kt
    shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/KaraokeBreathingDots.kt
    shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/KaraokeLyricsView.kt
    shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/LyricsLayoutCalculator.kt
  </files>
  <action>
    Backfill each file from its backup counterpart, applying these adaptations:

    **KaraokeLineText.kt:**
    - Source: `backup/presentation/src/main/java/com/cycling/presentation/lyrics/components/KaraokeLineText.kt`
    - Replace current stub (2.8KB) with full backup implementation (17KB)
    - Package: `com.cycling.sonic.presentation.lyrics.components`
    - Import mapping:
      - `com.cycling.domain.lyrics.model.karaoke.Karaoke*` → `com.cycling.sonic.domain.lyrics.model.karaoke.Karaoke*`
      - `com.cycling.presentation.lyrics.utils.*` → `com.cycling.sonic.presentation.lyrics.utils.*`
      - `com.cycling.presentation.lyrics.utils.easing.*` → `com.cycling.sonic.presentation.lyrics.utils.easing.*`
    - Keep the current stub's package declaration and import structure; add the Canvas rendering, gradient brush, `drawLyricsLine`, `drawRowText`, and syllable animation logic from backup
    - The backup's `createLineGradientBrush`, `drawLyricsLine`, `drawRowText` functions and their animation logic (DipAndRise, Swell, Bounce, char-by-char rendering) must all be preserved

    **KaraokeBreathingDots.kt:**
    - Source: `backup/presentation/src/main/java/com/cycling/presentation/lyrics/components/KaraokeBreathingDots.kt`
    - Apply same import/package mapping
    - The backup has ~8.2KB with wave-dot animation; current has ~4.6KB

    **KaraokeLyricsView.kt:**
    - Source: `backup/presentation/src/main/java/com/cycling/presentation/lyrics/components/KaraokeLyricsView.kt`
    - Apply same import/package mapping
    - The backup has ~22.7KB with full scrolling, synchronized display; current has ~14KB

    **LyricsLayoutCalculator.kt:**
    - Source: `backup/presentation/src/main/java/com/cycling/presentation/lyrics/components/LyricsLayoutCalculator.kt`
    - Apply same import/package mapping
    - The backup has ~13.4KB with syllable measurement and line wrapping; current has ~5.8KB

    **Important:**
    - Do NOT delete or simplify the backup's Canvas-based rendering logic
    - Do NOT add Android-only imports (Context, Toast, R, etc.)
    - Keep `collectAsState()` not `collectAsStateWithLifecycle()` if any state collection exists
    - All referenced utility types (Bounce, DipAndRise, Swell objects, LayerPaint, isPunctuation, isRtl) already exist in the current project under `com.cycling.sonic.presentation.lyrics.utils.*`
    - After each file is updated, run `./gradlew :shared:compileKotlinJvm` to verify compilation
  </action>
  <verify>
    <automated>./gradlew :shared:compileKotlinJvm --no-daemon 2>&1</automated>
  </verify>
  <done>
    - KaraokeLineText.kt: full Canvas syllable rendering, gradient brushes, char animations
    - KaraokeBreathingDots.kt: full animated breathing dots
    - KaraokeLyricsView.kt: full scrolling synchronized lyrics display
    - LyricsLayoutCalculator.kt: full syllable measurement and balanced line wrapping
    - All files compile on JVM target
  </done>
</task>

<task type="auto">
  <name>Task 2: Backfill basic lyrics components</name>
  <files>
    shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/LyricsLineItem.kt
    shared/src/commonMain/kotlin/com/cycling/sonic/presentation/lyrics/components/SyncedLineText.kt
  </files>
  <action>
    Backfill each file from its backup counterpart:

    **LyricsLineItem.kt:**
    - Source: `backup/presentation/src/main/java/com/cycling/presentation/lyrics/components/LyricsLineItem.kt`
    - Package/import mapping same as Task 1
    - Current: ~1.5KB, Backup: ~2.8KB — adds accompaniment line styling, active/inactive states

    **SyncedLineText.kt:**
    - Source: `backup/presentation/src/main/java/com/cycling/presentation/lyrics/components/SyncedLineText.kt`
    - Package/import mapping same as Task 1
    - Current: ~0.9KB, Backup: ~1.4KB — adds proper timing-aware text display

    Same anti-patterns as Task 1.
  </action>
  <verify>
    <automated>./gradlew :shared:compileKotlinJvm --no-daemon 2>&1</automated>
  </verify>
  <done>
    - LyricsLineItem.kt: accompaniment styling, active/inactive line states
    - SyncedLineText.kt: timing-aware synced text display
    - All files compile on JVM target
  </done>
</task>

</tasks>

<threat_model>
## STRIDE Threat Register
| Threat ID | Category | Component | Severity | Disposition | Mitigation |
|-----------|----------|-----------|----------|-------------|------------|
| T-02-01 | Tampering | Canvas rendering | low | accept | Canvas drawing code from backup — no new dependencies |
| T-02-SC | Tampering | Compose ML kit deps | low | mitigate | All deps already in catalog |
</threat_model>

<verification>
1. `./gradlew :shared:compileKotlinJvm` passes
2. Quick audit: verify no Android-specific imports in any of the 6 files
</verification>

<success_criteria>
- [ ] All 6 lyrics component files updated with full implementations from backup
- [ ] `./gradlew :shared:compileKotlinJvm` succeeds
- [ ] No Android-specific APIs leaked into commonMain files
</success_criteria>

<output>
Create `.planning/phases/02/02-01-SUMMARY.md` when done
</output>
