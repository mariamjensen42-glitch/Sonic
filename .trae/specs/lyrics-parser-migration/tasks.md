# Tasks

- [x] Task 1: 迁移歌词模型类到 data/lyrics/model
  - [x] SubTask 1.1: 迁移 ISyncedLine 接口
  - [x] SubTask 1.2: 迁移 SyncedLyrics 数据类
  - [x] SubTask 1.3: 迁移 Artist 和 Attributes 数据类
  - [x] SubTask 1.4: 迁移 SyncedLine 和 UncheckedSyncedLine
  - [x] SubTask 1.5: 迁移 KaraokeAlignment 枚举
  - [x] SubTask 1.6: 迁移 KaraokeSyllable 数据类
  - [x] SubTask 1.7: 迁移 KaraokeLine 数据类

- [x] Task 2: 迁移工具类到 data/lyrics/utils
  - [x] SubTask 2.1: 迁移 TimeUtils（时间解析和格式化）
  - [x] SubTask 2.2: 迁移 SimpleXmlParser（XML 解析器）
  - [x] SubTask 2.3: 迁移 LrcMetadataHelper（LRC 元数据处理）
  - [x] SubTask 2.4: 迁移 LyricsFormatGuesser（格式检测，移除 KRC）

- [x] Task 3: 迁移歌词解析器到 data/lyrics/parser
  - [x] SubTask 3.1: 迁移 ILyricsParser 接口
  - [x] SubTask 3.2: 迁移 LrcParser
  - [x] SubTask 3.3: 迁移 EnhancedLrcParser
  - [x] SubTask 3.4: 迁移 TTMLParser
  - [x] SubTask 3.5: 迁移 LyricifySyllableParser
  - [x] SubTask 3.6: 迁移 AutoParser（移除 KRC 注册）

- [x] Task 4: 迁移歌词导出器到 data/lyrics/exporter
  - [x] SubTask 4.1: 迁移 ILyricsExporter 接口
  - [x] SubTask 4.2: 迁移 LrcExporter
  - [x] SubTask 4.3: 迁移 TTMLExporter

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1, Task 2]
- [Task 4] depends on [Task 1, Task 2]
