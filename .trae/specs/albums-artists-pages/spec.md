# Albums and Artists Pages Spec

## Why
当前专辑和歌手页面是占位符页面，需要实现完整的 iOS 风格列表页面，让用户可以浏览所有专辑和歌手。

## What Changes
- 创建 AlbumsScreen 页面，显示专辑列表
- 创建 ArtistsScreen 页面，显示歌手列表
- 创建对应的 ViewModel 和 Contract 文件
- 使用已有的 iOS 风格组件（IOSListItem, IOSInsetGrouped, IOSTopAppBar 等）
- 支持点击跳转到详情页

## Impact
- Affected specs: AlbumsScreen, ArtistsScreen
- Affected code: 
  - presentation/albums/ (新建)
  - presentation/artists/ (新建)
  - navigation/AppNavGraph.kt (更新)

## ADDED Requirements

### Requirement: Albums Page
系统应提供专辑列表页面，显示所有专辑。

#### Scenario: 显示专辑列表
- **WHEN** 用户进入专辑页面
- **THEN** 显示所有专辑的列表，每个专辑项显示专辑封面、名称、艺术家

#### Scenario: 点击专辑
- **WHEN** 用户点击某个专辑
- **THEN** 导航到专辑详情页

### Requirement: Artists Page
系统应提供歌手列表页面，显示所有歌手。

#### Scenario: 显示歌手列表
- **WHEN** 用户进入歌手页面
- **THEN** 显示所有歌手的列表，每个歌手项显示头像、名称、专辑数量

#### Scenario: 点击歌手
- **WHEN** 用户点击某个歌手
- **THEN** 导航到歌手详情页

## Design Reference
- 使用 iOS Inset Grouped 列表风格
- TopBar 使用白色背景
- 页面背景使用 iOS 灰色
- 列表项使用已有的 IOSListItem 组件
