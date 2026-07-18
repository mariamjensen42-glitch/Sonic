# AlbumDetail 和 ArtistDetail 页面实现计划

## 概述

实现专辑详情页和歌手详情页，展示详细信息及歌曲列表，采用 iOS 风格 UI。

## AlbumDetail 页面

### 功能

* 显示专辑封面、名称、歌手、年份、歌曲数量

* 显示专辑内歌曲列表

* 点击歌曲可播放

### 需要创建的文件

1. `presentation/src/main/java/com/cycling/presentation/albumdetail/AlbumDetailContract.kt`

   * AlbumDetailUiState: album, songs, isLoading

   * AlbumDetailIntent: LoadAlbum, SongClick

   * AlbumDetailEffect: NavigateToPlayer

2. `presentation/src/main/java/com/cycling/presentation/albumdetail/AlbumDetailViewModel.kt`

   * 注入 AlbumRepository, SongRepository

   * 加载专辑信息和歌曲列表

3. `presentation/src/main/java/com/cycling/presentation/albumdetail/AlbumDetailScreen.kt`

   * iOS 风格 UI

   * 顶部大封面 + 专辑信息

   * 歌曲列表

### UI 布局

```
┌─────────────────────────────┐
│  ← 专辑详情                  │
├─────────────────────────────┤
│      ┌─────────────┐        │
│      │   专辑封面   │        │
│      │  (大尺寸)    │        │
│      └─────────────┘        │
│                             │
│        专辑名称              │
│        歌手名称              │
│     2024 · 12 首歌曲         │
│                             │
├─────────────────────────────┤
│  歌曲列表                    │
│  ├─ 1. 歌曲一               │
│  ├─ 2. 歌曲二               │
│  └─ 3. 歌曲三               │
└─────────────────────────────┘
```

## ArtistDetail 页面

### 功能

* 显示歌手头像、名称、专辑数、歌曲数

* 显示歌手的歌曲列表

* 点击歌曲可播放

### 需要创建的文件

1. `presentation/src/main/java/com/cycling/presentation/artistdetail/ArtistDetailContract.kt`

   * ArtistDetailUiState: artist, songs, isLoading

   * ArtistDetailIntent: LoadArtist, SongClick

   * ArtistDetailEffect: NavigateToPlayer

2. `presentation/src/main/java/com/cycling/presentation/artistdetail/ArtistDetailViewModel.kt`

   * 注入 ArtistRepository, SongRepository

   * 加载歌手信息和歌曲列表

3. `presentation/src/main/java/com/cycling/presentation/artistdetail/ArtistDetailScreen.kt`

   * iOS 风格 UI

   * 顶部头像 + 歌手信息

   * 歌曲列表

### UI 布局

```
┌─────────────────────────────┐
│  ← 歌手详情                  │
├─────────────────────────────┤
│      ┌─────────────┐        │
│      │  歌手头像    │        │
│      │  (圆形)     │        │
│      └─────────────┘        │
│                             │
│        歌手名称              │
│     5 张专辑 · 32 首歌曲      │
│                             │
├─────────────────────────────┤
│  歌曲列表                    │
│  ├─ 歌曲一                  │
│  ├─ 歌曲二                  │
│  └─ 歌曲三                  │
└─────────────────────────────┘
```

## 导航更新

修改 `AppNavGraph.kt`:

* AlbumDetail 路由：从占位符替换为 AlbumDetailScreen

* ArtistDetail 路由：从占位符替换为 ArtistDetailScreen

## 实现步骤

1. 创建 AlbumDetailContract.kt
2. 创建 AlbumDetailViewModel.kt
3. 创建 AlbumDetailScreen.kt
4. 创建 ArtistDetailContract.kt
5. 创建 ArtistDetailViewModel.kt
6. 创建 ArtistDetailScreen.kt
7. 更新 AppNavGraph.kt 导航配置

## 依赖的现有组件

* `AlbumRepository.getAlbumById(id)` - 获取专辑信息

* `SongRepository.getSongsByAlbum(albumId)` - 获取专辑歌曲

* `ArtistRepository.getArtistById(id)` - 获取歌手信息

* `SongRepository.getSongsByArtist(artistId)` - 获取歌手歌曲

* `IOSListItem` - 列表项组件

* `IOSTopAppBar` - 顶部导航栏

