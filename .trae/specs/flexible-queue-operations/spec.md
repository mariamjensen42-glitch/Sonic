# 灵活的播放队列操作 Spec

## Why
当前播放队列只支持添加到队尾，用户无法灵活调整播放顺序。需要支持"下一首播放"插队功能和拖拽排序功能，让用户能够像整理待办清单一样随心调整播放顺序。

## What Changes
- 添加"下一首播放"功能，将歌曲插入到当前播放歌曲之后
- 支持播放队列拖拽排序，用户可自由调整歌曲顺序
- 在歌曲列表的更多菜单中添加"下一首播放"选项
- 在播放队列 UI 中添加拖拽手柄和拖拽交互

## Impact
- Affected code:
  - `data/src/main/java/com/cycling/data/player/PlayerManager.kt` - 添加 playNext 和 moveQueueItem 方法
  - `domain/src/main/java/com/cycling/domain/repository/PlayerRepository.kt` - 添加接口方法
  - `data/src/main/java/com/cycling/data/repository/PlayerRepositoryImpl.kt` - 实现接口方法
  - `presentation/src/main/java/com/cycling/presentation/player/PlayerContract.kt` - 添加新 Intent
  - `presentation/src/main/java/com/cycling/presentation/player/PlayerViewModel.kt` - 处理新 Intent
  - `presentation/src/main/java/com/cycling/presentation/player/PlayerScreen.kt` - 实现拖拽排序 UI
  - `presentation/src/main/java/com/cycling/presentation/components/SongListItem.kt` - 添加"下一首播放"菜单项

## ADDED Requirements

### Requirement: 下一首播放
系统应支持将任意歌曲设为"下一首播放"，不破坏当前队列。

#### Scenario: 从歌曲列表添加下一首播放
- **WHEN** 用户在歌曲列表中点击"下一首播放"菜单项
- **THEN** 该歌曲被插入到当前播放歌曲的下一首位置，显示 Toast 提示"已设为下一首播放"

#### Scenario: 当前无播放歌曲时添加下一首播放
- **WHEN** 用户在无播放状态时点击"下一首播放"
- **THEN** 该歌曲直接开始播放

#### Scenario: 队列末尾添加下一首播放
- **WHEN** 当前播放的是队列最后一首歌曲
- **THEN** 歌曲被添加到队列末尾

### Requirement: 播放队列拖拽排序
系统应支持通过拖拽调整播放队列中歌曲的顺序。

#### Scenario: 拖拽队列项调整顺序
- **WHEN** 用户长按队列项并拖动到目标位置
- **THEN** 歌曲移动到新位置，队列顺序更新

#### Scenario: 拖拽当前播放歌曲
- **WHEN** 用户拖拽当前正在播放的歌曲
- **THEN** 歌曲移动到新位置，播放不受影响，当前播放索引更新

#### Scenario: 拖拽视觉反馈
- **WHEN** 用户开始拖拽队列项
- **THEN** 被拖拽的项有视觉高亮效果，其他项自动让位

### Requirement: 队列项拖拽手柄
系统应在队列项上显示拖拽手柄，提示用户可拖拽排序。

#### Scenario: 显示拖拽手柄
- **WHEN** 显示播放队列列表
- **THEN** 每个队列项右侧显示拖拽手柄图标

## MODIFIED Requirements
无

## REMOVED Requirements
无
