# Tasks

## Phase 1: Core UI 模块基础设施

- [x] Task 1: 配置 core 模块支持 Compose
  - [x] SubTask 1.1: 更新 core/build.gradle.kts 添加 Compose 依赖
  - [x] SubTask 1.2: 创建 core/ui 包结构

- [x] Task 2: 创建主题系统 (core/ui/theme/)
  - [x] SubTask 2.1: 创建 Color.kt - 定义 Material 3 核心色彩
  - [x] SubTask 2.2: 创建 Typography.kt - 定义 Material 3 字体样式
  - [x] SubTask 2.3: 创建 DesignTokens.kt - 定义间距、形状、动画参数
  - [x] SubTask 2.4: 创建 Theme.kt - Material 3 主题配置和动态色彩

## Phase 2: Core UI 通用组件

- [x] Task 3: 创建按钮组件 (core/ui/components/M3Button.kt)
- [x] Task 4: 创建卡片组件 (core/ui/components/M3Card.kt)
- [x] Task 5: 创建列表组件 (core/ui/components/M3ListItem.kt)
- [x] Task 6: 创建导航组件 (core/ui/components/M3Navigation.kt)
- [x] Task 7: 创建布局组件 (core/ui/components/M3Layout.kt)
- [x] Task 8: 创建输入组件 (core/ui/components/M3Input.kt)
- [x] Task 9: 创建指示器组件 (core/ui/components/M3Indicator.kt)
- [x] Task 10: 创建动画系统 (core/ui/animation/Motion.kt)

## Phase 3: Presentation 播放器界面重构

- [x] Task 11: 重构 MiniPlayer 组件
- [x] Task 12: 重构 PlayerScreen 组件
- [x] Task 13: 重构播放队列组件

## Phase 4: Presentation 页面界面重构

- [x] Task 14: 重构 HomeScreen
- [x] Task 15: 重构 SongsScreen
- [x] Task 16: 重构 AlbumsScreen 和 ArtistsScreen

## Phase 5: Presentation 剩余页面重构 (新增)

- [ ] Task 25: 重构 SettingsScreen
  - [ ] SubTask 25.1: 移除 presentation.theme 引用，使用 core.ui.theme
  - [ ] SubTask 25.2: 使用 M3ListItem 替换旧列表项
  - [ ] SubTask 25.3: 使用 M3TopAppBar 替换旧顶栏

- [ ] Task 26: 重构 SearchScreen
  - [ ] SubTask 26.1: 移除 presentation.theme 引用
  - [ ] SubTask 26.2: 使用 M3SearchBar 替换旧搜索框
  - [ ] SubTask 26.3: 使用 M3ListItem 替换旧列表项

- [ ] Task 27: 重构 FolderScreen
  - [ ] SubTask 27.1: 移除 IOSCenteredContent、IOSListItem、IOSScreenWithTopBar 引用
  - [ ] SubTask 27.2: 移除 presentation.theme 引用
  - [ ] SubTask 27.3: 使用 M3ListItem 和 M3TopAppBar

- [ ] Task 28: 重构 MyMusicScreen
  - [ ] SubTask 28.1: 移除 IOS* 组件引用
  - [ ] SubTask 28.2: 移除 presentation.theme 引用
  - [ ] SubTask 28.3: 使用 M3 组件重构

- [ ] Task 29: 重构 SongDetailScreen
  - [ ] SubTask 29.1: 移除 IOS* 组件引用
  - [ ] SubTask 29.2: 移除 presentation.theme 引用
  - [ ] SubTask 29.3: 使用 M3 组件重构

- [ ] Task 30: 重构详情页面
  - [ ] SubTask 30.1: 重构 AlbumDetailScreen
  - [ ] SubTask 30.2: 重构 ArtistDetailScreen
  - [ ] SubTask 30.3: 重构 PlaylistDetailScreen

- [ ] Task 31: 重构 PlaylistsScreen
  - [ ] SubTask 31.1: 移除 IOS* 组件引用
  - [ ] SubTask 31.2: 使用 M3 组件重构

- [ ] Task 32: 重构 AiInfoScreen
  - [ ] SubTask 32.1: 移除 presentation.theme 引用
  - [ ] SubTask 32.2: 使用 M3 组件重构

- [ ] Task 33: 重构其他组件
  - [ ] SubTask 33.1: 重构 KaraokeLyricsView
  - [ ] SubTask 33.2: 重构 SongComponents
  - [ ] SubTask 33.3: 重构 AiPlaylistCreationDialog
  - [ ] SubTask 33.4: 删除或重构 IOSNavAnimations

## Phase 6: 清理和优化

- [x] Task 23: 移除 iOS 风格组件
- [x] Task 24: 更新设计系统文档

- [ ] Task 34: 清理残留引用
  - [ ] SubTask 34.1: 确保无 presentation.theme 引用
  - [ ] SubTask 34.2: 确保无 IOS* 组件引用
  - [ ] SubTask 34.3: 删除未使用的旧组件文件

---

# Task Dependencies

- Task 25-33 依赖 Task 3-10 (核心组件)
- Task 25-33 可并行进行
- Task 34 依赖 Task 25-33
