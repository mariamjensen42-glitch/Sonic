# Material 3 Expressive Core 组件完善规格

## Why
当前 core 模块的 M3 组件已实现基础功能，但根据 Material 3 Expressive 最新设计规范，需要新增表现型组件（按钮组、分割按钮、FAB 菜单、工具栏、垂直菜单等），并完善现有组件的 Expressive 特性（更丰富的形状变体、弹簧动画系统、Expressive 颜色等）。

## What Changes
- 新增 M3 Expressive 表现型组件：ButtonGroup、SplitButton、FABMenu、Toolbar、VerticalMenu
- 完善 DesignTokens：新增 35 种形状变体、Expressive 颜色、弹簧动画 Token
- 完善现有组件：增加 Expressive 变体和更丰富的配置选项
- 新增 M3Menu 组件文件

## Impact
- Affected specs: material3-expressive-refactor
- Affected code: 
  - `core/ui/theme/DesignTokens.kt` - 扩展设计令牌
  - `core/ui/components/M3Button.kt` - 新增 ButtonGroup、SplitButton
  - `core/ui/components/M3Navigation.kt` - 新增 Toolbar
  - `core/ui/components/` - 新增 M3Menu.kt

---

## ADDED Requirements

### Requirement: M3 Expressive 形状系统扩展

系统 SHALL 提供 35 种新形状和形状变体。

#### Scenario: 形状分类
- **WHEN** 组件需要使用形状
- **THEN** 应使用扩展的形状系统

**形状定义**:
```
基础圆角:
- Corner Small: 8dp
- Corner Medium: 12dp  
- Corner Large: 16dp
- Corner Extra Large: 28dp
- Corner Full: 50% (完全圆角)

Expressive 形状变体:
- Circle: 完全圆形
- Square: 无圆角方形
- Rounded: 标准圆角矩形
- Pill: 药丸形 (高度一半圆角)
- SoftHexagon: 柔和六边形
- Slanted: 倾斜形状
- Blob: 有机形状
- Diamond: 菱形
- Leaf: 叶片形状
- Star: 星形

动态形状:
- 支持 Shape Morphing 动画
- 状态变化时形状过渡
```

---

### Requirement: M3 Expressive 颜色系统

系统 SHALL 提供 Expressive 强调颜色。

#### Scenario: Expressive 颜色
- **WHEN** 需要更丰富的颜色表现
- **THEN** 应使用 Expressive 颜色

**颜色定义**:
```
Expressive 强调色:
- Pink: #FFD8E4
- Purple: #E8DEF8
- Blue: #D0E4FF
- Green: #C6EA7D
- Yellow: #F9DEDC
- Orange: #FFD8E4

语义 Expressive 色:
- ExpressivePrimary: 动态提取
- ExpressiveSecondary: 动态提取
- ExpressiveTertiary: 动态提取
```

---

### Requirement: M3 Expressive 弹簧动画 Token

系统 SHALL 提供标准化的弹簧动画 Token。

#### Scenario: 弹簧动画配置
- **WHEN** 组件需要动画
- **THEN** 应使用标准化的弹簧 Token

**Token 定义**:
```
空间动画 (A到B移动):
- Standard: stiffness=200, dampingRatio=0.8
- Expressive: stiffness=200, dampingRatio=0.6 (有回弹)

形变动画 (形状变化):
- Standard: stiffness=400, dampingRatio=0.8
- Expressive: stiffness=300, dampingRatio=0.6

速度预设:
- Fast: 小元素使用
- Default: 大多数元素
- Slow: 大元素使用
```

---

### Requirement: 按钮组组件 (Button Group)

系统 SHALL 提供按钮组组件。

#### Scenario: 水平按钮组
- **WHEN** 需要一组水平排列的相关操作
- **THEN** 应使用水平 Button Group

**规范**:
```
排列: 水平
间距: 8dp
共享容器: 可选
样式: connected (连接) / separated (分离)
形状: 统一圆角
动画: 按压缩放 + 涟漪
```

#### Scenario: 垂直按钮组
- **WHEN** 需要一组垂直排列的相关操作
- **THEN** 应使用垂直 Button Group

**规范**:
```
排列: 垂直
间距: 8dp
共享容器: 可选
样式: connected / separated
```

---

### Requirement: 分割按钮组件 (Split Button)

