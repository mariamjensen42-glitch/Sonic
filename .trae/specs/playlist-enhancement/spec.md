# 播放列表增强功能 Spec

## Why
当前播放列表功能不完整，用户无法从歌曲列表添加歌曲到播放列表，无法在详情页移除歌曲，也无法重命名播放列表。需要增强这些核心功能以提供完整的播放列表管理体验。

## What Changes
- 在歌曲列表页添加"添加到播放列表"功能
- 在播放列表详情页添加移除歌曲的 UI
- 在播放列表列表页添加重命名功能
- 创建播放列表选择弹窗组件

## Impact
- Affected code:
  - `presentation/songs/SongsScreen.kt` - 添加歌曲操作菜单
  - `presentation/songs/SongsContract.kt` - 添加相关 Intent 和 Effect
  - `presentation/songs/SongsViewModel.kt` - 处理添加到播放列表逻辑
  - `presentation/playlists/PlaylistsScreen.kt` - 添加重命名 UI
  - `presentation/playlists/PlaylistsContract.kt` - 添加重命名 Intent
  - `presentation/playlists/PlaylistsViewModel.kt` - 处理重命名逻辑
  - `presentation/playlistdetail/PlaylistDetailScreen.kt` - 添加移除歌曲 UI
  - `presentation/components/PlaylistPickerDialog.kt` - 新建播放列表选择弹窗组件

## ADDED Requirements

### Requirement: 添加歌曲到播放列表
系统应允许用户从歌曲列表将歌曲添加到播放列表。

#### Scenario: 显示添加选项
- **WHEN** 用户点击歌曲项的更多按钮
- **THEN** 显示操作菜单，包含"添加到播放列表"选项

#### Scenario: 选择播放列表
- **WHEN** 用户点击"添加到播放列表"
- **THEN** 显示播放列表选择弹窗
- **AND** 弹窗列出所有播放列表
- **AND** 提供"新建播放列表"选项

#### Scenario: 添加成功
- **WHEN** 用户选择一个播放列表
- **THEN** 歌曲被添加到该播放列表
- **AND** 显示成功提示

#### Scenario: 新建播放列表并添加
- **WHEN** 用户在弹窗中点击"新建播放列表"并输入名称
- **THEN** 创建新播放列表并将歌曲添加到其中
- **AND** 显示成功提示

### Requirement: 播放列表重命名
系统应允许用户重命名播放列表。

#### Scenario: 显示重命名选项
- **WHEN** 用户点击播放列表项的更多按钮
- **THEN** 显示操作菜单，包含"重命名"选项

#### Scenario: 重命名操作
- **WHEN** 用户点击"重命名"并输入新名称
- **THEN** 播放列表名称更新
- **AND** 显示成功提示

### Requirement: 从播放列表移除歌曲
系统应允许用户从播放列表详情页移除歌曲。

#### Scenario: 显示移除选项
- **WHEN** 用户在播放列表详情页点击歌曲项的更多按钮
- **THEN** 显示操作菜单，包含"从播放列表移除"选项

#### Scenario: 移除成功
- **WHEN** 用户点击"从播放列表移除"
- **THEN** 歌曲从播放列表中移除
- **AND** 列表自动更新
- **AND** 显示成功提示

## MODIFIED Requirements

### Requirement: 播放列表详情页面
原有功能基础上增加移除歌曲的 UI 交互。

#### Scenario: 移除歌曲 UI
- **WHEN** 用户在播放列表详情页查看歌曲列表
- **THEN** 每首歌曲显示更多操作按钮
- **AND** 点击后显示"从播放列表移除"选项
