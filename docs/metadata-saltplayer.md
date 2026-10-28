# 元数据来源：Salt Player 逆向参考

反编译对象：`com.salt.music`（Salt Player，`base.apk`，dex 未混淆业务名、类名用 R8 字典重排到 `androidx.media3.*`）。
工具：jadx 1.5.6（成品源码在 `D:\workbuddy\sources`）。

## 结论

Salt **没有**自己写标签解析器。它把官方 **TagLib C++** 编进 `libsalt-tag.so`，Java 侧只有一个
`@Keep` 的 `com.moriafly.taglib.Taglib`（10 个 native 方法）和一个 12 字段的 POJO `AudioTag`。
库列表不解析文件，全靠 MediaStore；标签只在「需要真实值」时按需调 native。

## 三层来源

| 用途 | 来源 | 位置 |
| --- | --- | --- |
| 曲库列表（title/artist/album/albumArtist/year/track/duration/size/bitrate/dateAdded） | `MediaStore.Audio.Media` 一次 Cursor | `androidx.media3.C1400.mo123` case 6 |
| 技术参数、真实标题、歌词、ReplayGain、编码器、封面 | `libsalt-tag.so`（TagLib C++ + JNI） | `androidx.media3.bv4` / `MusicController` / `fe1` / `iy1` / `SongPicture` |
| 封面兜底 | `MediaMetadataRetriever.getEmbeddedPicture()` → 同目录同名 jpg → `content://media/.../albumart` | `SongPicture` |

MediaStore 查询的 selection 是 `" _data != '' AND _size > 100"`，排序无关；`album_artist`、`bitrate`
列只在 SDK ≥ 30 读，低于 30 时 bitrate 用 `8 * size / (duration/1000)` 估算。

## native 接口（`com.moriafly.taglib.Taglib`，`VERSION = v1.2.0`）

```java
public final native AudioTag read(AudioTag out, int fd, String ext);        // fd 路径
public final native void     readAudioTag(AudioTag out, String path, String ext); // 真实路径
public final native String   readTagLyrics(int fd);   // USLT / SYLT / LYRICS / WM/Lyrics
public final native String   readEncoder(int fd);     // LAME 等编码器串
public final native String   readTagString(int fd);   // 未在 v10 中调用
public final native String   getReplayGain(String path); // 注意：吃路径，不吃 fd
public final native byte[]   getFlacArtworkArtist(int fd);   // FLAC PICTURE 块
public final native byte[]   getApeArtwork(int fd);          // APE COVER ART (FRONT)
public final native byte[]   getFlacArtworkArtistByRealPath(String path);
public final native String   getNativePath(ContentResolver cr, Uri uri); // 恒返回 ""
```

全部只读：**这个 so 里没有任何写方法**，Salt v10 不写回文件标签。

`ext` 是小写扩展名，native 侧转大写后派发；so 里出现的类型枚举：
`MP3 / M4A / FLAC / OGG / OPUS / SPX / TTA / WAV / WMA / WV / APE / AIFF`。

`readAudioTag` 用 `read()`+`seek` 走真实路径；`read` 走 `ParcelFileDescriptor.detachFd()` 得到的
fd 上建 `TagLib::FileStream`，因此 `content://` 也能读，不需要 `MANAGE_EXTERNAL_STORAGE`。

## `AudioTag` 只有 12 个字段

`title / artist / album / albumArtist / year / trackNumber / discNumber / copyright /
bitrate / bitsPerSample / sampleRate / duration`

没有 genre、composer、comment、publisher。Salt 的 `Song` 实体也同样没有这些字段 —— 它本来就不做
完整标签编辑器。`quality`（Hi-Res 标记）由 `bitsPerSample` + `sampleRate` 现算。

## 读取路径

```
URL 是 file://         → Taglib.readAudioTag(tag, path, ext)       （bv4.m927）
URL 是 content://      → fd = ContentResolver.openFileDescriptor(uri,"r").detachFd()
                         Taglib.read(tag, fd, ext)                  （bv4.m928 / MusicController.m11284）
读成功后校验            → sampleRate|duration|bitrate|bitsPerSample > 0
                        或 title/artist/album 非空，否则视为失败    （bv4.m930）
```

