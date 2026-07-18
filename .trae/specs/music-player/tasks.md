# Tasks

- [ ] Task 1: 创建播放器服务层 (data模块)
  - [ ] SubTask 1.1: 创建 PlayerRepository 接口 (domain层)
  - [ ] SubTask 1.2: 创建 PlayerService 使用 Media3 ExoPlayer (data层)
  - [ ] SubTask 1.3: 创建 PlayerManager 管理播放状态 (data层)
  - [ ] SubTask 1.4: 创建 PlayerRepositoryImpl 实现 (data层)
  - [ ] SubTask 1.5: 创建 Hilt 模块提供播放器依赖 (data层)

- [ ] Task 2: 创建播放器UI组件 (presentation模块)
  - [ ] SubTask 2.1: 创建 PlayerContract.kt (UiState, Intent, Effect)
  - [ ] SubTask 2.2: 创建 PlayerViewModel.kt
  - [ ] SubTask 2.3: 创建 MiniPlayer.kt 底部迷你播放器
  - [ ] SubTask 2.4: 创建 PlayerScreen.kt 全屏播放器页面

- [ ] Task 3: 集成播放器到主界面
  - [ ] SubTask 3.1: 更新 MainActivity 添加 MiniPlayer
  - [ ] SubTask 3.2: 更新 AppNavGraph 添加 Player 页面路由
  - [ ] SubTask 3.3: 更新 Screen.kt 添加 Player 路由定义
  - [ ] SubTask 3.4: 更新 AndroidManifest.xml 注册服务

# Task Dependencies
- [Task 2] 依赖 [Task 1]
- [Task 3] 依赖 [Task 1] 和 [Task 2]
