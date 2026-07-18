# Material Design 3 Expressive 风格重构规格

## Why
当前项目采用 iOS 风格设计，与 Android 平台原生体验不一致。Material 3 Expressive 是 Google 最新的设计系统增强，提供更富有表现力、更符合 Android 用户习惯的界面风格。重构后将提升用户体验一致性，增强界面的情感化和个性化。

## What Changes
- **BREAKING**: 移除所有 iOS 风格组件，替换为 Material 3 Expressive 组件
- **BREAKING**: 重构主题系统，采用 Material 3 动态色彩
- 重构所有 UI 组件，使用 Material 3 Expressive 设计语言
- 引入运动弹簧动画系统
- 采用强调型排版系统
- 使用丰富的形状库

## Impact
- Affected specs: 所有 UI 相关功能
- Affected code: 
  - `core` 模块: 新增 `ui` 子包存放通用 UI 组件
  - `presentation` 模块: 页面和业务组件

## 模块结构

```
core/
├── src/main/java/com/cycling/core/
│   └── ui/                          # 通用 UI 组件
│       ├── theme/                   # 主题系统
│       │   ├── Color.kt
│       │   ├── Typography.kt
│       │   ├── Theme.kt
│       │   └── DesignTokens.kt
│       ├── components/              # 通用组件
│       │   ├── M3Button.kt
│       │   ├── M3Card.kt
│       │   ├── M3ListItem.kt
│       │   ├── M3Navigation.kt
│       │   ├── M3Layout.kt
│       │   ├── M3Input.kt
│       │   └── M3Indicator.kt
│       └── animation/               # 动画系统
│           └── Motion.kt

presentation/
├── src/main/java/com/cycling/presentation/
│   ├── home/                        # 首页
│   ├── songs/                       # 歌曲页
│   ├── albums/                      # 专辑页
│   ├── artists/                     # 艺术家页
│   ├── player/                      # 播放器
│   ├── search/                      # 搜索页
│   ├── playlist/                    # 播放列表
│   ├── folder/                      # 文件夹
│   ├── settings/                    # 设置
│   └── lyrics/                      # 歌词
```

---

## ADDED Requirements

### Requirement: Material 3 Expressive 设计令牌系统

系统 SHALL 提供完整的 Material 3 Expressive 设计令牌。

#### Scenario: 动态色彩系统
- **WHEN** 应用需要使用颜色
- **THEN** 应使用 Material 3 动态色彩系统

**色彩定义**:
```
Primary: 动态提取或默认 #6750A4
Secondary: 动态提取或默认 #625B71
Tertiary: 动态提取或默认 #7D5260
Error: #B3261E
Surface: #FFFBFE (Light) / #1C1B1F (Dark)
Surface Variant: #E7E0EC (Light) / #49454F (Dark)
Outline: #79747E
Outline Variant: #CAC4D0

语义色:
- On Primary: 白色
- On Secondary: 白色
- On Surface: #1C1B1F (Light) / #E6E1E5 (Dark)
- On Surface Variant: #49454F (Light) / #CAC4D0 (Dark)
```

#### Scenario: 强调型排版系统
- **WHEN** 应用需要显示文本
- **THEN** 应使用 Material 3 Expressive 排版样式

**字体定义**:
```
Display Large: 57sp, Regular
Display Medium: 45sp, Regular
Display Small: 36sp, Regular
Headline Large: 32sp, Regular
Headline Medium: 28sp, Regular
Headline Small: 24sp, Regular
Title Large: 22sp, Medium
Title Medium: 16sp, Medium
Title Small: 14sp, Medium
Body Large: 16sp, Regular
Body Medium: 14sp, Regular
Body Small: 12sp, Regular
Label Large: 14sp, Medium
Label Medium: 12sp, Medium
Label Small: 11sp, Medium

Expressive 增强:
- 强调标题: 加粗字重
- 行动按钮: 大号字体 (16sp+)
- 关键信息: 高对比度
```

