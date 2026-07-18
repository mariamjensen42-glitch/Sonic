# Material 3 Expressive 完整改进规格

## Why
当前项目虽然已经实现了基础的 Material 3 Expressive 组件，但在以下方面仍有改进空间：
1. 部分页面仍使用旧版组件模式，未完全遵循 M3 Expressive 设计规范
2. 动效系统可以更加生动和有意义
3. 状态层（State Layers）实现不完整
4. 颜色系统可以更充分地利用 Surface Container 层级
5. 形状系统可以更加多样化和富有表现力

## What Changes

### 1. 主题系统增强
- **新增**: 完整的 Surface Container 层级使用
- **新增**: 状态层颜色（Hover, Focus, Pressed, Drag）
- **新增**: 强调色渐变支持
- **改进**: 动态颜色过渡动画

### 2. 动效系统升级
- **新增**: Material 3 Expressive 标准缓动曲线
- **新增**: 容器变换动画（Container Transform）
- **新增**: 共享轴过渡（Shared Axis）
- **改进**: 弹簧动画参数调优

### 3. 形状系统扩展
- **新增**: 更多非对称形状变体
- **新增**: Pill 形状组件
- **新增**: 切角形状支持

### 4. 组件状态层实现
- **新增**: 所有可交互组件的状态层支持
- **新增**: 正确的 Ripple 效果
- **新增**: 焦点指示器

### 5. 页面级改进
- **重构**: SettingsScreen - 使用 M3 组件完成重构
- **重构**: SearchScreen - 使用 M3 组件完成重构
- **重构**: FolderScreen - 使用 M3 组件完成重构
- **重构**: MyMusicScreen - 使用 M3 组件完成重构
- **重构**: SongDetailScreen - 使用 M3 组件完成重构
- **重构**: AlbumDetailScreen - 使用 M3 组件完成重构
- **重构**: ArtistDetailScreen - 使用 M3 组件完成重构
- **重构**: PlaylistDetailScreen - 使用 M3 组件完成重构
- **重构**: PlaylistsScreen - 使用 M3 组件完成重构
- **重构**: AiInfoScreen - 使用 M3 组件完成重构

### 6. 无障碍改进
- **新增**: 完整的焦点导航支持
- **新增**: 语义标签优化
- **新增**: 对比度检查

## Impact

### Affected Specs
- integrate-m3-expressive (已完成，需要增强)
- m3-expressive-components (已完成，需要增强)
- material3-expressive-refactor (部分完成，需要继续)

### Affected Code
- `core/src/main/java/com/cycling/core/ui/theme/` - 主题系统
- `core/src/main/java/com/cycling/core/ui/components/` - 组件库
- `presentation/src/main/java/com/cycling/presentation/*Screen.kt` - 所有页面

## ADDED Requirements

### Requirement: 主题系统增强
The system SHALL provide a complete Material 3 Expressive theme system with:
- Full Surface Container hierarchy usage
- State layer colors for all interactive states
- Smooth dynamic color transitions
- Gradient accent color support

#### Scenario: 动态颜色切换
- **WHEN** 用户切换歌曲时
- **THEN** 主题颜色应平滑过渡到新歌曲的专辑封面颜色
- **AND** 过渡动画应使用 EmphasizedDecelerate 缓动曲线
- **AND** 持续时间应为 500ms

### Requirement: 状态层实现
The system SHALL implement state layers for all interactive components:
- Hover state: 8% opacity overlay
- Focus state: 12% opacity overlay  
- Pressed state: 12% opacity overlay
- Drag state: 16% opacity overlay

#### Scenario: 按钮按下
- **WHEN** 用户按下按钮
- **THEN** 按钮应显示 Pressed 状态层
- **AND** 状态层颜色应为 OnSurface 的 12% 透明度
- **AND** 动画持续时间应为 150ms

### Requirement: 动效系统
The system SHALL implement Material 3 Expressive motion:
- Standard easing for regular transitions
- Emphasized easing for prominent elements
- Container transform for hero animations
- Shared axis for page transitions

#### Scenario: 页面导航
- **WHEN** 用户导航到新页面
- **THEN** 应使用 Shared Axis Y 过渡动画
- **AND** 持续时间应为 300ms
- **AND** 缓动应为 EmphasizedDecelerate

### Requirement: 页面重构
The system SHALL refactor all remaining screens to use M3 Expressive components:
- SettingsScreen, SearchScreen, FolderScreen
- MyMusicScreen, SongDetailScreen
- AlbumDetailScreen, ArtistDetailScreen
- PlaylistDetailScreen, PlaylistsScreen, AiInfoScreen

#### Scenario: 设置页面显示
- **WHEN** 用户打开设置页面
- **THEN** 应使用 M3ListItem 组件
- **AND** 应使用 M3TopAppBar 组件
- **AND** 所有列表项应有正确的状态层反馈

## MODIFIED Requirements

### Requirement: 现有组件增强
All existing M3 components SHALL be enhanced with:
- Complete state layer support
- Improved spring animation parameters
- Better accessibility labels

## REMOVED Requirements

### Requirement: 旧版组件使用
**Reason**: 完全迁移到 Material 3 Expressive
**Migration**: 所有页面使用 core.ui.components 中的 M3 组件
