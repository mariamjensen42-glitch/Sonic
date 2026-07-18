# Tasks

## Phase 1: 设计令牌扩展

- [x] Task 1: 扩展 DesignTokens.kt
  - [x] SubTask 1.1: 新增 M3ExpressiveShapes 对象 - 35 种形状变体
  - [x] SubTask 1.2: 新增 M3ExpressiveColors 对象 - Expressive 强调色
  - [x] SubTask 1.3: 新增 M3SpringTokens 对象 - 弹簧动画 Token (Standard/Expressive, Fast/Default/Slow)
  - [x] SubTask 1.4: 新增 M3ExpressiveComponentSize 对象 - 组件尺寸扩展

## Phase 2: 新增表现型组件

- [x] Task 2: 创建 M3ButtonGroup 组件
  - [x] SubTask 2.1: 实现 M3HorizontalButtonGroup
  - [x] SubTask 2.2: 实现 M3VerticalButtonGroup
  - [x] SubTask 2.3: 支持 connected/separated 样式
  - [x] SubTask 2.4: 添加弹簧动画

- [x] Task 3: 创建 M3SplitButton 组件
  - [x] SubTask 3.1: 实现主按钮 + 下拉箭头结构
  - [x] SubTask 3.2: 实现下拉菜单展开动画
  - [x] SubTask 3.3: 支持自定义主按钮内容

- [x] Task 4: 创建 M3FABMenu 组件
  - [x] SubTask 4.1: 实现 FAB 点击展开菜单
  - [x] SubTask 4.2: 支持向上/向左展开方向
  - [x] SubTask 4.3: 实现交错展开动画
  - [x] SubTask 4.4: 支持可选文字标签

- [x] Task 5: 创建 M3Toolbar 组件
  - [x] SubTask 5.1: 实现底部工具栏布局
  - [x] SubTask 5.2: 支持图标按钮均匀分布
  - [x] SubTask 5.3: 添加按钮交互反馈动画

- [x] Task 6: 创建 M3Menu 组件文件
  - [x] SubTask 6.1: 实现 M3VerticalMenu 基础菜单
  - [x] SubTask 6.2: 实现带间隙选项 (withGap)
  - [x] SubTask 6.3: 实现标准/活力菜单风格
  - [x] SubTask 6.4: 实现子菜单展开动画
  - [x] SubTask 6.5: 支持选中状态样式

## Phase 3: 其他新组件

- [x] Task 7: 创建 M3Carousel 组件
  - [x] SubTask 7.1: 实现横向滚动轮播布局
  - [x] SubTask 7.2: 实现指示器组件
  - [x] SubTask 7.3: 添加弹簧滑动动画
  - [x] SubTask 7.4: 支持可见前后项目预览

- [x] Task 8: 创建 M3Container 组件
  - [x] SubTask 8.1: 实现分组容器基础布局
  - [x] SubTask 8.2: 支持可选标题
  - [x] SubTask 8.3: 支持多种背景和边框样式

## Phase 4: 现有组件 Expressive 增强

- [x] Task 9: 增强 M3Button 组件
  - [x] SubTask 9.1: 添加 Expressive 变体样式
  - [x] SubTask 9.2: 添加更大尺寸选项 (Small/Medium/Large)
  - [x] SubTask 9.3: 优化弹簧动画参数

- [x] Task 10: 增强 M3Card 组件
  - [x] SubTask 10.1: M3MediaCard 支持形状变体
  - [x] SubTask 10.2: M3ArtistCard 支持多种头像形状
  - [x] SubTask 10.3: 添加 Expressive 动画效果

- [x] Task 11: 增强 M3Navigation 组件
  - [x] SubTask 11.1: M3TopAppBar 支持 CenterAligned 变体
  - [x] SubTask 11.2: M3NavigationBar 更圆润的选中指示器
  - [x] SubTask 11.3: 优化导航项动画

- [x] Task 12: 增强 M3ListItem 组件
  - [x] SubTask 12.1: 支持更丰富的尾部组件变体
  - [x] SubTask 12.2: 优化列表项交互动画

---

# Task Dependencies

- Task 2-8 依赖 Task 1 (设计令牌)
- Task 9-12 依赖 Task 1 (设计令牌)
- Task 2-8 可并行进行
- Task 9-12 可并行进行
