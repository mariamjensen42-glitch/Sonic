# Tasks

- [x] Task 1: 迁移歌词 UI 工具类到 presentation/lyrics/utils
  - [x] SubTask 1.1: 迁移 String.kt（字符判断工具）
  - [x] SubTask 1.2: 迁移 Color.kt（颜色工具）
  - [x] SubTask 1.3: 迁移 Brush.kt（画笔工具）
  - [x] SubTask 1.4: 迁移 easing/CubicBezierEasing.kt
  - [x] SubTask 1.5: 迁移 easing/NewtonPolynomialInterpolationEasing.kt
  - [x] SubTask 1.6: 迁移 modifier/DynamicFadeEdge.kt

- [x] Task 2: 迁移歌词布局计算类到 presentation/lyrics/components
  - [x] SubTask 2.1: 迁移 LyricsLayoutCalculator.kt（音节布局计算）
  - [x] SubTask 2.2: 迁移 LyricsLineItem.kt（歌词行组件，替换 ContinuousRoundedRectangle）

- [x] Task 3: 迁移歌词显示组件到 presentation/lyrics/components
  - [x] SubTask 3.1: 迁移 SyncedLineText.kt（同步行文本）
  - [x] SubTask 3.2: 迁移 KaraokeLineText.kt（卡拉OK行文本）
  - [x] SubTask 3.3: 迁移 KaraokeBreathingDots.kt（呼吸点动画）
  - [x] SubTask 3.4: 迁移 KaraokeLyricsView.kt（主歌词视图）

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 1, Task 2]