系统 SHALL 提供分割按钮组件。

#### Scenario: 分割按钮
- **WHEN** 需要主操作 + 下拉扩展
- **THEN** 应使用 Split Button

**规范**:
```
结构: [主按钮] [下拉箭头]
形状: Corner Medium (12dp)
主按钮: 可自定义图标和文字
下拉: 展开菜单
动画: 展开动画 + 弹簧效果
```

---

### Requirement: FAB 菜单组件

系统 SHALL 提供 FAB 菜单组件。

#### Scenario: 展开 FAB 菜单
- **WHEN** FAB 点击后需要显示多个操作
- **THEN** 应展开 FAB 菜单

**规范**:
```
展开方向: 向上 / 向左
项目: 2-5 个
动画: 弹簧展开 + 交错动画
形状: FAB 圆角 + 菜单项圆角
标签: 可选显示文字标签
```

---

### Requirement: 工具栏组件 (Toolbar)

系统 SHALL 提供工具栏组件。

#### Scenario: 底部工具栏
- **WHEN** 需要底部操作栏
- **THEN** 应使用 Toolbar

**规范**:
```
高度: 56dp
背景: Surface Container
项目: 图标按钮
间距: 均匀分布
动画: 按钮交互反馈
```

---

### Requirement: 垂直菜单组件 (Vertical Menu)

系统 SHALL 提供垂直菜单组件。

#### Scenario: 垂直菜单
- **WHEN** 需要垂直菜单列表
- **THEN** 应使用 Vertical Menu

**规范**:
```
形状: 更圆润的圆角
分隔线: 不占满整个宽度
间隙选项: 可选，用于分组
选中状态: Secondary 容器背景
子菜单: 支持展开动画
```

#### Scenario: 带间隙菜单
- **WHEN** 需要分组菜单项
- **THEN** 应使用带间隙选项

**规范**:
```
间隙: 分组项之间
分隔线: 可选搭配
使用指南:
- 不改变间隙大小
- 每个菜单间隙不超过 1-2 个
- 不在可滚动菜单中使用间隙
```

---

### Requirement: 菜单颜色风格

系统 SHALL 提供标准菜单和活力菜单风格。

#### Scenario: 标准菜单
- **WHEN** 需要低视觉强调的菜单
- **THEN** 应使用标准菜单

**规范**:
```
背景: Surface
视觉强调: 较低
适用: 常规操作菜单
```

#### Scenario: 活力菜单
- **WHEN** 需要高视觉强调的菜单
- **THEN** 应使用活力菜单

**规范**:
```
背景: Tertiary 容器色
视觉强调: 较高
适用: 重要操作菜单
注意: 谨慎使用
```

---

### Requirement: 轮播组件 (Carousel)

系统 SHALL 提供轮播组件。

#### Scenario: 横向轮播
- **WHEN** 需要展示多个卡片内容
- **THEN** 应使用 Carousel

**规范**:
```
项目宽度: 80% 屏幕宽度 (可配置)
间距: 16dp
形状: Corner Large
指示器: 底部居中，圆点样式
动画: 平滑滑动 + 弹簧回弹
预览: 可见前后项目边缘
```

---

### Requirement: 容器组件 (Container)

系统 SHALL 提供容器组件用于分组。

#### Scenario: 分组容器
- **WHEN** 需要将相似信息分组
- **THEN** 应使用 Container 组件

**规范**:
```
形状: Corner Medium / Large
背景: Surface Container
边框: 可选 Outline
内边距: 16dp
标题: 可选
```

---

## MODIFIED Requirements

### Requirement: 现有组件 Expressive 增强

现有组件 SHALL 增加 Expressive 变体。

**增强内容**:
| 组件 | 新增内容 |
|------|----------|
| M3FilledButton | Expressive 变体，更大尺寸选项 |
| M3ExtendedFAB | 多种尺寸 (Small/Standard/Large)，更方正轮廓 |
| M3MediaCard | 支持形状变体，Expressive 动画 |
| M3ArtistCard | 支持多种头像形状 |
| M3ListItem | 支持更丰富的尾部组件 |
| M3TopAppBar | 支持 CenterAligned 变体 |
| M3NavigationBar | 更圆润的选中指示器 |

---

## REMOVED Requirements

无移除内容。
