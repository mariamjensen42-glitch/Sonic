# Tasks

## Phase 1: 主题系统增强

- [x] Task 1: 扩展 DesignTokens.kt 添加状态层支持
  - [x] SubTask 1.1: 添加 M3StateLayers 对象（Hover 8%, Focus 12%, Pressed 12%, Drag 16%）
  - [x] SubTask 1.2: 添加 M3Easing 对象（Standard, Emphasized, Decelerate, Accelerate）
  - [x] SubTask 1.3: 添加 M3Duration 对象（Short, Medium, Long, ExtraLong）

- [x] Task 2: 增强 Color.kt 添加渐变支持
  - [x] SubTask 2.1: 添加渐变色生成函数
  - [x] SubTask 2.2: 添加 M3GradientColors 对象

- [x] Task 3: 创建 Motion.kt 动画系统
  - [x] SubTask 3.1: 创建 M3EasingTokens 对象
  - [x] SubTask 3.2: 创建 M3DurationTokens 对象
  - [x] SubTask 3.3: 创建容器变换动画辅助函数
  - [x] SubTask 3.4: 创建共享轴过渡动画辅助函数

## Phase 2: 组件状态层实现

- [x] Task 4: 增强 M3Button 组件
  - [x] SubTask 4.1: 添加完整的状态层支持（Hover, Focus, Pressed）
  - [x] SubTask 4.2: 使用正确的 Ripple 效果
  - [x] SubTask 4.3: 添加焦点指示器

- [x] Task 5: 增强 M3Card 组件
  - [x] SubTask 5.1: 添加 Hover 状态层
  - [x] SubTask 5.2: 添加 Pressed 状态层
  - [x] SubTask 5.3: 优化卡片海拔变化动画

- [x] Task 6: 增强 M3ListItem 组件
  - [x] SubTask 6.1: 添加 Hover 状态层
  - [x] SubTask 6.2: 添加 Pressed 状态层
  - [x] SubTask 6.3: 添加焦点指示器

- [x] Task 7: 创建 M3StateLayer 通用组件
  - [x] SubTask 7.1: 创建可复用的状态层修饰符
  - [x] SubTask 7.2: 支持所有交互状态

## Phase 3: 动效系统升级

- [x] Task 8: 增强动态颜色过渡
  - [x] SubTask 8.1: 修改 M3ThemeManager 使用 EmphasizedDecelerate 缓动
  - [x] SubTask 8.2: 将颜色过渡时间改为 500ms
  - [x] SubTask 8.3: 添加颜色插值动画

- [x] Task 9: 创建页面过渡动画
  - [x] SubTask 9.1: 创建 SharedAxisTransition 组件
  - [x] SubTask 9.2: 在导航中应用页面过渡

## Phase 4: 页面重构

- [x] Task 10: 重构 SettingsScreen
  - [x] SubTask 10.1: 使用 M3ListItem 替换旧列表项
  - [x] SubTask 10.2: 使用 M3TopAppBar 替换旧顶栏
  - [x] SubTask 10.3: 添加状态层反馈

- [x] Task 11: 重构 SearchScreen
  - [x] SubTask 11.1: 使用 M3SearchBar 替换旧搜索框
  - [x] SubTask 11.2: 使用 M3ListItem 替换旧列表项
  - [x] SubTask 11.3: 添加状态层反馈

- [x] Task 12: 重构 FolderScreen
  - [x] SubTask 12.1: 使用 M3ListItem 替换旧列表项
  - [x] SubTask 12.2: 使用 M3TopAppBar 替换旧顶栏
  - [x] SubTask 12.3: 添加状态层反馈

- [x] Task 13: 重构 MyMusicScreen
  - [x] SubTask 13.1: 使用 M3 组件替换旧组件
  - [x] SubTask 13.2: 添加状态层反馈

- [x] Task 14: 重构 SongDetailScreen
  - [x] SubTask 14.1: 使用 M3 组件替换旧组件
  - [x] SubTask 14.2: 添加状态层反馈

- [x] Task 15: 重构 AlbumDetailScreen
  - [x] SubTask 15.1: 使用 M3 组件替换旧组件
  - [x] SubTask 15.2: 添加状态层反馈

- [x] Task 16: 重构 ArtistDetailScreen
  - [x] SubTask 16.1: 使用 M3 组件替换旧组件
  - [x] SubTask 16.2: 添加状态层反馈

- [x] Task 17: 重构 PlaylistDetailScreen
  - [x] SubTask 17.1: 使用 M3 组件替换旧组件
  - [x] SubTask 17.2: 添加状态层反馈

- [x] Task 18: 重构 PlaylistsScreen
  - [x] SubTask 18.1: 使用 M3 组件替换旧组件
  - [x] SubTask 18.2: 添加状态层反馈

- [x] Task 19: 重构 AiInfoScreen
  - [x] SubTask 19.1: 使用 M3 组件替换旧组件
  - [x] SubTask 19.2: 添加状态层反馈

## Phase 5: 无障碍改进

- [x] Task 20: 添加语义标签
  - [x] SubTask 20.1: 为所有按钮添加 contentDescription
  - [x] SubTask 20.2: 为所有图标添加语义标签
  - [x] SubTask 20.3: 优化屏幕阅读器支持

- [x] Task 21: 实现焦点导航
  - [x] SubTask 21.1: 确保所有可交互元素可聚焦
  - [x] SubTask 21.2: 添加焦点顺序

## Phase 6: 验证和优化

- [x] Task 22: 深色模式验证
  - [x] SubTask 22.1: 检查所有页面深色模式显示
  - [x] SubTask 22.2: 验证对比度合规

- [x] Task 23: 动效验证
  - [x] SubTask 23.1: 验证所有动画流畅
  - [x] SubTask 23.2: 检查动画持续时间

- [x] Task 24: 编译和运行验证
  - [x] SubTask 24.1: 确保无编译错误
  - [x] SubTask 24.2: 确保无运行时崩溃

---

# Task Dependencies

- Task 2 依赖 Task 1
- Task 3 依赖 Task 1
- Task 4-7 依赖 Task 1, 2, 3
- Task 8 依赖 Task 3
- Task 9 依赖 Task 3
- Task 10-19 依赖 Task 4-7
- Task 20-21 依赖 Task 10-19
- Task 22-24 依赖 Task 10-21
