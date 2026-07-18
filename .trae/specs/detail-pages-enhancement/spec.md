# 专辑/歌手详情页增强 Spec

## Why
当前专辑详情页和歌手详情页缺少常用的操作按钮（播放全部、随机播放），且歌曲列表项未显示封面，用户体验不够完善。

## What Changes
- 在头部信息和歌曲列表之间添加"播放全部"和"随机播放"两个按钮
- 歌曲列表项增加封面图片显示
- 优化歌曲列表项布局

## Impact
- Affected code:
  - `presentation/albumdetail/AlbumDetailScreen.kt` - 添加操作按钮和封面显示
  - `presentation/artistdetail/ArtistDetailScreen.kt` - 添加操作按钮和封面显示

## ADDED Requirements

### Requirement: 操作按钮
专辑详情页和歌手详情页应在头部信息和歌曲列表之间提供操作按钮。

#### Scenario: 显示操作按钮
- **WHEN** 用户进入专辑详情页或歌手详情页
- **THEN** 显示"播放全部"和"随机播放"两个按钮

#### Scenario: 点击播放全部
- **WHEN** 用户点击"播放全部"按钮
- **THEN** 从第一首歌曲开始播放列表

#### Scenario: 点击随机播放
- **WHEN** 用户点击"随机播放"按钮
- **THEN** 随机选择一首歌曲开始播放

### Requirement: 歌曲封面显示
歌曲列表项应显示歌曲封面图片。

#### Scenario: 显示封面
- **WHEN** 歌曲有封面图片
- **THEN** 在歌曲列表项左侧显示封面缩略图

#### Scenario: 无封面时显示占位图
- **WHEN** 歌曲没有封面图片
- **THEN** 显示默认的音乐图标作为占位
