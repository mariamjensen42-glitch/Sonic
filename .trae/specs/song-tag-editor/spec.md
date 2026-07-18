# 歌曲标签修改功能 Spec

## Why
用户需要能够编辑歌曲的元数据信息（标题、艺术家、专辑、年份、流派等），以便更好地管理和组织音乐库。目前应用只能读取歌曲标签，无法修改。

## What Changes
- 在歌曲详情页添加编辑标签功能入口
- 创建标签编辑界面，支持编辑常用标签字段
- 扩展 AudioMetadataHelper 支持写入 ID3v2 标签
- 更新数据库中的歌曲信息
- 支持批量编辑多首歌曲的标签

## Impact
- Affected specs: 歌曲详情页、音频元数据处理
- Affected code: 
  - `AudioMetadataHelper.kt` - 添加写入标签功能
  - `SongDetailScreen.kt` - 添加编辑入口
  - `SongRepository.kt` - 添加更新歌曲信息接口
  - 新增 `TagEditorScreen.kt` - 标签编辑界面

## ADDED Requirements

### Requirement: 标签编辑入口
系统 SHALL 在歌曲详情页提供编辑标签的入口按钮。

#### Scenario: 进入标签编辑
- **WHEN** 用户在歌曲详情页点击编辑按钮
- **THEN** 系统导航到标签编辑界面，显示当前歌曲的所有可编辑标签信息

### Requirement: 单曲标签编辑
系统 SHALL 支持编辑单首歌曲的以下标签字段：
- 标题 (Title)
- 艺术家 (Artist)
- 专辑 (Album)
- 年份 (Year)
- 流派 (Genre)
- 作曲 (Composer)

#### Scenario: 编辑并保存标签
- **WHEN** 用户修改标签字段并点击保存
- **THEN** 系统将修改写入 MP3 文件的 ID3v2 标签，并更新数据库中的歌曲信息

#### Scenario: 取消编辑
- **WHEN** 用户点击取消或返回
- **THEN** 系统丢弃所有修改，返回上一页面

### Requirement: 标签写入功能
系统 SHALL 使用 mp3agic 库将标签修改写入 MP3 文件的 ID3v2 标签。

#### Scenario: 写入成功
- **WHEN** 标签写入操作成功完成
- **THEN** 系统显示保存成功提示，并刷新歌曲详情页显示新标签

#### Scenario: 写入失败
- **WHEN** 标签写入操作失败（文件不存在、权限不足等）
- **THEN** 系统显示错误提示，保留用户编辑的内容

### Requirement: 数据库同步
系统 SHALL 在标签修改成功后同步更新本地数据库。

#### Scenario: 数据库更新
- **WHEN** 标签写入文件成功
- **THEN** 系统更新 SongEntity 中对应的字段，并通知 UI 刷新

### Requirement: 编辑状态管理
系统 SHALL 在编辑过程中管理状态变化。

#### Scenario: 未保存提示
- **WHEN** 用户修改了标签但未保存就尝试离开
- **THEN** 系统显示确认对话框，询问是否放弃修改
