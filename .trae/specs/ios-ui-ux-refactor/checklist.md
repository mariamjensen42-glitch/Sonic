# iOS UI/UX 重构检查清单

## Phase 1: 设计令牌系统

- [x] 色彩系统包含所有 iOS 系统色 (Blue, Green, Red, Orange, Yellow, Purple, Pink, Teal, Indigo)
- [x] 语义色彩完整 (Background, Label, Separator, Fill, Tint)
- [x] 深色模式色彩正确映射
- [x] 字体系统包含 iOS 标准样式 (Large Title, Title 1-3, Headline, Body, Callout, Subhead, Footnote, Caption)
- [x] 间距系统定义完整 (xs, sm, md, lg, xl, xxl)
- [x] 圆角系统定义完整 (small, medium, large, xlarge)
- [x] 动画参数统一定义

## Phase 2: 核心组件

- [x] IOSFilledButton 符合 iOS 规范 (圆角 12dp, 按压缩放 0.96x)
- [x] IOSTextButton 符合 iOS 规范 (无背景, 主色文字)
- [x] IOSSegmentedButton 符合 iOS 规范 (圆角 8dp, 平滑切换)
- [x] IOSMediaCard 符合 iOS 规范 (圆角 12dp, 按压缩放 0.96x)
- [x] IOSArtistCard 符合 iOS 规范 (圆形头像, 粉紫渐变占位)
- [x] IOSListItem 按压反馈使用高亮色而非缩放
- [x] IOSListItem 分割线左侧缩进正确 (62dp)
- [x] IOSTopAppBar 符合 iOS 规范
- [x] IOSLargeTitleTopAppBar 滚动动画流畅
- [x] IOSSectionHeader 符合 iOS 规范
- [x] IOSCardContainer 符合 iOS 规范

## Phase 3: 导航和动画

- [x] Push 动画符合 iOS 规范 (从右滑入, 淡入)
- [x] Pop 动画符合 iOS 规范 (向右滑出, 淡出)
- [x] Modal 动画符合 iOS 规范 (从底部滑入)
- [x] 弹簧动画参数统一 (dampingRatio = 0.85, stiffness = mediumLow)

## Phase 4: 播放器界面

- [x] MiniPlayer 高度 64dp
- [x] MiniPlayer 进度条在顶部, 2dp 高度
- [x] MiniPlayer 专辑封面 48dp, 圆角 8dp
- [x] PlayerScreen 专辑封面 85% 宽度, 1:1 比例
- [x] PlayerScreen 播放按钮 72dp 圆形, 红色背景
- [x] PlayerScreen 进度条红色滑块和轨道
- [x] PlayerScreen 支持下拉关闭手势
- [x] 播放队列底部弹窗符合 iOS 规范

## Phase 5: 页面界面

- [x] HomeScreen 大标题导航栏滚动动画流畅
- [x] HomeScreen 快速访问区域布局符合 iOS 规范
- [x] SongsScreen 列表样式符合 iOS 规范
- [x] AlbumsScreen 网格布局符合 iOS 规范
- [x] ArtistsScreen 网格布局符合 iOS 规范
- [x] 详情页面头部区域符合 iOS 规范
- [x] SearchScreen 搜索栏符合 iOS 规范 (圆角 10dp)
- [x] SettingsScreen 使用 Inset Grouped 样式

## Phase 6: 歌词界面

- [x] KaraokeLyricsView 样式符合 iOS 规范
- [x] 歌词动画效果流畅
- [x] 深色模式歌词显示正确

## Phase 7: 深色模式和无障碍

- [x] 所有组件深色模式适配正确
- [x] 深色模式切换动画平滑
- [x] 所有图标按钮有 contentDescription
- [x] 字体缩放适配正确

## 整体检查

- [x] 无硬编码颜色值 (使用主题色)
- [x] 无硬编码尺寸值 (使用设计令牌)
- [x] 所有动画使用统一参数
- [x] 所有交互有视觉反馈
- [x] 整体视觉风格统一
