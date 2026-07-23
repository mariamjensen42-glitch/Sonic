# Phase 2 Context: Components Migration

## Key Findings

### Shared Components (MIGR-03)
- PieChart.kt, PlaylistPickerDialog.kt, SongComponents.kt already exist in current project
- Minor adaptations needed: `com.cycling.*` → `com.cycling.sonic.*` package mapping, Android resources → CMP Res.*
- These are close to complete — verify and fix any remaining Android-specific API usage

### Lyrics Components (MIGR-04)
- All 6 component files exist but are **simplified stubs** (current ~30KB total vs backup ~65KB)
- Backup has full implementations with Canvas-based syllable rendering, gradient brushes, animations
- Key files to backfill:

| File | Current | Backup | Delta |
|------|---------|--------|-------|
| KaraokeLineText.kt | 2.8KB (skeleton) | 17KB (full karaoke rendering) | +14KB |
| KaraokeLyricsView.kt | 14KB | 22.7KB | +8.5KB |
| LyricsLayoutCalculator.kt | 5.8KB | 13.4KB | +7.6KB |
| KaraokeBreathingDots.kt | 4.6KB | 8.2KB | +3.6KB |
| LyricsLineItem.kt | 1.5KB | 2.8KB | +1.3KB |
| SyncedLineText.kt | 0.9KB | 1.4KB | +0.5KB |

### Backup-Only Utilities
- `LyricsLayoutCalculator.kt` backup uses `CubicBezierEasing.kt` and animation objects (Bounce, DipAndRise, Swell)
- These may already exist in current project — verify before assuming gaps

### Adaptations Needed
- Package: `com.cycling.presentation.lyrics.components.*` → `com.cycling.sonic.presentation.lyrics.components.*`
- Domain model: `com.cycling.domain.lyrics.*` → `com.cycling.sonic.domain.lyrics.*`
- Utils: `com.cycling.presentation.lyrics.utils.*` → `com.cycling.sonic.presentation.lyrics.utils.*`
- Remove Android-only APIs (if any)
- Keep CMP-compatible rendering (Canvas, Brush, etc. are already in Compose Multiplatform)

## Decision: Merge Strategy
- **Prefer merge over replace**: Integrate backup logic into current stubs to preserve any KMP adaptations already made
- **Import mapping first**: Fix package names, then fill in missing logic blocks
- **Compile-check after each file**: Ensure Kotlin KMP compatibility
