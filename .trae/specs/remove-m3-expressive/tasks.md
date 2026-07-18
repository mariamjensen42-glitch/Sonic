# Tasks

## Phase 1: 清理主题文件

- [x] Task 1: 简化 Typography.kt
  - [x] SubTask 1.1: 移除 ExpressiveTypography 对象

- [x] Task 2: 简化 DesignTokens.kt
  - [x] SubTask 2.1: 移除 M3ExpressiveShapes 对象
  - [x] SubTask 2.2: 移除 M3SpringTokens 对象
  - [x] SubTask 2.3: 移除 M3ExpressiveComponentSize 对象

## Phase 2: 简化 M3 组件

- [x] Task 3: 简化 M3Button.kt
  - [x] SubTask 3.1: 移除对 M3ExpressiveComponentSize 的引用
  - [x] SubTask 3.2: 移除 M3SpringTokens 引用，使用 M3Motion

- [x] Task 4: 简化 M3Card.kt
  - [x] SubTask 4.1: 移除 expressive 参数
  - [x] SubTask 4.2: 简化动画效果

- [x] Task 5: 简化 M3Navigation.kt
  - [x] SubTask 5.1: 移除 Expressive 相关样式

- [x] Task 6: 简化 M3ListItem.kt
  - [x] SubTask 6.1: 移除 Expressive 相关样式

- [x] Task 7: 简化 M3Carousel.kt
  - [x] SubTask 7.1: 移除 Expressive 相关样式

- [x] Task 8: 简化 M3Indicator.kt
  - [x] SubTask 8.1: 移除 Expressive 相关样式

## Phase 3: 更新页面引用

- [x] Task 9: 更新 HomeScreen.kt
- [x] Task 10: 更新 SongComponents.kt
- [x] Task 11: 更新 SettingsScreen.kt
- [x] Task 12: 更新 PlaylistDetailScreen.kt
- [x] Task 13: 更新 ArtistDetailScreen.kt
- [x] Task 14: 更新 SearchScreen.kt
- [x] Task 15: 更新 AlbumDetailScreen.kt
- [x] Task 16: 更新 SongDetailScreen.kt
- [x] Task 17: 更新 MyMusicScreen.kt
- [x] Task 18: 更新 FolderScreen.kt
- [x] Task 19: 更新 AiInfoScreen.kt
- [x] Task 20: 更新 AiPlaylistCreationDialog.kt

---

# Task Dependencies

- Task 3-8 依赖 Task 1-2 (主题文件清理)
- Task 9-20 依赖 Task 3-8 (组件简化)
- Task 3-8 可并行进行
- Task 9-20 可并行进行
