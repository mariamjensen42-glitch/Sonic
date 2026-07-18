# Checklist

## Phase 1-4: 已完成

- [x] core 模块已配置 Compose 依赖
- [x] core/ui 主题系统已创建
- [x] core/ui 组件库已创建 (Button, Card, ListItem, Navigation, Layout, Input, Indicator)
- [x] core/ui 动画系统已创建
- [x] MiniPlayer 已重构
- [x] PlayerScreen 已重构
- [x] HomeScreen 已重构
- [x] SongsScreen 已重构
- [x] AlbumsScreen 已重构
- [x] ArtistsScreen 已重构
- [x] iOS 风格组件已删除

## Phase 5: 剩余页面重构

### SettingsScreen
- [ ] 无 presentation.theme 引用
- [ ] 使用 M3ListItem 组件
- [ ] 使用 M3TopAppBar 组件

### SearchScreen
- [ ] 无 presentation.theme 引用
- [ ] 使用 M3SearchBar 组件
- [ ] 使用 M3ListItem 组件

### FolderScreen
- [ ] 无 IOSCenteredContent 引用
- [ ] 无 IOSListItem 引用
- [ ] 无 IOSScreenWithTopBar 引用
- [ ] 无 presentation.theme 引用
- [ ] 使用 M3 组件

### MyMusicScreen
- [ ] 无 IOS* 组件引用
- [ ] 无 presentation.theme 引用
- [ ] 使用 M3 组件

### SongDetailScreen
- [ ] 无 IOS* 组件引用
- [ ] 无 presentation.theme 引用
- [ ] 使用 M3 组件

### AlbumDetailScreen
- [ ] 无 IOS* 组件引用
- [ ] 无 presentation.theme 引用
- [ ] 使用 M3 组件

### ArtistDetailScreen
- [ ] 无 IOS* 组件引用
- [ ] 无 presentation.theme 引用
- [ ] 使用 M3 组件

### PlaylistDetailScreen
- [ ] 无 IOS* 组件引用
- [ ] 无 presentation.theme 引用
- [ ] 使用 M3 组件

### PlaylistsScreen
- [ ] 无 IOS* 组件引用
- [ ] 使用 M3 组件

### AiInfoScreen
- [ ] 无 presentation.theme 引用
- [ ] 使用 M3 组件

### 其他组件
- [ ] KaraokeLyricsView 无 presentation.theme 引用
- [ ] SongComponents 无 presentation.theme 引用
- [ ] AiPlaylistCreationDialog 无 IOS* 引用
- [ ] IOSNavAnimations 已删除或重构

## Phase 6: 清理验证

- [ ] 全局无 presentation.theme 引用
- [ ] 全局无 IOS* 组件引用
- [ ] 未使用的旧组件文件已删除

## 整体验证

- [ ] 所有页面深色模式显示正确
- [ ] 所有组件动画流畅
- [ ] 无编译错误
- [ ] 无运行时崩溃
- [ ] UI 符合 Material 3 Expressive 设计规范
- [ ] presentation 模块正确依赖 core 模块
