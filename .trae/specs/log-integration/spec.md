# 日志集成 Spec

## Why
项目已经创建了 LogUtils 日志工具类，但尚未在关键位置使用日志来帮助调试和问题排查。需要在所有 ViewModel、Repository 和关键业务类中集成日志，以便开发时能够追踪应用行为。

## What Changes
- 在所有 ViewModel 中添加日志记录用户操作和状态变化
- 在所有 Repository 中添加日志记录数据操作
- 在播放器相关类中添加日志记录播放状态
- 在音乐扫描相关类中添加日志记录扫描进度
- 在辅助工具类中添加日志记录处理过程

## Impact
- Affected specs: 无
- Affected code: 
  - presentation 层所有 ViewModel（22个）
  - data 层所有 Repository（14个）
  - 播放器相关类（PlayerManager、PlayerService）
  - 扫描相关类（MediaStoreHelper）

## ADDED Requirements
### Requirement: ViewModel 日志
所有 ViewModel SHALL 在关键操作处记录日志，包括：
- 页面加载状态变化
- 用户操作（点击、切换等）
- 数据加载成功/失败
- 错误处理

### Requirement: Repository 日志
所有 Repository SHALL 在数据操作处记录日志，包括：
- 数据库操作结果
- API 调用结果
- 数据转换过程
- 错误信息

### Requirement: 播放器日志
播放器相关类 SHALL 记录播放状态变化，包括：
- 播放/暂停状态
- 歌曲切换
- 播放队列变化
- 错误信息

### Requirement: 扫描日志
扫描相关类 SHALL 记录扫描过程，包括：
- 扫描开始/结束
- 扫描进度
- 发现的歌曲数量
- 错误信息

## MODIFIED Requirements
无

## REMOVED Requirements
无
