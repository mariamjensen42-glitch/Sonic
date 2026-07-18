# 单元测试体系 Spec

## Why
项目目前只有示例测试文件，缺少真正的单元测试覆盖。需要建立完整的测试体系以确保代码质量、防止回归问题，并提高代码的可维护性。

## What Changes
- 添加现代测试依赖（MockK、kotlinx-coroutines-test、Turbine等）
- 为domain层编写单元测试（歌词解析器、Use Cases、工具类）
- 为data层编写单元测试（Repository实现）
- 为presentation层编写单元测试（ViewModels）
- 建立测试基础设施和通用测试工具

## Impact
- Affected specs: 无
- Affected code: 
  - 所有模块的 build.gradle.kts（添加测试依赖）
  - 新增测试文件到各模块的 test 目录

## ADDED Requirements

### Requirement: 测试依赖配置
系统 SHALL 配置以下测试依赖：
- MockK：用于模拟依赖
- kotlinx-coroutines-test：用于协程测试
- Turbine：用于Flow测试
- JUnit 5：作为测试运行器（可选，目前使用JUnit 4）

### Requirement: Domain层单元测试
系统 SHALL 为domain层提供单元测试覆盖：

#### Scenario: LrcParser解析标准LRC歌词
- **WHEN** 输入标准格式的LRC歌词文本
- **THEN** 正确解析为SyncedLyrics对象，包含正确的时间戳和歌词内容

#### Scenario: LrcParser解析带翻译的歌词
- **WHEN** 输入包含原文和翻译的LRC歌词
- **THEN** 正确合并原文和翻译到同一行

#### Scenario: TimeUtils时间解析
- **WHEN** 输入各种格式的时间字符串
- **THEN** 正确转换为毫秒值

#### Scenario: UseCase调用Repository
- **WHEN** UseCase被调用
- **THEN** 正确调用对应的Repository方法并返回结果

### Requirement: Data层单元测试
系统 SHALL 为data层提供单元测试覆盖：

#### Scenario: SongRepository获取歌曲列表
- **WHEN** 调用getAllSongs
- **THEN** 从Dao获取数据并正确映射为Domain模型

#### Scenario: SongRepository切换收藏状态
- **WHEN** 调用toggleFavorite
- **THEN** 正确更新数据库中的收藏状态

#### Scenario: Repository错误处理
- **WHEN** 数据源发生错误
- **THEN** 正确处理异常并返回适当的结果

### Requirement: Presentation层单元测试
系统 SHALL 为presentation层提供单元测试覆盖：

#### Scenario: SongsViewModel加载歌曲
- **WHEN** ViewModel初始化
- **THEN** 正确从Repository加载歌曲并更新UI状态

#### Scenario: SongsViewModel切换视图模式
- **WHEN** 用户触发切换视图模式
- **THEN** 正确保存偏好设置并更新UI状态

#### Scenario: ViewModel处理Intent
- **WHEN** 用户发送Intent
- **THEN** 正确处理并产生相应的State更新和Effect

### Requirement: 测试覆盖率目标
系统 SHALL 达到以下测试覆盖率目标：
- Domain层：90%+
- Data层Repository：70%+
- Presentation层ViewModel：80%+
