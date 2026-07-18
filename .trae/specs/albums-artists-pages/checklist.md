# Checklist

- [x] AlbumsContract.kt 定义了 UiState（专辑列表、加载状态）和 UiIntent
- [x] AlbumsViewModel 正确注入 AlbumRepository 并获取专辑列表
- [x] AlbumsScreen 使用 Scaffold 和 IOSTopAppBar 组件
- [x] AlbumsScreen 使用 IOSInsetGrouped 和 IOSListItem 显示专辑列表
- [x] AlbumsScreen 点击专辑项能正确导航到详情页
- [x] ArtistsContract.kt 定义了 UiState（歌手列表、加载状态）和 UiIntent
- [x] ArtistsViewModel 正确注入 ArtistRepository 并获取歌手列表
- [x] ArtistsScreen 使用 Scaffold 和 IOSTopAppBar 组件
- [x] ArtistsScreen 使用 IOSInsetGrouped 和 IOSListItem 显示歌手列表
- [x] ArtistsScreen 点击歌手项能正确导航到详情页
- [x] AppNavGraph.kt 正确配置了 AlbumsScreen 和 ArtistsScreen 的导航
