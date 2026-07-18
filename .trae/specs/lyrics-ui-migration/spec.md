# 歌词 UI 组件迁移 Spec

## Why
将参考项目 `lyrics` 目录中的歌词 UI 组件迁移到 Sonic 项目的 presentation 模块中，配合之前迁移的歌词解析模块，为播放器提供完整的歌词显示功能。

## What Changes
- 迁移歌词 UI 组件（KaraokeLyricsView、KaraokeLineText 等）到 presentation 模块
- 迁移歌词 UI 工具类（easing、modifier、utils）到 presentation 模块
- 修改包名为 `com.cycling.presentation.lyrics`
- **BREAKING**: 移除对 `com.mocharealm.gaze.capsule.ContinuousRoundedRectangle` 的依赖，使用标准圆角替代

## Impact
- Affected specs: 歌词功能
- Affected code: presentation 模块新增 lyrics 包

## ADDED Requirements

### Requirement: 歌词 UI 组件
系统应提供歌词显示 UI 组件，支持卡拉OK效果。

#### Scenario: 显示卡拉OK歌词
- **WHEN** 播放器播放带有时间同步歌词的歌曲
- **THEN** 系统显示带有逐字高亮效果的歌词

#### Scenario: 显示同步歌词
- **WHEN** 播放器播放带有行级时间同步歌词的歌曲
- **THEN** 系统显示带有行级高亮效果的歌词

#### Scenario: 显示呼吸点动画
- **WHEN** 歌曲进入间奏或前奏部分
- **THEN** 系统显示呼吸点动画指示进度

#### Scenario: 对唱歌词显示
- **WHEN** 歌词包含对唱信息（v1/v2）
- **THEN** 系统分别左右对齐显示不同歌手的歌词

### Requirement: 歌词交互
系统应支持歌词交互功能。

#### Scenario: 点击歌词跳转
- **WHEN** 用户点击某一行歌词
- **THEN** 播放器跳转到该行歌词对应的时间点

#### Scenario: 长按歌词
- **WHEN** 用户长按某一行歌词
- **THEN** 系统触发分享或菜单操作

## MODIFIED Requirements

### Requirement: 外部依赖处理
移除对 `com.mocharealm.gaze.capsule.ContinuousRoundedRectangle` 的依赖，使用 Compose 标准 `RoundedCornerShape` 替代。
