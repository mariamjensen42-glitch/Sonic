# 歌词功能优化 Spec

## Why
参考示例代码，优化歌词播放位置更新机制，避免不必要的重组，提升歌词滚动流畅度。

## What Changes
- 优化 LyricsViewModel 播放位置更新方式，使用帧同步更新
- 优化 LyricsScreen 传递当前位置的方式，使用 lambda provider
- 移除不必要的 StateFlow 订阅，改用 rememberUpdatedState

## Impact
- Affected specs: 歌词功能
- Affected code: presentation/lyrics 模块

## ADDED Requirements

### Requirement: 播放位置平滑更新
系统应使用帧同步方式更新播放位置，避免歌词滚动卡顿。

#### Scenario: 播放中更新位置
- **WHEN** 歌曲正在播放
- **THEN** 系统使用 awaitFrame() 同步更新位置，每帧刷新一次

#### Scenario: 暂停时更新位置
- **WHEN** 歌曲暂停
- **THEN** 系统直接使用播放器当前位置

### Requirement: 歌词位置提供者
系统应使用 lambda 提供当前位置，避免不必要的重组。

#### Scenario: 传递位置提供者
- **WHEN** 调用 KaraokeLyricsView
- **THEN** 使用 lambda 函数提供当前位置，而不是 State 值
