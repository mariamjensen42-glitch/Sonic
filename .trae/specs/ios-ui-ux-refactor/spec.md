# iOS 设计风格 UI/UX 重构规格

## Why
当前项目虽然已有 iOS 风格的基础组件，但整体设计系统不够统一和完善，部分组件和屏幕未完全遵循 iOS Human Interface Guidelines。需要全面重构以确保整个应用呈现一致、专业的 iOS 原生体验。

## What Changes
- **BREAKING**: 重构主题系统，采用完整的 iOS 设计令牌
- 重构所有 UI 组件，完全遵循 iOS Human Interface Guidelines
- 统一动画和交互规范
- 优化深色模式支持
- 完善无障碍访问支持

## Impact
- Affected specs: 所有 UI 相关功能
- Affected code: `presentation` 模块下的所有文件

---

## ADDED Requirements

### Requirement: iOS 设计令牌系统

系统 SHALL 提供完整的 iOS 设计令牌，包括色彩、字体、间距、圆角、阴影等。

#### Scenario: 色彩系统
- **WHEN** 应用需要使用颜色
- **THEN** 应使用 iOS 系统色彩（SF Symbols 配色）

**色彩定义**:
```
iOS 系统色 (Light/Dark):
- Blue: #007AFF / #0A84FF (主色)
- Green: #34C759 / #30D158 (成功)
- Red: #FF3B30 / #FF453A (错误/播放控制)
- Orange: #FF9500 / #FF9F0A (警告)
- Yellow: #FFCC00 / #FFD60A
- Purple: #AF52DE / #BF5AF2
- Pink: #FF2D55 / #FF375F (收藏)
- Teal: #5AC8FA / #64D2FF
- Indigo: #5856D6 / #5E5CE6

语义色 (Light/Dark):
- Background: #F2F2F7 / #000000
- Secondary Background: #FFFFFF / #1C1C1E
- Tertiary Background: #F2F2F7 / #2C2C2E
- Separator: #C6C6C8 / #38383A
- Label: #000000 / #FFFFFF
- Secondary Label: #3C3C43 / #EBEBF5
- Tertiary Label: #3C3C43 / #EBEBF5
```

#### Scenario: 字体系统
- **WHEN** 应用需要显示文本
- **THEN** 应使用 iOS 标准字体样式

**字体定义**:
```
Large Title: 34sp, Bold
Title 1: 28sp, Bold
Title 2: 22sp, Bold
Title 3: 20sp, SemiBold
Headline: 17sp, SemiBold
Body: 17sp, Regular
Callout: 16sp, Regular
Subhead: 15sp, Regular
Footnote: 13sp, Regular
Caption 1: 12sp, Regular
Caption 2: 11sp, Regular
```

#### Scenario: 间距系统
- **WHEN** 应用需要布局间距
- **THEN** 应使用 iOS 标准间距

**间距定义**:
```
xs: 4dp
sm: 8dp
md: 16dp
lg: 24dp
xl: 32dp
xxl: 48dp

列表项内边距: 16dp (水平)
卡片内边距: 16dp
屏幕边距: 16dp
分组间距: 35dp (iOS 标准)
```

#### Scenario: 圆角系统
- **WHEN** 应用需要圆角
- **THEN** 应使用 iOS 标准圆角

**圆角定义**:
```
small: 8dp (按钮、小卡片)
medium: 12dp (卡片、列表组)
large: 16dp (模态框、大卡片)
xlarge: 20dp (底部弹窗)
continuous: 使用连续曲线圆角 (iOS 特有)
```

---

### Requirement: iOS 风格导航组件

系统 SHALL 提供完整的 iOS 风格导航组件。

#### Scenario: 大标题导航栏
- **WHEN** 页面使用大标题导航栏
- **THEN** 滚动时标题应平滑缩小并固定在顶部

**规范**:
- 大标题: 34sp, 左对齐, 滚动后隐藏
- 小标题: 17sp, 居中, 滚动后显示
- 返回按钮: 使用 chevron.left 图标 + 前一页面标题
- 右侧操作按钮: 使用 SF Symbols 风格图标

#### Scenario: Tab Bar
- **WHEN** 应用需要底部标签栏
- **THEN** 应使用 iOS 标准 Tab Bar 样式

**规范**:
- 高度: 49dp (不含安全区域)
- 图标: 24dp
- 标签: 10sp
- 选中色: 主色
- 未选中色: gray (Light) / gray (Dark)
- 背景毛玻璃效果

---

### Requirement: iOS 风格列表组件

系统 SHALL 提供完整的 iOS 风格列表组件。

#### Scenario: Inset Grouped 列表
- **WHEN** 显示设置或菜单列表
- **THEN** 应使用 Inset Grouped 样式

**规范**:
- 圆角: 12dp
- 水平边距: 16dp
- 分组间距: 35dp
- 分割线: 左侧缩进 62dp (图标 + 间距)
- 列表项高度: 44dp (单行) / 64dp (双行)

#### Scenario: 列表项交互
- **WHEN** 用户点击列表项
- **THEN** 应显示 iOS 风格的点击反馈

**规范**:
- 按压时: 背景变为灰色半透明 (highlight color)
- 无缩放动画 (与 Android 默认不同)
- 松开后: 平滑恢复

---

### Requirement: iOS 风格按钮组件

系统 SHALL 提供完整的 iOS 风格按钮组件。

#### Scenario: 填充按钮
- **WHEN** 需要主要操作按钮
- **THEN** 应使用 iOS 风格填充按钮

**规范**:
- 圆角: 12dp
- 内边距: 14dp 垂直, 24dp 水平
- 按压缩放: 0.96x
- 弹簧动画: dampingRatio = 0.7, stiffness = medium

