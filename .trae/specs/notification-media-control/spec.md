# 通知栏和锁屏播放控制 Spec

## Why
当前播放器通知只显示歌曲信息，缺少播放控制按钮，用户无法在通知栏或锁屏界面控制播放。需要增强通知功能，提供完整的播放控制体验。

## What Changes
- 在通知栏添加播放控制按钮（上一首、播放/暂停、下一首）
- 在锁屏界面显示播放控制和专辑封面
- 优化通知样式和交互体验

## Impact
- Affected code:
  - `data/src/main/java/com/cycling/data/player/PlayerService.kt` - 增强通知功能
  - `data/src/main/java/com/cycling/data/player/PlayerManager.kt` - 可能需要添加媒体按钮接收处理
  - `app/src/main/AndroidManifest.xml` - 可能需要添加媒体按钮接收器

## ADDED Requirements

### Requirement: 通知栏播放控制
系统应在通知栏提供播放控制按钮。

#### Scenario: 显示播放控制按钮
- **WHEN** 音乐正在播放或暂停
- **THEN** 通知栏显示上一首、播放/暂停、下一首按钮

#### Scenario: 点击播放/暂停
- **WHEN** 用户点击通知栏的播放/暂停按钮
- **THEN** 切换播放状态

#### Scenario: 点击上一首
- **WHEN** 用户点击通知栏的上一首按钮
- **THEN** 播放上一首歌曲

#### Scenario: 点击下一首
- **WHEN** 用户点击通知栏的下一首按钮
- **THEN** 播放下一首歌曲

### Requirement: 锁屏播放控制
系统应在锁屏界面显示播放控制。

#### Scenario: 锁屏显示播放控制
- **WHEN** 设备锁屏且音乐正在播放
- **THEN** 锁屏界面显示播放控制界面

#### Scenario: 锁屏显示专辑封面
- **WHEN** 设备锁屏且音乐正在播放
- **THEN** 锁屏界面显示当前歌曲的专辑封面（大图）

#### Scenario: 锁屏播放控制
- **WHEN** 用户在锁屏界面点击播放控制按钮
- **THEN** 执行对应的播放操作

### Requirement: 通知专辑封面
系统应在通知中显示专辑封面。

#### Scenario: 显示专辑封面
- **WHEN** 当前歌曲有专辑封面
- **THEN** 通知显示专辑封面图片

#### Scenario: 无专辑封面
- **WHEN** 当前歌曲没有专辑封面
- **THEN** 通知显示默认音乐图标
