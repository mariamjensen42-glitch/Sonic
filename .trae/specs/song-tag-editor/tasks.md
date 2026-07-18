# Tasks

- [x] Task 1: 扩展 AudioMetadataHelper 支持写入 ID3v2 标签
  - [x] SubTask 1.1: 在 AudioMetadataHelper 中添加 updateMp3Tags 方法
  - [x] SubTask 1.2: 创建 TagUpdate 数据类封装需要更新的标签字段
  - [x] SubTask 1.3: 实现写入标题、艺术家、专辑、年份、流派、作曲字段

- [x] Task 2: 扩展 Domain 层接口
  - [x] SubTask 2.1: 在 domain 模块创建 TagUpdate 模型
  - [x] SubTask 2.2: 在 AudioMetadataRepository 添加 updateAudioTags 方法
  - [x] SubTask 2.3: 在 SongRepository 添加 updateSongInfo 方法

- [x] Task 3: 实现 Data 层逻辑
  - [x] SubTask 3.1: 实现 AudioMetadataRepositoryImpl.updateAudioTags
  - [x] SubTask 3.2: 实现 SongRepositoryImpl.updateSongInfo
  - [x] SubTask 3.3: 在 SongDao 添加更新歌曲基本信息的方法

- [x] Task 4: 创建标签编辑界面
  - [x] SubTask 4.1: 创建 TagEditorScreen Composable
  - [x] SubTask 4.2: 创建 TagEditorContract (UiState, Intent, Effect)
  - [x] SubTask 4.3: 创建 TagEditorViewModel 处理编辑逻辑
  - [x] SubTask 4.4: 实现未保存修改的确认对话框

- [x] Task 5: 添加编辑入口
  - [x] SubTask 5.1: 在 SongDetailScreen 添加编辑按钮
  - [x] SubTask 5.2: 添加导航到标签编辑页面的路由

- [x] Task 6: 集成测试与验证
  - [x] SubTask 6.1: 验证标签写入文件成功
  - [x] SubTask 6.2: 验证数据库同步更新
  - [x] SubTask 6.3: 验证 UI 正确显示更新后的标签

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 2]
- [Task 4] depends on [Task 2]
- [Task 5] depends on [Task 4]
- [Task 6] depends on [Task 3, Task 5]
