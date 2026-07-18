# 代码质量修复 Spec

## Why
项目审查发现了 12 个问题，包括内存泄漏、潜在的 ANR、依赖注入缺失、UI 体验问题等，需要系统性修复以提升应用稳定性和用户体验。

## What Changes
- 修复 PlayerManager 协程泄漏问题
- 修复 PlayerService 静态变量问题
- 修复 DeepSeekApiServiceImpl 缺少协程调度器问题
- 优化数据库迁移策略
- 添加 AiRepository 的 Hilt 绑定
- 修复 HomeViewModel isLoading 状态问题
- 优化 PlayerScreen Slider 拖动体验
- 修复 MusicScanner 进度计算错误
- 移除未使用的 Log 导入
- 优化 PlayerScreen 队列高度适配
- 统一 SongListItem 参数风格

## Impact
- Affected code: 
  - `data/player/PlayerManager.kt`
  - `data/player/PlayerService.kt`
  - `data/api/DeepSeekApiServiceImpl.kt`
  - `data/di/DatabaseModule.kt`
  - `data/di/RepositoryModule.kt`
  - `presentation/home/HomeViewModel.kt`
  - `presentation/player/PlayerScreen.kt`
  - `data/local/scanner/MusicScanner.kt`
  - `data/local/mediastore/MediaStoreHelper.kt`
  - `presentation/components/SongComponents.kt`

## ADDED Requirements

### Requirement: 协程生命周期管理
系统应正确管理协程生命周期，避免内存泄漏。

#### Scenario: PlayerManager 释放
- **WHEN** PlayerManager 不再需要时
- **THEN** 所有相关协程应被正确取消

### Requirement: 网络请求线程安全
网络请求应在 IO 调度器上执行，避免阻塞主线程。

#### Scenario: API 调用
- **WHEN** 调用 DeepSeek API
- **THEN** 请求应在 IO 线程执行

### Requirement: 依赖注入完整性
所有 Repository 实现都应正确绑定到 Hilt 模块。

#### Scenario: AiRepository 注入
- **WHEN** 请求 AiRepository
- **THEN** 应正确注入 AiRepositoryImpl

### Requirement: UI 进度条交互优化
进度条拖动应在松手后才执行跳转，避免频繁 seek。

#### Scenario: 播放进度拖动
- **WHEN** 用户拖动进度条
- **THEN** 只在拖动结束时执行 seekTo

## MODIFIED Requirements

### Requirement: 数据库迁移
数据库版本升级时应保留用户数据，而非删除重建。

### Requirement: 加载状态管理
加载状态应在所有数据加载完成后才设置为 false。

## REMOVED Requirements
无
