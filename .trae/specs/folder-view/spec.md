# 文件夹视图 Spec

## Why
用户希望按文件系统目录层级浏览音乐文件，类似于文件管理器的方式，而不是仅按歌曲、专辑、艺术家分类。这对于按文件夹组织音乐的用户非常有用。

## What Changes
- 新增"文件夹"浏览入口
- 创建文件夹浏览页面，支持按目录层级导航
- 支持在文件夹中播放歌曲

## Impact
- Affected specs: 主页导航
- Affected code: presentation 层新增 FolderScreen、FolderViewModel，data 层新增文件夹查询逻辑

## ADDED Requirements

### Requirement: 文件夹浏览入口
系统 SHALL 在主页提供文件夹浏览入口。

#### Scenario: 进入文件夹视图
- **WHEN** 用户在主页点击"文件夹"
- **THEN** 系统导航到文件夹浏览页面

### Requirement: 文件夹层级浏览
系统 SHALL 支持按文件系统目录层级浏览音乐文件。

#### Scenario: 查看根目录文件夹
- **WHEN** 用户进入文件夹视图
- **THEN** 系统显示所有包含音乐的顶级文件夹列表

#### Scenario: 进入子文件夹
- **WHEN** 用户点击某个文件夹
- **THEN** 系统显示该文件夹内的子文件夹和歌曲

#### Scenario: 返回上级文件夹
- **WHEN** 用户点击返回按钮
- **THEN** 系统返回到上级文件夹

### Requirement: 文件夹内歌曲播放
系统 SHALL 支持在文件夹中播放歌曲。

#### Scenario: 播放文件夹内歌曲
- **WHEN** 用户点击文件夹内的歌曲
- **THEN** 系统开始播放该歌曲，播放队列限定为当前文件夹内的歌曲
