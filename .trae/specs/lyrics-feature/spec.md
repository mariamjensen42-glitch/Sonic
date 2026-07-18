# 本地歌词获取与显示 Spec

## Why
为播放器添加歌词显示功能，支持从本地获取歌词（嵌入式歌词和独立歌词文件），利用已迁移的歌词解析模块和UI组件实现完整的歌词体验。

## What Changes
- 新增歌词获取服务，支持嵌入式歌词（第三方库）和独立歌词文件
- 新增歌词仓库接口和实现
- 新增歌词页面（独立页面，从播放器跳转）
- 支持歌词点击跳转功能

## Impact
- Affected specs: 歌词功能
- Affected code: data 模块、domain 模块、presentation 模块

## ADDED Requirements

### Requirement: 歌词获取
系统应支持从本地获取歌词。

#### Scenario: 获取嵌入式歌词
- **WHEN** 音频文件包含嵌入式歌词
- **THEN** 系统使用第三方库提取歌词内容

#### Scenario: 获取独立歌词文件
- **WHEN** 音频文件同目录下存在同名歌词文件（.lrc, .ttml 等）
- **THEN** 系统读取歌词文件内容

#### Scenario: 支持的歌词格式
- **WHEN** 查找歌词文件
- **THEN** 支持 .lrc、.ttml、.xml 扩展名

#### Scenario: 歌词优先级
- **WHEN** 同时存在嵌入式歌词和独立歌词文件
- **THEN** 优先使用独立歌词文件

#### Scenario: 无歌词
- **WHEN** 歌曲没有任何歌词来源
- **THEN** 返回空歌词状态

### Requirement: 歌词解析
系统应使用歌词解析模块解析歌词内容。

#### Scenario: 自动格式检测
- **WHEN** 获取到歌词内容
- **THEN** 系统自动检测格式并解析为 SyncedLyrics

#### Scenario: 解析失败
- **WHEN** 歌词格式无法识别或解析失败
- **THEN** 返回空歌词状态

### Requirement: 歌词页面
系统应提供独立的歌词显示页面。

#### Scenario: 进入歌词页面
- **WHEN** 用户从播放器点击歌词按钮
- **THEN** 导航到独立的歌词页面

#### Scenario: 显示歌词
- **WHEN** 当前歌曲有歌词
- **THEN** 在歌词页面显示歌词视图

#### Scenario: 歌词滚动
- **WHEN** 播放进度变化
- **THEN** 歌词自动滚动到当前行

#### Scenario: 点击跳转
- **WHEN** 用户点击某一行歌词
- **THEN** 播放器跳转到该时间点

#### Scenario: 无歌词提示
- **WHEN** 当前歌曲无歌词
- **THEN** 显示"暂无歌词"提示

#### Scenario: 返回播放器
- **WHEN** 用户点击返回
- **THEN** 返回播放器页面