#### Scenario: 间距系统
- **WHEN** 应用需要布局间距
- **THEN** 应使用 Material 3 间距系统

**间距定义**:
```
none: 0dp
extraSmall: 4dp
small: 8dp
medium: 16dp
large: 24dp
extraLarge: 32dp
extraExtraLarge: 48dp
extraExtraExtraLarge: 64dp

组件内边距:
- Button: 16dp 水平, 10dp 垂直
- Card: 16dp
- ListItem: 16dp 水平
- FAB: 16dp
```

#### Scenario: 形状系统
- **WHEN** 应用需要圆角
- **THEN** 应使用 Material 3 形状系统

**形状定义**:
```
Corner Small: 8dp
Corner Medium: 12dp
Corner Large: 16dp
Corner Extra Large: 28dp
Corner Full: 50% (完全圆角)

Expressive 形状变体:
- 圆形 (Circle)
- 方形 (Square)
- 圆角矩形 (Rounded)
- 药丸形 (Pill)
- 抽象形状 (Abstract)
- 支持形状变换动画
```

---

### Requirement: Material 3 Expressive 按钮组件

系统 SHALL 提供完整的 Material 3 Expressive 按钮组件。

#### Scenario: 填充按钮 (Filled Button)
- **WHEN** 需要主要操作按钮
- **THEN** 应使用 Material 3 填充按钮

**规范**:
```
形状: Corner Medium (12dp)
内边距: 16dp 水平, 10dp 垂直
最小高度: 40dp
颜色: Primary 容器色
文字: Label Large, Medium 字重
图标: 可选, 18dp
运动弹簧动画: 按压缩放 0.95x
```

#### Scenario: 扩展浮动按钮 (Extended FAB)
- **WHEN** 需要突出的主要操作
- **THEN** 应使用 Extended FAB

**规范**:
```
形状: Corner Large (16dp)
高度: 56dp (标准) / 48dp (小型)
内边距: 16dp
图标: 24dp
文字: Label Large
阴影: 6dp elevation
运动弹簧动画: 展开/收起动画
```

#### Scenario: 按钮组 (Button Group)
- **WHEN** 需要一组相关操作
- **THEN** 应使用 Button Group

**规范**:
```
排列: 水平或垂直
间距: 8dp
共享容器: 可选
分段样式: connected / separated
```

#### Scenario: 分段按钮 (Segmented Button)
- **WHEN** 需要切换选项
- **THEN** 应使用 Material 3 分段按钮

**规范**:
```
形状: Corner Full (药丸形)
选中状态: Secondary 容器色
未选中状态: Surface 色
图标: 可选
动画: 平滑切换
```

---

### Requirement: Material 3 Expressive 卡片组件

系统 SHALL 提供完整的 Material 3 Expressive 卡片组件。

#### Scenario: 填充卡片 (Filled Card)
- **WHEN** 显示重要内容
- **THEN** 应使用填充卡片

**规范**:
```
形状: Corner Medium (12dp)
背景: Surface Variant
内边距: 16dp
阴影: 无
点击: 状态涟漪效果
```

#### Scenario: elevated 卡片 (Elevated Card)
- **WHEN** 需要突出显示
- **THEN** 应使用 elevated 卡片

**规范**:
```
形状: Corner Medium (12dp)
背景: Surface
阴影: 1dp elevation (rest) / 2dp (hover)
悬停: 阴影增强
```

#### Scenario: 媒体卡片
- **WHEN** 显示专辑/播放列表
- **THEN** 应使用 Material 3 媒体卡片

**规范**:
```
宽度: 160dp (标准) / 自适应
宽高比: 1:1
形状: Corner Medium (12dp)
封面: AsyncImage
占位图: 渐变背景 + 图标
动画: 按压缩放 + 涟漪
```

#### Scenario: 艺术家卡片
- **WHEN** 显示艺术家
- **THEN** 应使用圆形头像样式

**规范**:
```
头像: 圆形 (Corner Full)
尺寸: 100dp
占位图: 渐变背景
名称: Title Medium
```

