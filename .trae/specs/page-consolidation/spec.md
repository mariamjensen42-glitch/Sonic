# 页面整合优化 Spec

## Why
当前项目存在多个功能相似但独立成页面的情况，导致：
1. 页面碎片化严重，用户需要频繁跳转
2. 代码重复度高（如 FavoritesScreen、RecentlyPlayedScreen、MostPlayedScreen 结构几乎完全相同）
3. 导航层级过深，用户体验不流畅
4. 维护成本高，每个页面都需要独立的 ViewModel、Contract、Screen 文件

## What Changes
- **合并播放统计相关页面**：将 FavoritesScreen、RecentlyPlayedScreen、MostPlayedScreen 合并为一个统一的 "我的音乐" 页面，使用 Tab 切换
- **整合歌词到播放器**：将 LyricsScreen 整合到 PlayerScreen 中，作为播放器的一个视图模式
- **整合设置相关页面**：将 ScanScreen、ExcludeFoldersScreen、ApiKeyConfigScreen、LibraryStatsScreen 整合到 SettingsScreen 中作为子页面或内嵌内容
- **简化歌曲详情与标签编辑**：将 TagEditorScreen 作为 SongDetailScreen 的编辑模式

## Impact
- Affected specs: 页面导航结构、UI/UX 设计
- Affected code:
  - `presentation/favorites/` - 将被合并
  - `presentation/recentlyplayed/` - 将被合并
  - `presentation/mostplayed/` - 将被合并
  - `presentation/lyrics/` - 将被整合到播放器
  - `presentation/scan/` - 将被整合到设置
  - `presentation/excludefolders/` - 将被整合到设置
  - `presentation/settings/` - 需要重构
  - `presentation/player/` - 需要添加歌词视图
  - `presentation/songdetail/` - 需要添加编辑模式
  - `presentation/tageditor/` - 将被整合
  - `presentation/navigation/` - 导航结构需要简化

## ADDED Requirements

### Requirement: 统一的"我的音乐"页面
系统 SHALL 提供一个统一的"我的音乐"页面，整合喜欢、最近播放、最常播放三个视图。

#### Scenario: 用户查看喜欢的歌曲
- **WHEN** 用户进入"我的音乐"页面
- **THEN** 默认显示"喜欢"标签页，展示收藏的歌曲列表
- **AND** 显示歌曲数量统计
- **AND** 提供全部播放和随机播放按钮

#### Scenario: 用户切换到最近播放
- **WHEN** 用户点击"最近播放"标签
- **THEN** 显示最近播放的歌曲列表
- **AND** 按播放时间倒序排列

#### Scenario: 用户切换到最常播放
- **WHEN** 用户点击"最常播放"标签
- **THEN** 显示播放次数最多的歌曲列表
- **AND** 显示每首歌曲的播放次数

### Requirement: 播放器内嵌歌词视图
系统 SHALL 在播放器界面提供歌词视图切换功能。

#### Scenario: 用户查看歌词
- **WHEN** 用户在播放器界面点击歌词按钮
- **THEN** 播放器界面切换到歌词视图
- **AND** 显示同步滚动的歌词
- **AND** 保持播放控制功能可用

#### Scenario: 用户从歌词返回封面
- **WHEN** 用户在歌词视图点击返回按钮
- **THEN** 播放器界面切换回封面视图

### Requirement: 整合的设置页面
系统 SHALL 提供整合的设置页面，将扫描、排除文件夹、API配置、统计作为设置项内嵌展示。

#### Scenario: 用户扫描音乐
- **WHEN** 用户在设置页面点击"扫描本地音乐"
- **THEN** 在设置页面内展开扫描界面
- **AND** 显示扫描进度和结果
- **AND** 完成后可返回设置主界面

#### Scenario: 用户管理排除文件夹
- **WHEN** 用户在设置页面点击"排除文件夹"
- **THEN** 在设置页面内展开文件夹管理界面
- **AND** 可添加/删除排除的文件夹

### Requirement: 歌曲详情编辑模式
系统 SHALL 在歌曲详情页面提供编辑模式，替代独立的标签编辑页面。

#### Scenario: 用户编辑歌曲信息
- **WHEN** 用户在歌曲详情页点击编辑按钮
- **THEN** 页面切换到编辑模式
- **AND** 可编辑歌曲标题、艺术家、专辑等信息
- **AND** 提供保存和取消按钮

## MODIFIED Requirements

### Requirement: 简化的导航结构
导航结构 SHALL 移除以下独立页面入口：
- 移除独立的 Favorites、RecentlyPlayed、MostPlayed 页面导航
- 移除独立的 Lyrics 页面导航
- 移除独立的 Scan、ExcludeFolders、ApiKeyConfig、LibraryStats 页面导航
- 移除独立的 TagEditor 页面导航

首页快速访问入口 SHALL 更新为：
- "喜欢的歌曲"、"最近播放"、"最常播放" 统一导航到 "我的音乐" 页面

## REMOVED Requirements

### Requirement: 独立的收藏页面
**Reason**: 合并到统一的"我的音乐"页面
**Migration**: FavoritesScreen、FavoritesViewModel、FavoritesContract 将被移除

### Requirement: 独立的最近播放页面
**Reason**: 合并到统一的"我的音乐"页面
**Migration**: RecentlyPlayedScreen、RecentlyPlayedViewModel、RecentlyPlayedContract 将被移除

### Requirement: 独立的最常播放页面
**Reason**: 合并到统一的"我的音乐"页面
**Migration**: MostPlayedScreen、MostPlayedViewModel、MostPlayedContract 将被移除

### Requirement: 独立的歌词页面
**Reason**: 整合到播放器界面
**Migration**: LyricsScreen 将被移除，歌词组件保留并整合到 PlayerScreen

### Requirement: 独立的扫描页面
**Reason**: 整合到设置页面
**Migration**: ScanScreen、ScanViewModel、ScanContract 将被移除，扫描功能作为设置页面的子功能

### Requirement: 独立的排除文件夹页面
**Reason**: 整合到设置页面
**Migration**: ExcludeFoldersScreen、ExcludeFoldersViewModel、ExcludeFoldersContract 将被移除

### Requirement: 独立的标签编辑页面
**Reason**: 整合到歌曲详情页面
**Migration**: TagEditorScreen、TagEditorViewModel、TagEditorContract 将被移除
