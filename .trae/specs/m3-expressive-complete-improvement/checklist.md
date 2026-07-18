# Checklist

## Phase 1: 主题系统增强

### Task 1: 扩展 DesignTokens.kt
- [x] M3StateLayers 对象已添加（Hover 8%, Focus 12%, Pressed 12%, Drag 16%）
- [x] M3Easing 对象已添加（Standard, Emphasized, Decelerate, Accelerate）
- [x] M3Duration 对象已添加（Short, Medium, Long, ExtraLong）

### Task 2: 增强 Color.kt
- [x] 渐变色生成函数已添加
- [x] M3GradientColors 对象已添加

### Task 3: 创建 Motion.kt
- [x] M3EasingTokens 对象已创建
- [x] M3DurationTokens 对象已创建
- [x] 容器变换动画辅助函数已创建
- [x] 共享轴过渡动画辅助函数已创建

## Phase 2: 组件状态层实现

### Task 4: 增强 M3Button
- [x] Hover 状态层实现正确
- [x] Focus 状态层实现正确
- [x] Pressed 状态层实现正确
- [x] Ripple 效果使用正确
- [x] 焦点指示器可见

### Task 5: 增强 M3Card
- [x] Hover 状态层实现正确
- [x] Pressed 状态层实现正确
- [x] 海拔变化动画流畅

### Task 6: 增强 M3ListItem
- [x] Hover 状态层实现正确
- [x] Pressed 状态层实现正确
- [x] 焦点指示器可见

### Task 7: 创建 M3StateLayer
- [x] 状态层修饰符可复用
- [x] 支持所有交互状态

## Phase 3: 动效系统升级

### Task 8: 增强动态颜色过渡
- [x] 使用 EmphasizedDecelerate 缓动
- [x] 过渡时间为 500ms
- [x] 颜色插值动画平滑

### Task 9: 创建页面过渡动画
- [x] SharedAxisTransition 组件已创建
- [x] 页面过渡已应用到导航

## Phase 4: 页面重构

### Task 10: SettingsScreen
- [x] 使用 M3ListItem 组件
- [x] 使用 M3TopAppBar 组件
- [x] 状态层反馈正确

### Task 11: SearchScreen
- [x] 使用 M3SearchBar 组件
- [x] 使用 M3ListItem 组件
- [x] 状态层反馈正确

### Task 12: FolderScreen
- [x] 使用 M3ListItem 组件
- [x] 使用 M3TopAppBar 组件
- [x] 状态层反馈正确

### Task 13: MyMusicScreen
- [x] 使用 M3 组件
- [x] 状态层反馈正确

### Task 14: SongDetailScreen
- [x] 使用 M3 组件
- [x] 状态层反馈正确

### Task 15: AlbumDetailScreen
- [x] 使用 M3 组件
- [x] 状态层反馈正确

### Task 16: ArtistDetailScreen
- [x] 使用 M3 组件
- [x] 状态层反馈正确

### Task 17: PlaylistDetailScreen
- [x] 使用 M3 组件
- [x] 状态层反馈正确

### Task 18: PlaylistsScreen
- [x] 使用 M3 组件
- [x] 状态层反馈正确

### Task 19: AiInfoScreen
- [x] 使用 M3 组件
- [x] 状态层反馈正确

## Phase 5: 无障碍改进

### Task 20: 语义标签
- [x] 所有按钮有 contentDescription
- [x] 所有图标有语义标签
- [x] 屏幕阅读器支持良好

### Task 21: 焦点导航
- [x] 所有可交互元素可聚焦
- [x] 焦点顺序正确

## Phase 6: 验证和优化

### Task 22: 深色模式
- [x] 所有页面深色模式显示正确
- [x] 对比度符合 WCAG 标准

### Task 23: 动效
- [x] 所有动画流畅无卡顿
- [x] 动画持续时间符合规范

### Task 24: 编译和运行
- [x] 无编译错误
- [x] 无运行时崩溃

## 整体验收

- [x] UI 完全符合 Material 3 Expressive 设计规范
- [x] 所有组件有正确的状态层反馈
- [x] 动效系统生动且有意义
- [x] 深色模式和浅色模式都表现良好
- [x] 无障碍功能完整
