# 架构

Sonic 走 **Clean Architecture + MVI**，依赖方向严格单向。

## 模块划分

```text
app → presentation → domain ← data
app → domain, app → data
benchmark ⇢ app（com.android.test，不在产品依赖链上）
```

| 模块 | 包名 | 职责 |
| --- | --- | --- |
| **app** | `com.cycling.sonic` | `MainActivity`、`SonicApplication`、主题装配。只做装配，不含业务逻辑 |
| **domain** | `com.cycling.domain` | 纯 Kotlin，无 Android 依赖（除 core-ktx） |
| **data** | `com.cycling.data` | 实现所有 repository，落地所有数据源 |
| **presentation** | `com.cycling.presentation` + `com.cycling.core.ui` | Compose UI + ViewModel + 主题系统 |
| **benchmark** | `com.cycling.benchmark` | Macrobenchmark 测试，冷启动 / 设置页滚动 |

## domain

```
domain/src/main/java/com/cycling/domain/
├── model/       Song / Album / Artist / Lyrics / PlayerState
├── repository/  仓库接口（Flow<T> 或 suspend）
├── usecase/     用例（operator fun invoke）
├── lyrics/      歌词子系统：parser / model / exporter / utils
└── scrape/      元数据刮削匹配规则（纯函数）
```

**硬约束**：domain 不依赖 data、presentation，也不依赖 Android 框架，只用 core-ktx + coroutines。

## data

```
data/src/main/java/com/cycling/data/
├── local/
│   ├── database/   AppDatabase
│   ├── dao/
│   ├── entity/
│   ├── mediastore/ MediaStore 查询与写入
│   ├── scanner/    音乐文件扫描
│   ├── lyrics/
│   └── metadata/   AudioMetadataHelper（标签读写入口）
├── api/
│   ├── DeepSeek（AI）
│   ├── lyrics/     AMLL / 网易云 / QQ / 酷狗 / LRCLIB / 音源脚本
│   ├── cover/      在线封面与元数据
│   └── search/     网易云 / QQ / 酷狗 / 酷我 / 咪咕
├── online/lx/      洛雪脚本引擎 + 插件管理 + QRC 解码
├── player/         PlayerManager / PlayerService
├── download/       DownloadTransfer / DownloadService
├── repository/     XxxRepositoryImpl
├── mapper/         Entity ↔ Domain
├── store/          DataStore 封装
├── cache/          CacheJanitor
└── di/             Hilt 模块
```

## presentation

```
presentation/src/main/java/com/cycling/
├── presentation/<feature>/   XxxContract / XxxViewModel / XxxScreen
├── presentation/navigation/  Screen / AppNavGraph / Navigator
├── core/ui/theme/            主题系统（唯一入口）
├── core/ui/apple/            Apple Music 复刻层（唯一例外）
└── core/ui/components/       Material 未提供的组合组件
```

## MVI 数据流

```text
用户操作 → XxxIntent → ViewModel.handleIntent() → UseCase/Repository → 数据源
                                ↓
                  _uiState.update{} → uiState(StateFlow) → Compose 渲染
                  _uiEffect.emit{} → uiEffect(SharedFlow) → 一次性事件（导航/Toast）
```

每个功能包一个 `XxxContract.kt`，里面定义 `UiState` / `Intent` / `Effect` 三件套。

## 依赖注入

- ViewModel：`@HiltViewModel` + `@Inject constructor`
- Repository：接口在 domain，实现在 data，Hilt `@Binds` 绑定（`RepositoryModule`）
- UseCase：`@Singleton class XxxUseCase @Inject constructor`
- `@Provides`：`DatabaseModule` / `PlayerModule` / `AiModule`

## 技术栈

| 领域 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.3.0（jvmToolchain 17） |
| UI | Jetpack Compose（BOM `2026.09.00`）+ Material 3 Expressive `1.5.0-alpha29` |
| 导航 | Navigation 3（`androidx.navigation3`）+ `@Serializable` 类型安全路由 |
| 播放 | Media3 ExoPlayer 1.9.0 + MediaSessionService |
| 数据库 | Room 2.7.1 |
| 偏好 | DataStore Preferences |
| 依赖注入 | Hilt 2.59.2 + KSP 2.3.8 |
| 图片 | Coil 3.1.0 |
| 标签 | jaudiotagger 3.0.1 |
| 取色 | palette-ktx |

`compileSdk` 37（`compileSdkMinor = 1`），`targetSdk` 36，`minSdk` 30。

## 设计体系

全应用只用 **Material 3 Expressive**，设计系统的唯一入口是 `core/ui/theme/`。

**唯一例外**：`core/ui/apple/`。首页、资料库、迷你播放器与底部 Tab 条按 Apple Music 7.0.0-beta
做像素级复刻，用 iOS 语义色板、SF Pro 变体字体与独立几何。
这个例外被收束在这一个包内，不修改 `core/ui/theme/` 的语义。

主题配色来源优先级：

1. 系统壁纸动态取色（默认）
2. 品牌种子色 `SonicSeed`

封面**不参与全局配色**，只通过 `rememberArtworkBackdrop` 产出局部动态色面。
