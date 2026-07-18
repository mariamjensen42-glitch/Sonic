# 播放统计与收藏功能 Spec

## Why
用户需要收藏喜欢的歌曲、查看播放历史和播放统计数据，以更好地管理和发现音乐。当前应用缺少这些核心功能，无法满足用户个性化音乐体验需求。

## What Changes
- 新增歌曲收藏/喜欢功能，用户可标记喜爱的歌曲
- 新增播放统计功能，记录每首歌曲的播放次数
- 新增播放历史记录功能，记录用户播放过的歌曲
- 首页新增"喜欢的歌曲"和"最近播放"入口
- 数据库新增收藏标记、播放次数、最后播放时间等字段

## Impact
- Affected code:
  - `data/local/entity/SongEntity.kt` - 添加 isFavorite, playCount, lastPlayedAt 字段
  - `data/local/dao/SongDao.kt` - 添加收藏、统计相关查询方法
  - `data/local/database/AppDatabase.kt` - 数据库版本升级
  - `domain/model/Song.kt` - 添加收藏、统计字段
  - `domain/repository/SongRepository.kt` - 添加收藏、统计相关接口
  - `data/repository/SongRepositoryImpl.kt` - 实现收藏、统计逻辑
  - `domain/usecase/` - 新增 ToggleFavoriteUseCase, GetFavoriteSongsUseCase, GetMostPlayedSongsUseCase, GetRecentlyPlayedSongsUseCase
  - `presentation/player/PlayerScreen.kt` - 添加收藏按钮
  - `presentation/player/PlayerContract.kt` - 添加收藏相关 Intent
  - `presentation/player/PlayerViewModel.kt` - 处理收藏逻辑
  - `presentation/home/HomeScreen.kt` - 添加"喜欢的歌曲"和"最近播放"入口
  - `presentation/home/HomeContract.kt` - 添加相关状态
  - `presentation/home/HomeViewModel.kt` - 加载收藏歌曲和最近播放
  - `presentation/favorites/` - 新建收藏歌曲页面
  - `presentation/recentlyplayed/` - 新建最近播放页面
  - `data/player/PlayerManager.kt` - 播放时更新播放次数和最后播放时间

## ADDED Requirements

### Requirement: 歌曲收藏功能
系统应允许用户收藏/取消收藏歌曲。

#### Scenario: 显示收藏状态
- **WHEN** 用户在播放器界面查看当前播放歌曲
- **THEN** 显示收藏按钮（心形图标）
- **AND** 根据歌曲收藏状态显示填充或空心图标

#### Scenario: 收藏歌曲
- **WHEN** 用户点击未收藏歌曲的收藏按钮
- **THEN** 歌曲被标记为收藏
- **AND** 收藏按钮变为填充状态
- **AND** 显示"已添加到喜欢"提示

#### Scenario: 取消收藏
- **WHEN** 用户点击已收藏歌曲的收藏按钮
- **THEN** 歌曲取消收藏标记
- **AND** 收藏按钮变为空心状态
- **AND** 显示"已从喜欢移除"提示

#### Scenario: 收藏列表查看
- **WHEN** 用户在首页点击"喜欢的歌曲"
- **THEN** 导航到收藏歌曲列表页
- **AND** 显示所有已收藏的歌曲
- **AND** 支持播放全部歌曲

### Requirement: 播放统计功能
系统应记录每首歌曲的播放次数。

#### Scenario: 播放次数记录
- **WHEN** 歌曲播放完成（播放进度超过 50%）
- **THEN** 该歌曲播放次数加 1
- **AND** 更新最后播放时间

#### Scenario: 最常播放列表
- **WHEN** 用户在首页查看"最常播放"
- **THEN** 显示按播放次数降序排列的歌曲列表
- **AND** 每首歌曲显示播放次数

#### Scenario: 播放次数显示
- **WHEN** 用户查看歌曲详情或列表
- **THEN** 可选显示该歌曲的播放次数

### Requirement: 播放历史记录
系统应记录用户的播放历史。

#### Scenario: 记录播放历史
- **WHEN** 用户开始播放一首歌曲
- **THEN** 该歌曲被记录到播放历史
- **AND** 更新最后播放时间

#### Scenario: 最近播放列表
- **WHEN** 用户在首页点击"最近播放"
- **THEN** 导航到最近播放列表页
- **AND** 按最后播放时间降序显示歌曲
- **AND** 显示每首歌曲的最后播放时间

#### Scenario: 首页最近播放展示
- **WHEN** 用户查看首页
- **THEN** 显示最近播放的歌曲列表（最多 10 首）
- **AND** 点击歌曲可直接播放

## MODIFIED Requirements

### Requirement: Song 数据模型
原有 Song 模型基础上增加收藏和统计字段。

#### Scenario: 扩展字段
- **WHEN** 系统加载歌曲数据
- **THEN** 包含 isFavorite（是否收藏）、playCount（播放次数）、lastPlayedAt（最后播放时间）字段

### Requirement: 首页模块
原有首页基础上增加收藏和最近播放入口。

#### Scenario: 快速访问入口
- **WHEN** 用户查看首页快速访问区域
- **THEN** 显示"喜欢的歌曲"入口
- **AND** 显示"最近播放"入口
- **AND** 显示"最常播放"入口（可选）

### Requirement: 播放器界面
原有播放器界面增加收藏按钮。

#### Scenario: 收藏按钮位置
- **WHEN** 用户查看播放器全屏界面
- **THEN** 在歌曲标题附近显示收藏按钮
