# AI 信息缓存持久化 Spec

## Why
当前每次打开 AI 解读弹窗都会调用 DeepSeek API，消耗 token 且响应慢。用户希望将 AI 返回的 Markdown 内容持久化缓存，下次直接显示缓存内容，节省 token 并提升用户体验。

## What Changes
- 新增 AI 信息本地缓存机制
- 优先读取缓存，缓存不存在时才调用 API
- 缓存数据存储在本地数据库中

## Impact
- Affected specs: deepseek-ai-integration
- Affected code: 
  - domain 层: AiRepository 接口新增缓存方法
  - data 层: 新增缓存存储实现
  - presentation 层: AiInfoViewModel 优先读取缓存

## ADDED Requirements

### Requirement: AI 信息缓存
系统 SHALL 将 AI 返回的信息持久化缓存到本地。

#### Scenario: 首次请求 AI 信息
- **WHEN** 用户首次请求某歌曲/艺术家/专辑的 AI 信息
- **THEN** 系统调用 API 获取信息并缓存到本地

#### Scenario: 再次请求相同信息
- **WHEN** 用户再次请求已缓存的 AI 信息
- **THEN** 系统直接从本地缓存读取，不调用 API

#### Scenario: 缓存数据结构
- **GIVEN** AI 信息需要按类型和标识符缓存
- **WHEN** 存储缓存时
- **THEN** 使用 type + 标识符（songTitle+artist / artistName / albumTitle+artist）作为唯一键

### Requirement: 缓存生命周期
系统 SHALL 管理缓存的生命周期。

#### Scenario: 缓存持久化
- **WHEN** 应用关闭后重新打开
- **THEN** 缓存的 AI 信息仍然可用

## MODIFIED Requirements

### Requirement: AiRepository 接口扩展
原有接口需新增缓存相关方法。

**修改后接口:**
```kotlin
interface AiRepository {
    // 原有方法保持不变
    
    // 新增缓存方法
    suspend fun getCachedSongInfo(songTitle: String, artist: String): AiInfo?
    suspend fun getCachedArtistInfo(artistName: String): AiInfo?
    suspend fun getCachedAlbumInfo(albumTitle: String, artist: String): AiInfo?
}
```

### Requirement: AiInfo 模型扩展
AiInfo 需要包含缓存键信息。

**修改后模型:**
```kotlin
data class AiInfo(
    val type: AiInfoType,
    val title: String,
    val content: String,
    val cacheKey: String = ""  // 新增：用于缓存标识
)
```
