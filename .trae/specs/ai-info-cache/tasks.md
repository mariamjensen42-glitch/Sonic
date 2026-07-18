# Tasks

- [x] Task 1: 创建 AI 信息缓存数据层
  - [x] SubTask 1.1: 创建 AiInfoCacheEntity 实体类 (data层)
  - [x] SubTask 1.2: 创建 AiInfoCacheDao 接口 (data层)
  - [x] SubTask 1.3: 在 AppDatabase 中添加 AiInfoCacheEntity (data层)
  - [x] SubTask 1.4: 创建 AiInfoCacheStore 封装缓存操作 (data层)

- [x] Task 2: 扩展 AiRepository 接口与实现
  - [x] SubTask 2.1: 在 AiRepository 接口添加缓存读取方法 (domain层)
  - [x] SubTask 2.2: 在 AiRepositoryImpl 实现缓存逻辑 (data层)
    - 获取信息时先查缓存，缓存不存在再调 API
    - API 返回成功后保存到缓存

- [x] Task 3: 更新 ViewModel 缓存逻辑
  - [x] SubTask 3.1: 修改 AiInfoViewModel 优先读取缓存 (presentation层)
    - Repository 层已实现缓存优先逻辑，ViewModel 无需额外修改

# Task Dependencies
- [Task 2] 依赖 [Task 1]
- [Task 3] 依赖 [Task 2]
