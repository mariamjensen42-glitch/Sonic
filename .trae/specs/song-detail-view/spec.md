# 歌曲详情查看功能 Spec

## Why
用户需要查看歌曲的详细信息，包括文件路径、格式、比特率等，以便更好地管理本地音乐文件。

## What Changes
- 新增歌曲详情页面 (SongDetailScreen)
- 在歌曲列表项添加点击进入详情页的入口
- 显示歌曲的完整元数据信息

## Impact
- Affected specs: 歌曲管理功能扩展
- Affected code: 
  - presentation/songs/ 模块新增 SongDetailScreen
  - 新增 SongDetailContract 和 SongDetailViewModel
  - 可能需要更新导航图

## ADDED Requirements
### Requirement: 歌曲详情页
系统 SHALL 提供一个专门展示歌曲详细信息的页面。

#### Scenario: 查看歌曲详情
- **WHEN** 用户在歌曲列表点击某一首歌曲
- **THEN** 打开歌曲详情页面，显示该歌曲的详细信息

#### Scenario: 查看文件路径
- **WHEN** 用户在歌曲详情页查看文件路径信息
- **THEN** 显示歌曲文件的完整路径，并提供复制功能

#### Scenario: 查看音频格式信息
- **WHEN** 用户在歌曲详情页查看格式信息
- **THEN** 显示音频格式（如 MP3、AAC、FLAC 等）、比特率、采样率等信息

#### Scenario: 查看文件大小
- **WHEN** 用户在歌曲详情页查看文件大小
- **THEN** 以人类可读格式显示文件大小（如 3.5 MB）

## MODIFIED Requirements
### Requirement: 歌曲列表点击行为
原需求：点击歌曲直接播放
- **MODIFIED**: 点击歌曲打开详情页，长按或添加播放按钮才播放

## REMOVED Requirements
无
