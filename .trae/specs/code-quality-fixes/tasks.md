# Tasks

## Critical 优先级

- [x] Task 1: 修复 PlayerManager 协程泄漏
  - [x] SubTask 1.1: 为 PlayerManager 添加 release() 方法取消协程作用域
  - [x] SubTask 1.2: 在 SonicApplication 中注册 ActivityLifecycleCallbacks 监听应用退出
  - [x] SubTask 1.3: 确保协程在适当时机被取消

- [x] Task 2: 修复 PlayerService 静态变量问题
  - [x] SubTask 2.1: 移除 companion object 中的 currentSong 静态变量
  - [x] SubTask 2.2: 使用 StateFlow 或 MediaSession 传递当前播放歌曲信息
  - [x] SubTask 2.3: 更新 PlayerManager 中对 PlayerService 的调用方式

## High 优先级

- [x] Task 3: 修复 DeepSeekApiServiceImpl 缺少协程调度器
  - [x] SubTask 3.1: 在 chat 方法中添加 withContext(Dispatchers.IO)
  - [x] SubTask 3.2: 确保网络请求在 IO 线程执行

- [x] Task 4: 添加 AiRepository 的 Hilt 绑定
  - [x] SubTask 4.1: 在 RepositoryModule 中添加 AiRepository 绑定

- [x] Task 5: 优化数据库迁移策略
  - [x] SubTask 5.1: 移除 fallbackToDestructiveMigration
  - [x] SubTask 5.2: 添加 Migration 策略（如需要）

## Medium 优先级

- [x] Task 6: 修复 HomeViewModel isLoading 状态问题
  - [x] SubTask 6.1: 使用 combine 或 zip 等待所有 Flow 完成
  - [x] SubTask 6.2: 正确设置加载状态

- [x] Task 7: 优化 PlayerScreen Slider 拖动体验
  - [x] SubTask 7.1: 添加临时状态存储拖动位置
  - [x] SubTask 7.2: 使用 onValueChangeFinished 执行 seekTo

- [x] Task 8: 修复 MusicScanner 进度计算错误
  - [x] SubTask 8.1: 修正批量插入时的进度计算逻辑

- [x] Task 9: 移除未使用的 Log 导入
  - [x] SubTask 9.1: 从 MediaStoreHelper 中移除未使用的 Log 导入

## Low 优先级

- [x] Task 10: 优化 PlayerScreen 队列高度适配
  - [x] SubTask 10.1: 使用动态高度计算替代硬编码 400.dp

- [x] Task 11: 统一 SongListItem 参数风格
  - [x] SubTask 11.1: 调整参数命名和默认值以保持一致性

# Task Dependencies
- Task 2 依赖 Task 1（PlayerService 重构需要 PlayerManager 支持）
- 其他任务可并行执行
