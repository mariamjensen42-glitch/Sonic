# Songs 页面 Spec

## Why
需要一个完整的歌曲列表页面，支持用户以列表或网格方式浏览所有歌曲，并支持多种排序方式。

## What Changes
- 创建 Songs 页面，支持列表和网格两种显示模式
- 支持多种排序方式：标题、艺术家、专辑、时长、添加日期
- 使用已有的 SongListItem 组件显示歌曲
- 添加视图切换和排序选择功能

## Impact
- Affected code:
  - `presentation/songs/SongsContract.kt` - UI 状态定义
  - `presentation/songs/SongsViewModel.kt` - ViewModel
  - `presentation/songs/SongsScreen.kt` - 页面 UI
  - `presentation/navigation/AppNavGraph.kt` - 导航配置
  - `domain/repository/SongRepository.kt` - 可能需要添加排序方法

## ADDED Requirements

### Requirement: 歌曲列表显示
系统应显示所有歌曲，支持列表和网格两种视图模式。

#### Scenario: 列表视图
- **WHEN** 用户选择列表视图
- **THEN** 以列表形式显示歌曲，每行一首歌曲，显示封面、标题、艺术家、时长

#### Scenario: 网格视图
- **WHEN** 用户选择网格视图
- **THEN** 以网格形式显示歌曲，每个单元格显示封面和标题

### Requirement: 视图模式切换
系统应允许用户在列表和网格视图之间切换。

#### Scenario: 切换视图
- **WHEN** 用户点击视图切换按钮
- **THEN** 切换到另一种视图模式
- **AND** 保存用户偏好

### Requirement: 排序功能
系统应支持多种排序方式。

#### Scenario: 按标题排序
- **WHEN** 用户选择"按标题排序"
- **THEN** 歌曲按标题字母顺序排列

#### Scenario: 按艺术家排序
- **WHEN** 用户选择"按艺术家排序"
- **THEN** 歌曲按艺术家名称排列

#### Scenario: 按专辑排序
- **WHEN** 用户选择"按专辑排序"
- **THEN** 歌曲按专辑名称排列

#### Scenario: 按时长排序
- **WHEN** 用户选择"按时长排序"
- **THEN** 歌曲按时长从短到长排列

#### Scenario: 按添加日期排序
- **WHEN** 用户选择"按添加日期排序"
- **THEN** 歌曲按添加日期从新到旧排列

### Requirement: 排序方向
系统应支持升序和降序排序。

#### Scenario: 切换排序方向
- **WHEN** 用户再次选择当前排序方式
- **THEN** 切换升序/降序方向
