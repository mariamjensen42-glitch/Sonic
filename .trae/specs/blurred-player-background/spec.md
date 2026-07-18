# 播放界面模糊背景 Spec

## Why
当前播放界面使用纯色背景，视觉效果较为单调。将封面模糊后作为播放界面背景可以提升视觉沉浸感，使播放界面更具个性化和美观性，类似于主流音乐播放器的设计风格。

## What Changes
- 在 PlayerScreen 中添加模糊背景层
- 使用当前播放歌曲的封面图片作为背景
- 应用高斯模糊效果处理背景图片
- 添加半透明遮罩层确保前景内容可读性

## Impact
- Affected code:
  - `presentation/src/main/java/com/cycling/presentation/player/PlayerScreen.kt` - 添加模糊背景组件
- Affected specs:
  - music-player - 播放界面视觉增强

## ADDED Requirements

### Requirement: 模糊背景显示
系统应在播放界面显示当前歌曲封面的模糊版本作为背景。

#### Scenario: 有封面图片时显示模糊背景
- **WHEN** 当前播放歌曲有封面图片
- **THEN** 在播放界面背景显示该封面图片的模糊版本

#### Scenario: 无封面图片时显示默认背景
- **WHEN** 当前播放歌曲无封面图片
- **THEN** 显示默认的纯色背景或渐变背景

#### Scenario: 歌曲切换时背景平滑过渡
- **WHEN** 切换到下一首歌曲
- **THEN** 背景图片平滑过渡到新歌曲的封面

### Requirement: 背景模糊效果
系统应对背景图片应用适当的模糊效果。

#### Scenario: 应用高斯模糊
- **WHEN** 显示封面背景
- **THEN** 应用足够程度的高斯模糊效果（模糊半径约 20-30dp）

#### Scenario: 背景图片缩放填充
- **WHEN** 显示封面背景
- **THEN** 图片应填充整个屏幕，保持宽高比并进行中心裁剪

### Requirement: 前景内容可读性
系统应确保播放界面上的文字和控制按钮在模糊背景上清晰可见。

#### Scenario: 添加半透明遮罩
- **WHEN** 显示模糊背景
- **THEN** 在背景和前景之间添加半透明深色遮罩层

#### Scenario: 保持原有 UI 布局
- **WHEN** 启用模糊背景
- **THEN** 播放界面的原有布局和交互保持不变
