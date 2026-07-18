# 搜索功能 Spec

## Why
用户需要快速查找本地音乐库中的歌曲、专辑和歌手，提升音乐浏览和播放效率。

## What Changes
- 新增全局搜索页面，支持搜索歌曲、专辑、歌手
- 新增搜索历史记录功能，保存用户最近搜索关键词
- 新增搜索建议功能，根据输入实时提供建议
- 新增分类筛选功能，可按歌曲、专辑、歌手分类查看搜索结果

## Impact
- Affected specs: 无
- Affected code:
  - `presentation/search/` - 新增搜索页面模块
  - `domain/usecase/` - 新增搜索相关用例
  - `domain/repository/` - 扩展 AlbumRepository、ArtistRepository 添加搜索方法
  - `data/repository/` - 实现搜索方法
  - `data/local/dao/` - 新增搜索历史 DAO
  - `data/local/entity/` - 新增搜索历史实体
  - `presentation/navigation/` - 添加搜索页面路由

## ADDED Requirements

### Requirement: 全局搜索功能
系统应提供全局搜索功能，支持同时搜索歌曲、专辑和歌手。

#### Scenario: 搜索音乐内容
- **WHEN** 用户在搜索框输入关键词
- **THEN** 系统实时搜索并显示匹配的歌曲、专辑、歌手结果

#### Scenario: 空搜索状态
- **WHEN** 搜索框为空或无匹配结果
- **THEN** 显示搜索历史或空状态提示

### Requirement: 搜索历史
系统应保存用户的搜索历史记录。

#### Scenario: 保存搜索历史
- **WHEN** 用户提交搜索
- **THEN** 系统将搜索关键词保存到历史记录（最多保留20条）

#### Scenario: 显示搜索历史
- **WHEN** 搜索框获得焦点且为空
- **THEN** 显示最近搜索历史列表

#### Scenario: 清除搜索历史
- **WHEN** 用户点击清除历史
- **THEN** 删除所有搜索历史记录

### Requirement: 搜索建议
系统应根据用户输入提供实时搜索建议。

#### Scenario: 输入时提供建议
- **WHEN** 用户输入搜索关键词
- **THEN** 系统根据历史记录和音乐库内容提供建议

### Requirement: 分类筛选
系统应支持按分类筛选搜索结果。

#### Scenario: 切换分类
- **WHEN** 用户选择歌曲/专辑/歌手分类标签
- **THEN** 仅显示该分类的搜索结果

#### Scenario: 点击搜索结果
- **WHEN** 用户点击歌曲结果
- **THEN** 播放该歌曲
- **WHEN** 用户点击专辑结果
- **THEN** 导航到专辑详情页
- **WHEN** 用户点击歌手结果
- **THEN** 导航到歌手详情页
