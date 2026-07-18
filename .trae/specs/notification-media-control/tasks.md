# Tasks

- [x] Task 1: 增强通知栏播放控制
  - [x] SubTask 1.1: 在 PlayerService 中添加自定义通知布局，包含上一首、播放/暂停、下一首按钮
  - [x] SubTask 1.2: 为每个控制按钮创建 PendingIntent
  - [x] SubTask 1.3: 创建 NotificationActionReceiver 广播接收器处理按钮点击
  - [x] SubTask 1.4: 在 AndroidManifest.xml 中注册广播接收器

- [x] Task 2: 添加专辑封面到通知
  - [x] SubTask 2.1: 加载专辑封面图片并设置到通知的大图标
  - [x] SubTask 2.2: 处理无专辑封面情况，显示默认图标

- [x] Task 3: 配置锁屏播放控制
  - [x] SubTask 3.1: 设置通知的 visibility 为 VISIBILITY_PUBLIC
  - [x] SubTask 3.2: 使用 MediaStyle 设置锁屏显示样式
  - [x] SubTask 3.3: 配置 MediaSession 支持锁屏控制

- [x] Task 4: 测试和验证
  - [x] SubTask 4.1: 测试通知栏控制按钮功能
  - [x] SubTask 4.2: 测试锁屏界面播放控制
  - [x] SubTask 4.3: 测试专辑封面显示

# Task Dependencies
- Task 2 依赖 Task 1（通知布局需要先完成）
- Task 3 依赖 Task 1 和 Task 2
- Task 4 依赖所有前置任务