#### Scenario: 文本按钮
- **WHEN** 需要次要操作按钮
- **THEN** 应使用 iOS 风格文本按钮

**规范**:
- 无背景
- 主色文字
- 按压时: 文字透明度降低

#### Scenario: 分段控件
- **WHEN** 需要切换选项
- **THEN** 应使用 iOS 风格分段控件

**规范**:
- 圆角: 8dp
- 选中背景: 主色
- 未选中背景: 透明/表面色
- 平滑切换动画

---

### Requirement: iOS 风格卡片组件

系统 SHALL 提供完整的 iOS 风格卡片组件。

#### Scenario: 媒体卡片
- **WHEN** 显示专辑/播放列表
- **THEN** 应使用 iOS 风格媒体卡片

**规范**:
- 宽度: 160dp (标准) / 可自定义
- 宽高比: 1:1
- 圆角: 12dp
- 按压缩放: 0.96x
- 占位图: 渐变背景 + 图标

#### Scenario: 艺术家卡片
- **WHEN** 显示艺术家
- **THEN** 应使用圆形头像样式

**规范**:
- 头像: 圆形
- 尺寸: 110dp
- 占位图: 粉紫渐变

---

### Requirement: iOS 风格播放器界面

系统 SHALL 提供完整的 iOS 风格播放器界面。

#### Scenario: Mini Player
- **WHEN** 正在播放音乐
- **THEN** 应在底部显示 Mini Player

**规范**:
- 高度: 64dp
- 进度条: 2dp, 顶部, 红色
- 专辑封面: 48dp, 圆角 8dp
- 播放按钮: 32dp
- 背景: 毛玻璃效果

#### Scenario: 全屏播放器
- **WHEN** 用户展开播放器
- **THEN** 应显示全屏播放器界面

**规范**:
- 专辑封面: 85% 宽度, 1:1 比例, 圆角 12dp
- 播放按钮: 72dp 圆形, 红色背景
- 进度条: 红色滑块和轨道
- 从底部滑入动画
- 下拉关闭手势

---

### Requirement: iOS 风格动画规范

系统 SHALL 提供统一的 iOS 风格动画。

#### Scenario: 导航转场
- **WHEN** 页面切换
- **THEN** 应使用 iOS 风格转场动画

**规范**:
```
Push 进入:
- 从右侧滑入 (fullWidth)
- 淡入 (alpha 0.3 → 1.0)
- 弹簧动画: dampingRatio = 0.85

Push 退出:
- 向左滑出 (fullWidth / 3)
- 淡出 (alpha 1.0 → 0.8)

Pop 进入:
- 从左侧滑入 (fullWidth / 3)
- 淡入 (alpha 0.8 → 1.0)

Pop 退出:
- 向右滑出 (fullWidth)
- 淡出 (alpha 1.0 → 0.3)

Modal:
- 从底部滑入
- 淡入 (alpha 0.5 → 1.0)
```

#### Scenario: 交互动画
- **WHEN** 用户交互
- **THEN** 应使用弹簧动画

**规范**:
```
按压缩放:
- scale: 0.96x (按钮) / 0.98x (列表项)
- spring: dampingRatio = 0.7, stiffness = medium

开关切换:
- AnimatedVisibility
- spring: dampingRatio = 0.8

滚动:
- Overscroll 效果
- 快速滑动减速
```

---

### Requirement: iOS 风格搜索界面

系统 SHALL 提供完整的 iOS 风格搜索界面。

#### Scenario: 搜索栏
- **WHEN** 用户需要搜索
- **THEN** 应显示 iOS 风格搜索栏

**规范**:
- 圆角: 10dp
- 背景: 表面色/表面变体色
- 占位符: "搜索"
- 取消按钮: 搜索时显示
- 搜索建议: 历史记录

---

### Requirement: iOS 风格设置界面

系统 SHALL 提供完整的 iOS 风格设置界面。

#### Scenario: 设置列表
- **WHEN** 显示设置选项
- **THEN** 应使用 iOS 风格设置列表

**规范**:
- Inset Grouped 样式
- 图标容器: 32dp, 圆角 8dp
- 分组标题: 13sp, 大写, 左对齐
- 分组页脚: 说明文字

---

### Requirement: 深色模式支持

系统 SHALL 完美支持深色模式。

#### Scenario: 深色模式切换
- **WHEN** 系统或用户切换深色模式
- **THEN** 所有界面元素应平滑过渡

**规范**:
- 使用语义化颜色
- 避免硬编码颜色
- 图片和图标适配
- 平滑过渡动画

---

### Requirement: 无障碍访问支持

系统 SHALL 支持无障碍访问。

#### Scenario: 屏幕阅读器
- **WHEN** 用户使用屏幕阅读器
- **THEN** 所有元素应有正确的描述

**规范**:
- contentDescription: 中文描述
- 重要操作: 添加状态描述
- 图标按钮: 描述功能

#### Scenario: 字体缩放
- **WHEN** 用户调整字体大小
- **THEN** 界面应正确适配

**规范**:
- 使用 sp 单位
- 避免固定高度
- 文本可换行

---

## MODIFIED Requirements

### Requirement: 现有组件优化

现有 iOS 风格组件 SHALL 按照新规范进行优化。

**优化项**:
1. `IOSButton.kt` - 统一动画参数
2. `IOSCard.kt` - 优化占位图渐变
3. `IOSListItem.kt` - 调整按压反馈
4. `IOSLayout.kt` - 优化大标题动画
5. `IOSNavAnimations.kt` - 统一动画参数

---

## REMOVED Requirements

无移除项，所有现有功能保留并优化。
