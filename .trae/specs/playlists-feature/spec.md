# Playlists 和 PlaylistDetail 功能 Spec

## Why
当前播放列表页面仅为占位符，用户无法管理播放列表，需要实现完整的播放列表功能，包括创建、删除、重命名播放列表以及管理播放列表中的歌曲。

## What Changes
- 实现 PlaylistsScreen 播放列表列表页面
- 实现 PlaylistDetailScreen 播放列表详情页面
- 创建对应的 ViewModel 和 Contract (MVI模式)
- 更新导航配置

## Impact
- Affected code:
  - `presentation/playlists/PlaylistsScreen.kt` - 新建播放列表列表页面
  - `presentation/playlists/PlaylistsViewModel.kt` - 新建播放列表ViewModel
  - `presentation/playlists/PlaylistsContract.kt` - 新建播放列表State/Intent/Effect
  - `presentation/playlistdetail/PlaylistDetailScreen.kt` - 新建播放列表详情页面
  - `presentation/playlistdetail/PlaylistDetailViewModel.kt` - 新建播放列表详情ViewModel
  - `presentation/playlistdetail/PlaylistDetailContract.kt` - 新建播放列表详情State/Intent/Effect
  - `presentation/navigation/AppNavGraph.kt` - 更新导航配置

## ADDED Requirements

### Requirement: 播放列表列表页面
系统应提供播放列表列表页面，展示所有播放列表并支持基本管理操作。

#### Scenario: 显示播放列表
- **WHEN** 用户进入播放列表页面
- **THEN** 显示所有播放列表，每个列表项显示名称和歌曲数量
- **AND** 按修改时间倒序排列

#### Scenario: 创建播放列表
- **WHEN** 用户点击创建按钮并输入名称
- **THEN** 创建新的播放列表并显示在列表中

#### Scenario: 删除播放列表
- **WHEN** 用户删除某个播放列表
- **THEN** 该播放列表从列表中移除

#### Scenario: 重命名播放列表
- **WHEN** 用户选择重命名并输入新名称
- **THEN** 播放列表名称更新为新名称

#### Scenario: 进入播放列表详情
- **WHEN** 用户点击某个播放列表
- **THEN** 导航到该播放列表的详情页面

#### Scenario: 空状态显示
- **WHEN** 没有任何播放列表
- **THEN** 显示空状态提示和创建按钮

### Requirement: 播放列表详情页面
系统应提供播放列表详情页面，展示播放列表内的歌曲并支持播放操作。

#### Scenario: 显示播放列表信息
- **WHEN** 用户进入播放列表详情页面
- **THEN** 显示播放列表名称和歌曲数量

#### Scenario: 显示歌曲列表
- **WHEN** 播放列表中有歌曲
- **THEN** 显示歌曲列表，每项显示封面、标题、时长

#### Scenario: 播放全部
- **WHEN** 用户点击"播放全部"按钮
- **THEN** 从第一首歌曲开始播放

#### Scenario: 随机播放
- **WHEN** 用户点击"随机播放"按钮
- **THEN** 随机选择一首歌曲开始播放

#### Scenario: 移除歌曲
- **WHEN** 用户从播放列表中移除某首歌曲
- **THEN** 该歌曲从播放列表中移除

#### Scenario: 空播放列表状态
- **WHEN** 播放列表中没有歌曲
- **THEN** 显示空状态提示
