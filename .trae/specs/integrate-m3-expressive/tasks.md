# Tasks

## Phase 1: 主题系统整合

*- [x] Task 1: 更新 MainActivity 使用 M3ManagedTheme
  - [x] SubTask 1.1: 在 Application 中初始化 M3ThemeManager
  - [x] SubTask 1.2: 替换 M3Theme 为 M3ManagedTheme
  - [ ] SubTask 1.3: 移除旧的 Theme.kt 或保留作为备用

- [x] Task 2: 实现播放器动态颜色
  - [x] SubTask 2.1: 创建 DynamicColorController 监听播放状态
  - [x] SubTask 2.2: 在歌曲切换时提取专辑封面颜色
  - [x] SubTask 2.3: 应用提取的颜色到主题
  - [x] SubTask 2.4: 添加颜色过渡动画

## Phase 2: 页面组件替换

*- [x] Task 3: 更新首页组件
  - [x] SubTask 3.1: 使用 M3Carousel 替换横向滚动卡片
  - [x] SubTask 3.2: 使用 M3Container 组织内容分组
  - [x] SubTask 3.3: 使用 M3SectionContainer 添加标题

*- [x] Task 4: 更新歌曲列表组件
  - [x] SubTask 4.1: 使用 M3ListItemExpressive 替换列表项
  - [x] SubTask 4.2: 添加尾部组件变体 (Chevron, Badge, Text)

* [x] Task 5: 更新底部导航栏

  * [x] SubTask 5.1: 使用增强后的 M3NavigationBar

  * [x] SubTask 5.2: 确保选中指示器圆润

* [x] Task 6: 更新卡片组件

  * [x] SubTask 6.1: 使用 M3MediaCard 替换媒体卡片

  * [x] SubTask 6.2: 使用 M3ArtistCard 替换艺术家卡片

  * [x] SubTask 6.3: 启用 expressive 参数

## Phase 3: 设置页面

* [x] Task 7: 创建主题设置页面

  * [x] SubTask 7.1: 创建 ThemeSettingsScreen

  * [x] SubTask 7.2: 添加主题预设选择器

  * [x] SubTask 7.3: 添加对比度选择器

  * [x] SubTask 7.4: 添加深色模式切换

  * [x] SubTask 7.5: 添加动态颜色开关

* [x] Task 8: 添加设置导航

  * [x] SubTask 8.1: 在设置页面添加主题设置入口

  * [x] SubTask 8.2: 添加导航路由

***

# Task Dependencies

* Task 2 依赖 Task 1

* Task 3-6 依赖 Task 1

* Task 7-8 依赖 Task 1

* Task 3-6 可并行进行