---

### Requirement: Material 3 Expressive 导航组件

系统 SHALL 提供完整的 Material 3 Expressive 导航组件。

#### Scenario: 顶部应用栏 (Top App Bar)
- **WHEN** 页面需要顶部导航
- **THEN** 应使用 Material 3 顶部应用栏

**规范**:
```
高度: 64dp (标准) / 152dp (大标题)
背景: Surface
滚动: 可隐藏或固定
标题: Title Large
操作按钮: 24dp 图标
返回按钮: 24dp 箭头图标
```

#### Scenario: 底部导航栏 (Navigation Bar)
- **WHEN** 应用需要底部导航
- **THEN** 应使用 Material 3 Navigation Bar

**规范**:
```
高度: 80dp
背景: Surface Container
项目: 3-5 个
图标: 24dp
标签: Label Medium
选中指示器: 药丸形状背景
动画: 图标缩放 + 指示器动画
```

#### Scenario: 侧边导航抽屉 (Navigation Drawer)
- **WHEN** 需要侧边导航
- **THEN** 应使用 Material 3 Navigation Drawer

**规范**:
```
宽度: 360dp (标准) / 适配屏幕
背景: Surface Container
项目: 图标 + 文字
选中状态: Secondary 容器背景
```

---

### Requirement: Material 3 Expressive 列表组件

系统 SHALL 提供完整的 Material 3 Expressive 列表组件。

#### Scenario: 列表项 (List Item)
- **WHEN** 显示列表内容
- **THEN** 应使用 Material 3 列表项

**规范**:
```
高度: 56dp (单行) / 72dp (双行) / 88dp (三行)
内边距: 16dp 水平
前导图标: 24dp, 16dp 边距
后尾图标: 24dp
分割线: 全宽或缩进
点击: 涟漪效果
```

#### Scenario: 列表项变体
- **WHEN** 需要不同类型的列表项
- **THEN** 应使用对应的变体

**变体**:
```
OneLine: 单行文字
TwoLine: 主标题 + 副标题
ThreeLine: 主标题 + 副标题 + 支持文字
WithLeadingAvatar: 带头像
WithLeadingIcon: 带图标
WithLeadingImage: 带图片
WithTrailingCheckbox: 带复选框
WithTrailingSwitch: 带开关
WithTrailingIconButton: 带操作按钮
```

---

### Requirement: Material 3 Expressive 播放器界面

系统 SHALL 提供完整的 Material 3 Expressive 播放器界面。

#### Scenario: Mini Player
- **WHEN** 正在播放音乐
- **THEN** 应在底部显示 Mini Player

**规范**:
```
高度: 64dp
背景: Surface Container High
进度条: 4dp, Primary 色, 顶部
专辑封面: 48dp, Corner Small
播放按钮: 40dp
动画: 滑入/滑出
```

#### Scenario: 全屏播放器
- **WHEN** 用户展开播放器
- **THEN** 应显示全屏播放器界面

**规范**:
```
背景: 动态模糊封面
专辑封面: 85% 宽度, Corner Large
播放控制: 64dp 圆形按钮
进度条: 4dp, 可拖动
音量/亮度滑块: 垂直滑块
动画: 从底部滑入 + 弹簧动画
手势: 下拉关闭
```

---

### Requirement: Material 3 Expressive 动画系统

系统 SHALL 提供运动弹簧动画系统。

#### Scenario: 运动弹簧动画
- **WHEN** 组件需要动画
- **THEN** 应使用物理弹簧模型

**规范**:
```
弹簧参数:
- stiffness: 200-800 (弹性刚度)
- dampingRatio: 0.6-0.9 (阻尼比)

常用动画:
- 按钮点击: scale 0.95, spring(0.8, 400)
- 卡片悬停: elevation + scale 1.02
- 页面切换: slide + fade
- 列表项进入: staggered animation
- FAB 展开: morph animation
```

#### Scenario: 共享元素转场
- **WHEN** 页面间有共享元素
- **THEN** 应使用共享元素转场

