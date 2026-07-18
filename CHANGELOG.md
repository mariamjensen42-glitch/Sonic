# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2025-02-21

### Added
- 🎵 音乐播放器核心功能
  - 支持本地音乐扫描和播放
  - 支持歌词显示（LRC、TTML 格式）
  - 支持播放队列管理
  - 支持后台播放和媒体通知控制
- 📁 音乐库管理
  - 按歌曲、专辑、艺术家分类浏览
  - 播放列表创建和管理
  - 收藏功能
  - 最近播放和最多播放记录
- 🔍 搜索功能
  - 全局搜索歌曲、专辑、艺术家
  - 搜索历史记录
- 🎨 UI/UX
  - iOS 风格界面设计
  - 深色/浅色主题支持
  - 卡拉OK式歌词显示
- 🤖 AI 功能
  - DeepSeek AI 集成
  - 歌曲信息智能展示
- 📊 统计功能
  - 音乐库统计
  - 播放次数统计

### Technical
- Clean Architecture + MVI 模式
- Jetpack Compose UI
- Hilt 依赖注入
- Room 数据库
- Media3 ExoPlayer