单独的 `MusicController.m11282(path)` / `m11284(fd)` 是播放期再读一次（用于取样率 / 位深做
bit-perfect 判断），失败静默返回 null。

## 歌词 / ReplayGain / 封面

- 歌词：`Song` → Uri → fd → `readTagLyrics(fd)` 拿原始字符串 → Kotlin 侧 `he1` 用正则解析 LRC
  （BOM 剥离、`\r\n|\n|\r` 切行）。native 只负责"把帧里的字节吐出来"。
- ReplayGain：`getReplayGain(path)` 返回字符串（如 `-7.53 dB`），失败返回空串；开关默认开。
- 封面按扩展名分支：
  - `flac` → `getFlacArtworkArtist(fd)`
  - `ape` → `getApeArtwork(fd)`，返回 `[mime\0data]`，Java 侧按第一个 `\0` 切开
  - 其它 → `MediaMetadataRetriever.getEmbeddedPicture()` → 同目录 `<同名>.jpg` →
    开关打开时 `content://media/external/audio/albumart/<albumId>`
- APE / WV / DSF 这类 Media3 解不了的格式由 BASS（`libsalt-bassplayer.so`）播放，标签仍走 TagLib。

## 对 Sonic 的落地选项

现状：`:taglib`（`com.cycling.salt.audiotag`，43 个 Kotlin 文件，自己写的 MP3/FLAC/OGG/MP4 解析器）
只被 `data/local/metadata/AudioMetadataHelper.kt` 使用；曲库列表走 MediaStore，不碰它。

| 方案 | 读 | 写 | NDK | 风险 |
| --- | --- | --- | --- | --- |
| A. 抄 Salt：内置 TagLib C++ + 自写 JNI | 全格式，快，含歌词/封面/ReplayGain | 需自己再加 `TagLib::FileRef::save()` 的 JNI | 要（CMake + 四 ABI，16KB page） | 自写 JNI 层 ~300 行 C++，版权 LGPL-2.1/MPL-2.0 |
| B. `net.jthink:jaudiotagger`（纯 JVM） | MP3/FLAC/OGG/MP4/WAV/AIFF，字段全（genre/composer/comment） | 有 | 不要 | apk +2MB，慢（每首全文件扫），Opus 支持弱 |
| C. 只留 MediaStore + MediaMetadataRetriever | 基础字段 | 无 | 不要 | 拿不到歌词/lossless 参数 |

推荐 **A 读 + mp3agic 写 MP3 + jaudiotagger 或 TagLib 写其它**，或直接 **B 全包**。A 才和 Salt 行为一致；
B 是唯一不引 NDK 就能删掉 `:taglib` 的路。**不要**继续维护手写解析器。

## 落地结果（选 B）

`:taglib` 与 `mp3agic` 已删除，`net.jthink:jaudiotagger:3.0.1` 接管全部标签读写：

- `data/local/metadata/AudioMetadataHelper.kt` — 读走 `AudioFileIO.read(File)`，技术参数取
  `AudioHeader`，文字字段取 `Tag.getFirst(FieldKey.*)`；写走 `tag.setField` + `AudioFileIO.write`
  （直接在原文件重写标签块，不再有临时文件与"整文件进内存"上限）。
- `data/local/lyrics/EmbeddedLyricsHelper.kt` — `FieldKey.LYRICS`，从「只认 MP3」扩到所有可读容器。
- 覆盖：MP3 / FLAC / OGG-Vorbis / MP4-M4A / WAV / AIFF / WMA / DSF 可读写，DFF 只读。
- 退步：Opus 与裸 AAC 不被 jaudiotagger 支持，退到 `MediaMetadataRetriever` 拿基础字段且不可写。
- R8：只补一条 `-keep class org.jaudiotagger.tag.id3.framebody.**`（帧体按类名反射构造），
  外加 `java.awt` / `javax.imageio` / `javax.swing` 的 `-dontwarn`（封面解码路径不会走到）。
