# 音乐播放功能 Spec

## Why
当前应用只能浏览音乐库，无法播放音乐。需要实现完整的音乐播放功能，包括后台播放、播放控制和播放器UI。

## What Changes
- 创建 PlayerService 使用 Media3 ExoPlayer 实现后台播放 (data层)
- 创建 PlayerManager 管理播放器状态 (data层)
- 创建 MiniPlayer 底部迷你播放器组件 (presentation层)
- 创建 PlayerScreen 全屏播放器页面 (presentation层)
- 更新导航和主界面布局

## Impact
- Affected code:
  - `data/src/main/java/com/cycling/data/player/` - 新建播放器服务和状态管理
  - `domain/src/main/java/com/cycling/domain/repository/PlayerRepository.kt` - 新建播放器仓库接口
  - `presentation/src/main/java/com/cycling/presentation/player/` - 新建播放器UI组件
  - `presentation/src/main/java/com/cycling/presentation/navigation/AppNavGraph.kt` - 更新导航
  - `app/src/main/AndroidManifest.xml` - 注册服务

## ADDED Requirements

### Requirement: 后台播放服务
系统应提供后台播放服务，支持在后台持续播放音乐。

#### Scenario: 启动播放服务
- **WHEN** 用户开始播放音乐
- **THEN** 启动前台服务并显示通知

#### Scenario: 通知控制
- **WHEN** 用户在通知栏点击播放/暂停/上一首/下一首
- **THEN** 执行对应的播放控制操作

### Requirement: 播放器状态管理
系统应管理播放器状态，包括当前歌曲、播放队列、播放进度等。

#### Scenario: 播放单首歌曲
- **WHEN** 用户点击某首歌曲
- **THEN** 开始播放该歌曲

#### Scenario: 播放列表
- **WHEN** 用户点击"播放全部"
- **THEN** 将当前列表加入播放队列并从第一首开始播放

#### Scenario: 播放进度
- **WHEN** 歌曲正在播放
- **THEN** 实时更新播放进度

### Requirement: 迷你播放器
系统应在页面底部显示迷你播放器。

#### Scenario: 显示迷你播放器
- **WHEN** 有歌曲正在播放或暂停
- **THEN** 在页面底部显示迷你播放器，包含封面、歌曲名、歌手名、播放/暂停按钮

#### Scenario: 迷你播放器控制
- **WHEN** 用户点击迷你播放器的播放/暂停按钮
- **THEN** 切换播放状态

#### Scenario: 展开播放器
- **WHEN** 用户点击迷你播放器
- **THEN** 展开全屏播放器页面

### Requirement: 全屏播放器页面
系统应提供全屏播放器页面，显示完整的播放控制。

#### Scenario: 显示播放器页面
- **WHEN** 用户展开播放器
- **THEN** 显示大封面、歌曲信息、进度条、播放控制按钮

#### Scenario: 进度条拖动
- **WHEN** 用户拖动进度条
- **THEN** 跳转到对应播放位置

#### Scenario: 上一首/下一首
- **WHEN** 用户点击上一首/下一首按钮
- **THEN** 播放上一首/下一首歌曲

#### Scenario: 循环模式
- **WHEN** 用户切换循环模式
- **THEN** 在单曲循环、列表循环、随机播放之间切换

### Requirement: 播放队列
系统应支持查看和管理播放队列。

#### Scenario: 显示播放队列
- **WHEN** 用户点击队列按钮
- **THEN** 显示当前播放队列

#### Scenario: 从队列移除
- **WHEN** 用户从队列中移除某首歌曲
- **THEN** 该歌曲从队列中移除
