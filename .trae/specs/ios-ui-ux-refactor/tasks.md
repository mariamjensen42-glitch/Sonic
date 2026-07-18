# Tasks

## Phase 1: 设计令牌系统重构

- [x] Task 1: 重构色彩系统 (Color.kt)
  - [x] SubTask 1.1: 完善 iOS 系统色彩定义 (Blue, Green, Red, Orange, Yellow, Purple, Pink, Teal, Indigo)
  - [x] SubTask 1.2: 添加完整的语义色彩 (Background, Label, Separator 等)
  - [x] SubTask 1.3: 确保深色模式色彩正确映射

- [x] Task 2: 重构字体系统 (Typography.kt)
  - [x] SubTask 2.1: 添加 iOS 标准字体样式 (Large Title, Title 1-3, Headline, Body, Callout, Subhead, Footnote, Caption)
  - [x] SubTask 2.2: 调整字号和字重符合 iOS 规范

- [x] Task 3: 创建设计令牌文件 (DesignTokens.kt)
  - [x] SubTask 3.1: 定义间距系统 (xs, sm, md, lg, xl, xxl)
  - [x] SubTask 3.2: 定义圆角系统 (small, medium, large, xlarge)
  - [x] SubTask 3.3: 定义动画参数 (弹簧动画参数)
  - [x] SubTask 3.4: 定义列表项标准尺寸

## Phase 2: 核心组件重构

- [x] Task 4: 重构按钮组件 (IOSButton.kt)
  - [x] SubTask 4.1: 优化 IOSFilledButton 样式和动画
  - [x] SubTask 4.2: 优化 IOSTextButton 样式
  - [x] SubTask 4.3: 优化 IOSSegmentedButton 样式
  - [x] SubTask 4.4: 添加 IOSIconButton 组件

- [x] Task 5: 重构卡片组件 (IOSCard.kt)
  - [x] SubTask 5.1: 优化 IOSMediaCard 占位图和动画
  - [x] SubTask 5.2: 优化 IOSArtistCard 样式
  - [x] SubTask 5.3: 添加 IOSPlaylistCard 组件

- [x] Task 6: 重构列表项组件 (IOSListItem.kt)
  - [x] SubTask 6.1: 优化按压反馈效果 (移除缩放，使用高亮色)
  - [x] SubTask 6.2: 调整分割线样式
  - [x] SubTask 6.3: 添加更多列表项变体

- [x] Task 7: 重构布局组件 (IOSLayout.kt)
  - [x] SubTask 7.1: 优化 IOSTopAppBar 样式
  - [x] SubTask 7.2: 优化 IOSLargeTitleTopAppBar 滚动动画
  - [x] SubTask 7.3: 优化 IOSSectionHeader 样式
  - [x] SubTask 7.4: 优化 IOSCardContainer 样式
  - [x] SubTask 7.5: 优化 IOSScrollbar 样式

## Phase 3: 导航和动画优化

- [x] Task 8: 优化导航动画 (IOSNavAnimations.kt)
  - [x] SubTask 8.1: 统一弹簧动画参数
  - [x] SubTask 8.2: 优化 Push/Pop 动画效果
  - [x] SubTask 8.3: 优化 Modal 动画效果
  - [x] SubTask 8.4: 添加交互式返回手势支持

## Phase 4: 播放器界面优化

- [x] Task 9: 重构 MiniPlayer 组件
  - [x] SubTask 9.1: 优化布局和尺寸
  - [x] SubTask 9.2: 添加毛玻璃背景效果
  - [x] SubTask 9.3: 优化进度条样式

- [x] Task 10: 重构 PlayerScreen 组件
  - [x] SubTask 10.1: 优化专辑封面展示
  - [x] SubTask 10.2: 优化控制按钮布局
  - [x] SubTask 10.3: 优化进度条样式
  - [x] SubTask 10.4: 添加下拉关闭手势
  - [x] SubTask 10.5: 优化播放队列底部弹窗

## Phase 5: 页面界面优化

- [x] Task 11: 优化 HomeScreen
  - [x] SubTask 11.1: 优化大标题导航栏
  - [x] SubTask 11.2: 优化快速访问区域布局

- [x] Task 12: 优化 SongsScreen
  - [x] SubTask 12.1: 优化列表样式
  - [x] SubTask 12.2: 优化排序菜单

- [x] Task 13: 优化 AlbumsScreen 和 ArtistsScreen
  - [x] SubTask 13.1: 优化网格布局
  - [x] SubTask 13.2: 优化卡片样式

- [x] Task 14: 优化详情页面 (AlbumDetail, ArtistDetail, PlaylistDetail)
  - [x] SubTask 14.1: 优化头部区域
  - [x] SubTask 14.2: 优化歌曲列表

- [x] Task 15: 优化 SearchScreen
  - [x] SubTask 15.1: 添加 iOS 风格搜索栏
  - [x] SubTask 15.2: 优化搜索结果展示

- [x] Task 16: 优化 SettingsScreen
  - [x] SubTask 16.1: 使用 Inset Grouped 样式
  - [x] SubTask 16.2: 优化分组布局

## Phase 6: 歌词界面优化

- [x] Task 17: 优化歌词组件
  - [x] SubTask 17.1: 优化 KaraokeLyricsView 样式
  - [x] SubTask 17.2: 优化歌词动画效果
  - [x] SubTask 17.3: 确保深色模式适配

## Phase 7: 深色模式和无障碍

- [x] Task 18: 完善深色模式支持
  - [x] SubTask 18.1: 检查所有组件深色模式适配
  - [x] SubTask 18.2: 添加平滑过渡动画

- [x] Task 19: 完善无障碍支持
  - [x] SubTask 19.1: 添加所有图标按钮的 contentDescription
  - [x] SubTask 19.2: 确保字体缩放适配

---

# Task Dependencies

- Task 4, 5, 6, 7 依赖 Task 1, 2, 3 (设计令牌)
- Task 8 可并行进行
- Task 9, 10 依赖 Task 4, 5, 6, 7
- Task 11-17 依赖 Task 4-10
- Task 18, 19 依赖所有前置任务
