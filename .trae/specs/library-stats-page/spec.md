# 音乐库统计页面 Spec

## Why
用户需要一个音乐库统计页面，直观展示音频质量的分布情况（HQ、HR、SQ、Others），帮助用户了解本地音乐库的整体质量构成。

## What Changes
- 新增音频质量字段到Song数据模型（bitrate）
- 新增音乐库统计页面（LibraryStatsScreen）
- 新增饼图组件展示质量占比
- 新增统计明细列表
- 更新音乐扫描器以提取bitrate信息
- 在设置页面添加入口

## Impact
- Affected specs: 数据模型、音乐扫描、导航
- Affected code:
  - `domain/model/Song.kt`
  - `data/local/entity/SongEntity.kt`
  - `data/local/scanner/MusicScanner.kt`
  - `presentation/navigation/Screen.kt`
  - `presentation/navigation/AppNavGraph.kt`
  - `presentation/settings/SettingsScreen.kt`

## ADDED Requirements

### Requirement: 音频质量分类
系统 SHALL 根据比特率(bitrate)将音频文件分为四类：
- **HR (Hi-Res)**: bitrate > 1411 kbps（高解析度音频）
- **SQ (Studio Quality)**: bitrate = 1411 kbps（CD质量/无损）
- **HQ (High Quality)**: 320 kbps ≤ bitrate < 1411 kbps（高质量）
- **Others**: bitrate < 320 kbps 或未知（其他）

#### Scenario: 音频质量分类
- **WHEN** 音乐文件被扫描
- **THEN** 系统根据bitrate自动分类到对应质量等级

### Requirement: 音乐库统计页面
系统 SHALL 提供音乐库统计页面，展示音频质量分布图表和统计明细。

#### Scenario: 查看统计页面
- **WHEN** 用户从设置页面点击"音乐库统计"
- **THEN** 导航到音乐库统计页面
- **AND** 显示饼图展示HQ/HR/SQ/Others占比
- **AND** 显示各分类的数量明细

#### Scenario: 饼图交互
- **WHEN** 用户点击饼图的某个扇区
- **THEN** 高亮显示该扇区
- **AND** 显示该分类的详细数量和百分比

### Requirement: 统计明细展示
系统 SHALL 在图表下方展示各分类的详细统计信息。

#### Scenario: 明细列表展示
- **WHEN** 统计页面加载完成
- **THEN** 显示四个分类的明细卡片
- **AND** 每个卡片包含：分类名称、图标、歌曲数量、占比百分比

### Requirement: 数据持久化
系统 SHALL 在扫描音乐时提取并存储音频比特率信息。

#### Scenario: 扫描时提取bitrate
- **WHEN** 音乐扫描器扫描音频文件
- **THEN** 使用MediaMetadataRetriever提取bitrate
- **AND** 将bitrate存储到数据库

## MODIFIED Requirements

### Requirement: Song数据模型
Song数据模型 SHALL 包含bitrate字段用于存储音频比特率。

```kotlin
data class Song(
    // ... 现有字段
    val bitrate: Int = 0, // 新增：音频比特率(kbps)
)
```

### Requirement: 导航路由
导航系统 SHALL 支持音乐库统计页面的路由。

```kotlin
@Serializable
data object LibraryStats : Screen
```

## Technical Notes

### 图表实现方案
使用自定义Canvas绘制饼图，避免引入额外依赖：
- 轻量级实现
- 完全控制样式
- 符合iOS风格设计

### 颜色方案
- HR: 金色 (#FFD700) - 代表高端
- SQ: 紫色 (#9C27B0) - 代表专业
- HQ: 蓝色 (#2196F3) - 代表优质
- Others: 灰色 (#9E9E9E) - 代表普通
