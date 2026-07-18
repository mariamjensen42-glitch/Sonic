# Tasks

## Phase 1: 创建统一的"我的音乐"页面
- [x] Task 1.1: 创建 MyMusicScreen 及其 Tab 结构
  - [x] 创建 MyMusicContract.kt 定义 UiState 和 Intent
  - [x] 创建 MyMusicViewModel.kt 整合三个数据源
  - [x] 创建 MyMusicScreen.kt 实现 Tab 切换 UI
  - [x] 创建可复用的 SongListTab 组件

## Phase 2: 整合歌词到播放器
- [x] Task 2.1: 重构 PlayerScreen 支持视图切换
  - [x] 在 PlayerUiState 中添加视图模式状态（封面/歌词）
  - [x] 添加视图切换动画
  - [x] 将歌词组件整合到 PlayerScreen
- [x] Task 2.2: 移除独立的 LyricsScreen
  - [x] 更新导航配置移除 Lyrics 路由
  - [x] 删除 LyricsScreen、LyricsViewModel、LyricsContract

## Phase 3: 整合设置相关页面
- [x] Task 3.1: 重构 SettingsScreen 支持内嵌子页面
  - [x] 创建 SettingsNavState 管理子页面状态
  - [x] 实现扫描功能的内嵌展示
  - [x] 实现排除文件夹管理的内嵌展示
  - [x] 实现 API 配置的内嵌展示
  - [x] 实现音乐库统计的内嵌展示
- [x] Task 3.2: 移除独立的设置子页面
  - [x] 更新导航配置移除相关路由
  - [x] 删除 ScanScreen、ScanViewModel、ScanContract
  - [x] 删除 ExcludeFoldersScreen、ExcludeFoldersViewModel、ExcludeFoldersContract
  - [x] 删除 ApiKeyConfigScreen、ApiKeyConfigViewModel
  - [x] 删除 LibraryStatsScreen、LibraryStatsViewModel、LibraryStatsContract

## Phase 4: 整合歌曲详情与标签编辑
- [x] Task 4.1: 重构 SongDetailScreen 支持编辑模式
  - [x] 在 SongDetailUiState 中添加编辑模式状态
  - [x] 实现编辑模式的 UI（可编辑字段）
  - [x] 添加保存和取消功能
- [x] Task 4.2: 移除独立的 TagEditorScreen
  - [x] 更新导航配置移除 TagEditor 路由
  - [x] 删除 TagEditorScreen、TagEditorViewModel、TagEditorContract

## Phase 5: 更新导航和首页
- [x] Task 5.1: 简化导航结构
  - [x] 更新 Screen.kt 移除废弃的路由定义
  - [x] 更新 AppNavGraph.kt 移除废弃的 composable
  - [x] 添加 MyMusic 路由
- [x] Task 5.2: 更新首页快速访问入口
  - [x] 修改 HomeScreen 的导航回调
  - [x] 将喜欢、最近播放、最常播放统一导航到 MyMusic

## Phase 6: 清理废弃代码
- [x] Task 6.1: 删除废弃的页面文件
  - [x] 删除 presentation/favorites/ 目录
  - [x] 删除 presentation/recentlyplayed/ 目录
  - [x] 删除 presentation/mostplayed/ 目录
  - [x] 删除 presentation/lyrics/ 目录（保留 components）
  - [x] 删除 presentation/scan/ 目录
  - [x] 删除 presentation/excludefolders/ 目录
  - [x] 删除 presentation/tageditor/ 目录

# Task Dependencies
- [Task 2.1] depends on [Task 1.1] (可以先并行开始)
- [Task 3.1] depends on [Task 1.1] (可以先并行开始)
- [Task 4.1] depends on [Task 1.1] (可以先并行开始)
- [Task 5.1] depends on [Task 1.1, Task 2.1, Task 3.1, Task 4.1]
- [Task 5.2] depends on [Task 5.1]
- [Task 6.1] depends on [Task 5.2]
