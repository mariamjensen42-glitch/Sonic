# 排除文件夹功能 Spec

## Why
用户可能希望排除某些文件夹中的歌曲（如录音文件夹、通知音文件夹等），避免这些音频文件出现在音乐库中。

## What Changes
- 新增排除文件夹管理功能，支持添加、删除排除的文件夹路径
- 扫描时自动过滤掉排除文件夹中的歌曲
- 在设置页面添加入口管理排除的文件夹

## Impact
- Affected code:
  - `domain/model/` - 新增 ExcludedFolder 模型
  - `domain/repository/` - 新增 ExcludedFolderRepository 接口
  - `data/repository/` - 新增 ExcludedFolderRepositoryImpl 实现
  - `data/local/mediastore/MediaStoreHelper.kt` - 修改查询逻辑支持排除路径
  - `data/local/scanner/MusicScanner.kt` - 传递排除路径参数
  - `presentation/settings/` - 添加排除文件夹管理入口
  - `presentation/excludefolders/` - 新增排除文件夹管理页面

## ADDED Requirements

### Requirement: 排除文件夹数据模型
系统应提供排除文件夹的数据模型，包含文件夹路径和添加时间。

#### Scenario: 创建排除文件夹
- **WHEN** 用户添加一个文件夹到排除列表
- **THEN** 系统保存文件夹路径和添加时间戳

### Requirement: 排除文件夹持久化存储
系统应使用 DataStore 持久化存储排除的文件夹列表。

#### Scenario: 加载排除文件夹
- **WHEN** 应用启动或扫描开始
- **THEN** 系统从 DataStore 加载已保存的排除文件夹列表

#### Scenario: 保存排除文件夹
- **WHEN** 用户添加或删除排除文件夹
- **THEN** 系统立即将更改保存到 DataStore

### Requirement: 扫描时过滤排除文件夹
系统在扫描音乐时应过滤掉位于排除文件夹中的歌曲。

#### Scenario: 扫描时排除文件夹
- **WHEN** 用户执行音乐扫描
- **AND** 存在排除的文件夹路径
- **THEN** 系统跳过位于排除文件夹中的所有歌曲

### Requirement: 排除文件夹管理界面
系统应提供 iOS 风格的排除文件夹管理界面。

#### Scenario: 查看排除文件夹列表
- **WHEN** 用户进入排除文件夹管理页面
- **THEN** 显示当前所有排除的文件夹列表

#### Scenario: 添加排除文件夹
- **WHEN** 用户点击添加按钮
- **THEN** 系统打开文件夹选择器
- **AND** 用户选择文件夹后添加到排除列表

#### Scenario: 删除排除文件夹
- **WHEN** 用户在某个排除文件夹上左滑或点击删除
- **THEN** 该文件夹从排除列表中移除
