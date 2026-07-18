# 歌词解析模块迁移 Spec

## Why
将参考项目 `core` 目录中的歌词解析模块迁移到 Sonic 项目的 data 模块中，为后续实现歌词显示功能提供基础支持。同时移除酷狗歌词相关代码，保持模块精简。

## What Changes
- 迁移歌词解析器（LRC、Enhanced LRC、TTML、Lyricify Syllable）到 data 模块
- 迁移歌词模型（SyncedLyrics、KaraokeLine 等）到 data 模块
- 迁移歌词导出器（LRC、TTML）到 data 模块
- 迁移工具类（时间解析、格式检测等）到 data 模块
- **BREAKING**: 移除酷狗 KRC 格式支持
- 修改包名为 `com.cycling.data.lyrics`

## Impact
- Affected specs: 歌词功能
- Affected code: data 模块新增 lyrics 包

## ADDED Requirements

### Requirement: 歌词解析模块
系统应提供歌词解析功能，支持多种歌词格式。

#### Scenario: 解析 LRC 格式
- **WHEN** 用户传入标准 LRC 格式歌词
- **THEN** 系统返回 SyncedLyrics 对象，包含时间戳和歌词内容

#### Scenario: 解析 Enhanced LRC 格式
- **WHEN** 用户传入增强 LRC 格式歌词（包含音节级时间戳）
- **THEN** 系统返回 SyncedLyrics 对象，包含 KaraokeLine

#### Scenario: 解析 TTML 格式
- **WHEN** 用户传入 Apple TTML 格式歌词
- **THEN** 系统返回 SyncedLyrics 对象，包含完整卡拉OK信息

#### Scenario: 解析 Lyricify Syllable 格式
- **WHEN** 用户传入 Lyricify 音节格式歌词
- **THEN** 系统返回 SyncedLyrics 对象

#### Scenario: 自动检测格式
- **WHEN** 用户传入未知格式的歌词
- **THEN** 系统自动检测格式并使用对应解析器

### Requirement: 歌词导出模块
系统应提供歌词导出功能。

#### Scenario: 导出 LRC 格式
- **WHEN** 用户请求导出歌词为 LRC 格式
- **THEN** 系统返回标准 LRC 格式字符串

#### Scenario: 导出 TTML 格式
- **WHEN** 用户请求导出歌词为 TTML 格式
- **THEN** 系统返回 TTML 格式字符串

## REMOVED Requirements

### Requirement: 酷狗 KRC 格式支持
**Reason**: 用户要求移除酷狗歌词相关功能
**Migration**: 删除 KugouKrcParser 和 KugouKrcMetadataDecoder，从 AutoParser 和 LyricsFormatGuesser 中移除相关注册