**规范**:
```
封面图片: 共享元素动画
列表项: sharedBounds 过渡
动画时长: 300-400ms
```

---

### Requirement: Material 3 Expressive 搜索界面

系统 SHALL 提供完整的 Material 3 Expressive 搜索界面。

#### Scenario: 搜索栏 (Search Bar)
- **WHEN** 用户需要搜索
- **THEN** 应显示 Material 3 搜索栏

**规范**:
```
高度: 56dp
形状: Corner Extra Large (28dp)
背景: Surface Container
图标: 搜索图标 24dp
占位符: "搜索"
展开动画: 宽度扩展
```

#### Scenario: 搜索视图 (Search View)
- **WHEN** 用户开始搜索
- **THEN** 应显示完整搜索视图

**规范**:
```
搜索历史: 列表展示
搜索建议: 实时显示
搜索结果: 分类展示
动画: 平滑展开
```

---

### Requirement: Material 3 Expressive 轮播组件

系统 SHALL 提供轮播图组件。

#### Scenario: 轮播图 (Carousel)
- **WHEN** 需要展示多个内容
- **THEN** 应使用 Carousel 组件

**规范**:
```
项目宽度: 80% 屏幕宽度 (可配置)
间距: 16dp
形状: Corner Large
指示器: 底部居中
动画: 平滑滑动 + 弹簧回弹
```

---

### Requirement: Material 3 Expressive 加载指示器

系统 SHALL 提供加载指示器组件。

#### Scenario: 圆形进度指示器
- **WHEN** 需要显示加载状态
- **THEN** 应使用圆形进度指示器

**规范**:
```
尺寸: 32dp (小) / 48dp (标准) / 64dp (大)
颜色: Primary
动画: 旋转 + 渐变
```

#### Scenario: 线性进度指示器
- **WHEN** 需要显示进度
- **THEN** 应使用线性进度指示器

**规范**:
```
高度: 4dp
颜色: Primary
背景: Surface Variant
动画: 平滑过渡
```

---

### Requirement: 深色模式支持

系统 SHALL 完美支持深色模式。

#### Scenario: 深色模式切换
- **WHEN** 系统或用户切换深色模式
- **THEN** 所有界面元素应平滑过渡

**规范**:
- 使用 Material 3 语义化颜色
- 动态色彩适配
- 平滑过渡动画

---

### Requirement: 动态色彩支持

系统 SHALL 支持动态色彩主题。

#### Scenario: 壁纸色彩提取
- **WHEN** 用户开启动态色彩
- **THEN** 应用应从壁纸提取主题色

**规范**:
- 使用 Material You 动态色彩 API
- 支持多种色彩方案
- 用户可自定义强调色

---

## MODIFIED Requirements

### Requirement: 现有组件重构

现有 iOS 风格组件 SHALL 替换为 Material 3 Expressive 组件。

**重构映射**:
| iOS 组件 | Material 3 组件 |
|---------|----------------|
| IOSFilledButton | FilledButton |
| IOSTextButton | TextButton |
| IOSSegmentedButton | SegmentedButton |
| IOSIconButton | IconButton |
| IOSMediaCard | ElevatedCard + MediaContent |
| IOSArtistCard | Card + Avatar |
| IOSListItem | ListItem |
| IOSInsetGrouped | Card + Column |
| IOSTopAppBar | TopAppBar |
| IOSLargeTitleTopAppBar | LargeTopAppBar |
| IOSScreen | Scaffold |

---

## REMOVED Requirements

### Requirement: iOS 设计令牌系统
**Reason**: 不再使用 iOS 设计风格
**Migration**: 替换为 Material 3 Expressive 设计令牌

### Requirement: iOS 风格导航组件
**Reason**: 不再使用 iOS 导航风格
**Migration**: 替换为 Material 3 导航组件

### Requirement: iOS 风格动画规范
**Reason**: 不再使用 iOS 动画风格
**Migration**: 替换为 Material 3 运动弹簧动画
