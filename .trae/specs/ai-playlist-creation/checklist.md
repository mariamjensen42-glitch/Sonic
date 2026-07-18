# Checklist

## AI Repository 扩展
- [x] AiRepository 接口包含 `generatePlaylistByTheme` 方法
- [x] AiRepositoryImpl 正确实现主题歌单生成
- [x] AI prompt 能够有效分析歌曲与主题的匹配度

## Playlist Repository 扩展
- [x] PlaylistRepository 接口包含 `addSongsToPlaylist` 方法
- [x] 批量添加歌曲功能正常工作

## UI 组件
- [x] AiPlaylistCreationDialog 组件正确显示
- [x] 随机模式和主题模式切换正常
- [x] 歌曲数量选择器工作正常
- [x] 主题输入框工作正常
- [x] 歌单预览界面正确显示歌曲列表

## ViewModel 逻辑
- [x] 随机创建歌单逻辑正确实现
- [x] 主题创建歌单逻辑正确实现
- [x] 歌单预览和确认流程正常
- [x] 错误处理完善（API Key 未配置、无匹配歌曲等）

## 页面集成
- [x] PlaylistsScreen 显示 AI 创建按钮
- [x] 点击按钮正确打开创建对话框
- [x] API Key 未配置时显示提示
- [x] 创建成功后刷新播放列表
