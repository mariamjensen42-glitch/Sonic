# Checklist

- [x] ExcludedFolder 数据模型已创建，包含 path 和 addedAt 字段
- [x] ExcludedFolderRepository 接口已创建，定义 getExcludedFolders 和 addExcludedFolder/removeExcludedFolder 方法
- [x] ExcludedFolderRepositoryImpl 使用 DataStore 正确持久化存储排除文件夹
- [x] MediaStoreHelper.queryAllSongs() 正确过滤排除文件夹中的歌曲
- [x] MusicScanner.scanMusic() 加载并传递排除路径到 MediaStoreHelper
- [x] ExcludeFoldersScreen 显示排除文件夹列表，支持 iOS 风格
- [x] ExcludeFoldersScreen 支持添加新文件夹（使用系统文件夹选择器）
- [x] ExcludeFoldersScreen 支持删除排除文件夹
- [x] SettingsScreen 添加了"排除文件夹"入口项
- [x] 导航配置正确，可从设置进入排除文件夹页面
