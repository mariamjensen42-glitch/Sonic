# AI 创建歌单功能 Spec

## Why
用户希望能够更智能地创建播放列表，包括随机生成歌单和根据主题让AI自动选择歌曲创建歌单，提升音乐发现体验和播放列表创建效率。

## What Changes
- 在播放列表页面添加"AI 创建歌单"入口
- 支持随机创建歌单模式
- 支持主题创建歌单模式（用户输入主题，AI从音乐库中选择匹配歌曲）
- 新增 AI 歌单创建对话框组件

## Impact
- Affected specs: 播放列表功能
- Affected code: presentation 层 PlaylistsScreen、PlaylistsViewModel，domain 层 AiRepository 新增方法

## ADDED Requirements

### Requirement: AI 创建歌单入口
系统 SHALL 在播放列表页面提供 AI 创建歌单的入口。

#### Scenario: 显示 AI 创建入口
- **WHEN** 用户进入播放列表页面
- **THEN** 系统显示"AI 创建歌单"按钮

#### Scenario: 未配置 API Key
- **WHEN** 用户点击"AI 创建歌单"但未配置 DeepSeek API Key
- **THEN** 系统提示用户先配置 API Key

### Requirement: 随机创建歌单
系统 SHALL 支持随机从音乐库中选择歌曲创建歌单。

#### Scenario: 随机创建成功
- **WHEN** 用户选择"随机创建"模式并指定歌曲数量
- **THEN** 系统从音乐库中随机选择指定数量的歌曲创建新歌单

#### Scenario: 音乐库歌曲不足
- **WHEN** 用户请求的歌曲数量超过音乐库中的歌曲数量
- **THEN** 系统使用所有可用歌曲创建歌单并提示用户

### Requirement: 主题创建歌单
系统 SHALL 支持根据用户输入的主题由 AI 选择歌曲创建歌单。

#### Scenario: 主题创建成功
- **WHEN** 用户输入主题（如"适合跑步的音乐"、"安静的夜晚"）并确认
- **THEN** 系统 AI 分析音乐库中的歌曲信息，选择与主题匹配的歌曲创建歌单

#### Scenario: 无匹配歌曲
- **WHEN** AI 分析后无法找到与主题匹配的歌曲
- **THEN** 系统提示用户未找到匹配歌曲，建议更换主题

#### Scenario: AI 分析失败
- **WHEN** AI 分析过程中发生错误
- **THEN** 系统显示友好的错误提示

### Requirement: 歌单创建确认
系统 SHALL 在创建歌单前显示预览，让用户确认。

#### Scenario: 预览歌单
- **WHEN** AI 完成歌曲选择
- **THEN** 系统显示歌单名称和选中的歌曲列表供用户预览

#### Scenario: 确认创建
- **WHEN** 用户确认歌单内容
- **THEN** 系统创建播放列表并添加歌曲

#### Scenario: 取消创建
- **WHEN** 用户取消创建
- **THEN** 系统放弃当前选择，返回播放列表页面

### Requirement: 歌单命名
系统 SHALL 为 AI 创建的歌单自动生成合适的名称。

#### Scenario: 随机歌单命名
- **WHEN** 用户创建随机歌单
- **THEN** 系统生成名称如"随机播放 #N"

#### Scenario: 主题歌单命名
- **WHEN** 用户创建主题歌单
- **THEN** 系统使用用户输入的主题作为歌单名称
