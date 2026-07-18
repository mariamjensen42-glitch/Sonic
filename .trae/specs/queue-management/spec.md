# 播放队列管理增强 Spec

## Why
当前播放队列只支持查看，缺少队列管理功能。用户无法从队列中移除歌曲、清空队列或点击队列中的歌曲直接播放。需要增强播放队列的交互功能。

## What Changes
- 在队列 ModalBottomSheet 中添加移除歌曲功能
- 添加清空队列按钮
- 支持点击队列中的歌曲直接播放
- 优化队列 UI 样式，更符合 iOS 风格

## Impact
- Affected code:
  - `presentation/src/main/java/com/cycling/presentation/player/PlayerScreen.kt` - 增强队列 UI
  - `presentation/src/main/java/com/cycling/presentation/player/PlayerViewModel.kt` - 已有相关 Intent 处理
  - `presentation/src/main/java/com/cycling/presentation/player/PlayerContract.kt` - 已有相关定义

## ADDED Requirements

### Requirement: 队列歌曲移除
系统应支持从播放队列中移除歌曲。

#### Scenario: 移除队列中的歌曲
- **WHEN** 用户在队列中滑动删除或点击删除按钮
- **THEN** 该歌曲从队列中移除，队列自动更新

#### Scenario: 移除当前播放歌曲
- **WHEN** 用户移除当前正在播放的歌曲
- **THEN** 自动播放下一首歌曲

### Requirement: 队列歌曲播放
系统应支持点击队列中的歌曲直接播放。

#### Scenario: 点击队列歌曲播放
- **WHEN** 用户点击队列中的某首歌曲
- **THEN** 立即开始播放该歌曲

### Requirement: 清空队列
系统应支持一键清空播放队列。

#### Scenario: 清空队列
- **WHEN** 用户点击清空队列按钮
- **THEN** 清空所有队列中的歌曲，停止播放

### Requirement: 队列 UI 优化
系统应提供符合 iOS 风格的队列管理界面。

#### Scenario: iOS 风格队列项
- **WHEN** 显示队列列表
- **THEN** 使用圆角卡片样式，当前播放歌曲有视觉高亮

#### Scenario: 队列项交互
- **WHEN** 用户在队列项上滑动
- **THEN** 显示删除选项
