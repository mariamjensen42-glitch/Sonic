# 移除 Material 3 Expressive 风格 Spec

## Why
用户不再需要 Material 3 Expressive 风格，希望回归更简单、更原生的 Material 3 设计。Expressive 风格增加了不必要的复杂性和自定义组件，维护成本高。

## What Changes
- 移除 `ExpressiveTypography` 对象
- 移除 `M3ExpressiveShapes` 对象
- 移除 `M3SpringTokens` 对象
- 移除 `M3ExpressiveComponentSize` 对象
- 简化 M3 组件，移除 Expressive 相关参数和动画
- 清理页面中对 Expressive 组件的引用

## Impact
- Affected specs: integrate-m3-expressive, m3-expressive-components, m3-expressive-complete-improvement, material3-expressive-refactor
- Affected code:
  - `core/src/main/java/com/cycling/core/ui/theme/Typography.kt`
  - `core/src/main/java/com/cycling/core/ui/theme/DesignTokens.kt`
  - `core/src/main/java/com/cycling/core/ui/components/M3Button.kt`
  - `core/src/main/java/com/cycling/core/ui/components/M3Card.kt`
  - `core/src/main/java/com/cycling/core/ui/components/M3Navigation.kt`
  - `core/src/main/java/com/cycling/core/ui/components/M3ListItem.kt`
  - `core/src/main/java/com/cycling/core/ui/components/M3Carousel.kt`
  - `presentation/src/main/java/com/cycling/presentation/` 下多个页面

## ADDED Requirements

### Requirement: 简化 Typography
系统 SHALL 只保留标准 Material 3 Typography 定义，移除 ExpressiveTypography 对象。

#### Scenario: Typography 简化完成
- **WHEN** 查看 Typography.kt 文件
- **THEN** 只存在 M3Typography 标准 Material 3 字体定义
- **AND** ExpressiveTypography 对象已被移除

### Requirement: 简化 DesignTokens
系统 SHALL 移除所有 Expressive 相关的设计令牌。

#### Scenario: DesignTokens 简化完成
- **WHEN** 查看 DesignTokens.kt 文件
- **THEN** M3ExpressiveShapes 对象已被移除
- **AND** M3SpringTokens 对象已被移除
- **AND** M3ExpressiveComponentSize 对象已被移除
- **AND** 保留基础的 M3Spacing, M3Shapes, M3Elevation, M3ComponentSize, M3Motion, M3Alpha, M3StateLayers, M3Easing, M3Duration

### Requirement: 简化 M3 组件
系统 SHALL 简化自定义 M3 组件，移除 Expressive 特有的动画和样式。

#### Scenario: 组件简化完成
- **WHEN** 查看 M3 组件文件
- **THEN** 组件使用标准 Material 3 样式
- **AND** 移除复杂的弹簧动画参数
- **AND** 移除 Expressive 相关参数

### Requirement: 更新页面引用
系统 SHALL 更新所有页面，移除对 Expressive 组件和样式的引用。

#### Scenario: 页面更新完成
- **WHEN** 查看各页面文件
- **THEN** 页面使用简化后的 M3 组件
- **AND** 无 Expressive 相关引用

## REMOVED Requirements

### Requirement: Expressive Typography
**Reason**: 用户不再需要 Expressive 风格
**Migration**: 使用标准 Material 3 Typography

### Requirement: Expressive Shapes
**Reason**: 用户不再需要复杂的形状变体
**Migration**: 使用标准 M3Shapes

### Requirement: Expressive Animations
**Reason**: 用户希望更简单的交互
**Migration**: 使用标准 M3Motion 动画
