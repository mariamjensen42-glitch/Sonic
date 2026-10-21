# taglib — vendored SaltAudioTag core

This module is **not hand-written Sonic code**. It is a copy of the `core` module from the local
project `D:\Programming\Kotlin\SaltAudioTag-main`, which is the author's own Kotlin Multiplatform
audio-tag library ("Salt Audio Tag", © 2025 Moriafly, LGPL-2.1 — see `LICENSE` here and the header
of every source file).

Only `:data`'s `AudioMetadataHelper` uses it: Sonic needs tag **read + write** for FLAC /
OGG-Vorbis / MP4(M4A) to complement mp3agic, which only handles MP3. The rest of Sonic should not
depend on this module (keep the dependency direction `data → taglib`).

## What was adapted when vendoring

The upstream `core` is a KMP module (`commonMain` + `androidMain`, kotlinx-io, buildkonfig).
To keep Sonic's build simple it is a plain Android library here:

1. `core/src/commonMain/kotlin/**` → `src/main/java/**` (same package `com.cycling.salt.audiotag`).
2. `core/src/androidMain/.../AudioTag.android.kt` → `src/main/java/...` (the module is Android-only).
3. `AudioTag.getVersionName()` and its `BuildKonfig` import were removed — Sonic does not need the
   library version string and does not use the buildkonfig plugin.
4. `OggPageParser.VORBIS_COMMENT_TO_METADATA` got `METADATA_BLOCK_PICTURE` / `COVERART` /
   `COVERARTMIME` passthrough entries. Ogg cover art is just a Vorbis comment; without passthrough a
   tag write would drop it. `TRACKTOTAL` / `DISCTOTAL` map to their own keys (upstream folded them
   into `TRACKNUMBER` / `DISCNUMBER`, which made the combined `3/12` unrepresentable).
5. `Mp4.kt` / `Mp4Reader.kt`: upstream wrote `trkn` / `disk` as UTF-8 text atoms, which only its own
   reader understood. The writer now emits the iTunes binary layout (`[0][0][index BE16][total
   BE16]…`, type 0) and the reader decodes that layout (while still accepting the old text form).
6. **OGG reading was broken upstream.** `OggVorbisReader` / `OggOpusReader` consumed the whole
   source with `readPages()` and then scanned it again with `readLastGranulePosition()`, which threw
   `EOFException` on every file whose last page carried the EOS flag. `readPages()` now returns
   packets **and** the last granule in one pass (`readPacketsAndLastGranule`), and `readPage()`
   treats EOF / a truncated trailing page as "no more pages".
7. `OggVorbisReader` also read `audio_channels` / `audio_sample_rate` from offsets 5/6, i.e. inside
   the `vorbis` magic instead of the fields after the version. Fixed to offsets 11/12.
8. `Mp4Writer` located `ilst` by mixing "relative to udta" and "relative to moov" offsets and never
   looked inside `meta` (a full box with a 4-byte version/flags prefix), so on real files it could
   miss the existing tags or corrupt parent box sizes. `findIlstInMoov()` now returns absolute
   offsets, follows `udta → meta → ilst`, and `rebuildMoov()` updates every ancestor box; `addIlstToMoov()`
   (files without tags) was rewritten to insert at unambiguous positions and fix moov/udta/meta sizes.
9. `core/src/commonTest/kotlin/**` → `src/test/java/**` with JUnit 4 via `kotlin-test-junit`
   (upstream runs them on the JVM/KMP test target). `Mp3ReadDebugTest` was left out because it
   points at a developer-specific path; `CdaTest` read `E:\Track03.cda` and `SystemFileSystemUtilTest.copy`
   wrote to `C:\Users\moria\1`, so both were replaced with synthetic/temp-path tests. The OGG CRC
   "known value" and one no-op expectation in the upstream tests were wrong and are corrected.
   `Mp4RoundtripTest` was added because upstream only unit-tested atom encoding, never a full
   read → write → read roundtrip.

Everything else is upstream and should be updated by re-copying, not by editing in place.
The tests under `src/test/java` are the verification for all of the above:
`.\gradlew.bat :taglib:testDebugUnitTest`.

## Format support (upstream README matrix, verified against the code)

| Format    | Streaminfo | Metadata   | Cover art | Notes                                           |
|:----------|:----------:|:----------:|:---------:|:------------------------------------------------|
| MP3       | Read       | Read/Write | Read      | ID3v2.2–2.4; writes ID3v2.4, strips ID3v1       |
| FLAC      | Read       | Read/Write | Read      | VorbisComment + PICTURE blocks                  |
| OGG/OggS  | Read       | Read/Write | —         | Vorbis; comments round-trip verbatim             |
| MP4 / M4A | Read       | Read/Write | Read      | `moov.udta.meta.ilst`                            |
| Opus      | Read       | Read       | —         | No writer                                       |
| CDA       | Read       | Read       | —         | RIFF/CDDA; track & disc info only               |

Memory note: the OGG and MP4 writers read the whole file into memory before rebuilding it; the FLAC
writer streams. `AudioMetadataHelper` therefore refuses whole-file-writer formats above
`MAX_BUFFERED_TAG_WRITE_BYTES` rather than risking an OOM, falling back to the database overrides.
