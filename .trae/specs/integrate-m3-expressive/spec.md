# 整合 M3 Expressive 组件和动态颜色系统 Spec

## Why
已完成 M3 Expressive 组件库和动态颜色系统的开发，现在需要将这些组件整合到应用的各个页面中，实现完整的 Material 3 Expressive 设计体验。

## What Changes
- 在应用主题中使用 M3DynamicTheme/M3ManagedTheme 替代现有主题
- 在各个页面中使用新的 M3 Expressive 组件
- 实现基于专辑封面的动态颜色提取
- 添加主题设置页面

## Impact
- Affected specs: m3-expressive-components, material3-expressive-refactor
- Affected code: 
  - `app/MainActivity.kt` - 主题切换
  - `app/ui/screens/` - 各页面组件替换
  - `core/ui/components/` - 组件使用

---

## ADDED Requirements

### Requirement: 应用主题整合

应用 SHALL 使用 M3ManagedTheme 作为主题系统。

#### Scenario: 主题初始化
- **WHEN** 应用启动时
- **THEN** 应初始化 M3ThemeManager 并使用 M3ManagedTheme

#### Scenario: 动态颜色切换
- **WHEN** 用户切换主题颜色
- **THEN** 应通过 M3ThemeManager 更新主题状态

### Requirement: 播放器动态颜色

播放器 SHALL 根据当前播放专辑封面动态调整颜色。

#### Scenario: 专辑封面颜色提取
- **WHEN** 播放新歌曲时
- **THEN** 应从专辑封面提取主色调并应用为主题种子颜色

#### Scenario: 颜色过渡动画
- **WHEN** 主题颜色变化时
- **THEN** 应平滑过渡到新颜色

### Requirement: 页面组件替换

各页面 SHALL 使用新的 M3 Expressive 组件。

#### Scenario: 首页组件替换
- **WHEN** 渲染首页
- **THEN** 应使用 M3Carousel 展示最近播放、M3Container 组织内容分组

#### Scenario: 歌曲列表组件替换
- **WHEN** 渲染歌曲列表
- **THEN** 应使用 M3ListItemExpressive 替代原有列表项

#### Scenario: 底部导航栏
- **WHEN** 渲染底部导航
- **THEN** 应使用增强后的 M3NavigationBar

### Requirement: 主题设置页面

应用 SHALL 提供主题设置页面。

#### Scenario: 主题预设选择
- **WHEN** 用户进入设置页面
- **THEN** 应显示 M3ThemePresets 供选择

#### Scenario: 对比度调整
- **WHEN** 用户调整对比度
- **THEN** 应通过 M3ThemeManager 更新对比度等级

#### Scenario: 深色模式切换
- **WHEN** 用户切换深色模式
- **THEN** 应通过 M3ThemeManager 更新深色模式设置

---

## MODIFIED Requirements

### Requirement: MainActivity 主题设置

MainActivity SHALL 初始化并使用 M3ManagedTheme。

### Requirement: MiniPlayer 组件

MiniPlayer SHALL 使用 M3ExpressiveTheme 支持动态颜色。

---

## REMOVED Requirements

无移除内容。
