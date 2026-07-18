<h1 align="center">Sonic</h1>

<p align="center">
  <strong>🎵 现代化 Android 音乐播放器，AI 驱动的音乐洞察</strong>
</p>

<p align="center">
  <a href="https://opensource.org/licenses/MIT"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License"></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.0.21-purple.svg" alt="Kotlin"></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/Jetpack%20Compose-2024.10.01-brightgreen.svg" alt="Compose"></a>
  <a href="https://android-arsenal.com/api?level=30"><img src="https://img.shields.io/badge/API-30%2B-orange.svg" alt="API Level"></a>
  <a href="https://github.com/cycling02/Sonic/actions"><img src="https://img.shields.io/github/actions/workflow/status/cycling02/Sonic/android.yml?branch=main" alt="Build Status"></a>
</p>

<p align="center">
  <a href="#-概述">概述</a> •
  <a href="#-功能特性">功能特性</a> •
  <a href="#-技术栈">技术栈</a> •
  <a href="#-架构设计">架构设计</a> •
  <a href="#-快速开始">快速开始</a>
</p>


## ✨ 概述

**Sonic** 是一款精心设计、功能丰富的 Android 音乐播放器，融合了现代 Material 3 设计与 iOS 风格的 UI 元素。采用最新的 Android 技术构建，包括 Jetpack Compose、Clean Architecture 和 MVI 模式，Sonic 为用户带来优质的音乐聆听体验，并提供 AI 驱动的专辑和艺术家洞察功能。

### 🎯 核心亮点

- 🎨 **精美界面** - Material 3 设计搭配 iOS 风格组件
- 🎤 **卡拉OK歌词** - 实时同步歌词显示
- 🤖 **AI 集成** - DeepSeek 驱动的专辑和艺术家洞察
- 🎵 **强大播放** - Media3 ExoPlayer 支持后台播放
- 📱 **现代架构** - Clean Architecture + MVI 模式

---

## 🚀 功能特性

### 🎵 音乐播放
- **后台播放** - 前台服务实现无缝后台播放
- **媒体控制** - 通知栏控制和媒体会话支持
- **播放队列** - 添加、移除、重排播放队列中的歌曲
- **播放模式** - 单曲循环、列表循环、随机播放
- **智能统计** - 播放超过 50% 后计入播放次数

### 🎤 高级歌词系统
支持多种歌词格式：
| 格式 | 描述 |
|--------|-------------|
| **LRC** | 标准 LRC 歌词 |
| **Enhanced LRC** | 增强型 LRC，支持单词级别时间轴 |
| **TTML** | 定时文本标记语言 |
| **Lyricify Syllable** | 逐音节歌词 |
| **Word by Word LRC** | 单词级别同步歌词 |

**卡拉OK 体验：**
- 实时音节高亮
- 平滑滚动动画
- 点击歌词跳转播放位置

### 🤖 AI 驱动洞察
- **专辑信息** - AI 生成的专辑描述和背景介绍
- **艺术家背景** - 全面的艺术家介绍
- **智能缓存** - 避免重复 API 调用
- **Markdown 渲染** - 富文本格式内容展示

### 📚 音乐库管理
- **自动扫描** - 基于 MediaStore 的本地音乐发现
- **文件夹排除** - 自定义扫描位置
- **播放列表** - 创建和管理自定义播放列表
- **收藏夹** - 快速访问喜爱的曲目
- **统计信息** - 最近播放、最多播放、音乐库统计

### 🎨 iOS 风格 UI 组件
- `IOSInsetGrouped` - iOS 风格分组列表布局
- `IOSLargeTitleTopAppBar` - 大标题导航栏
- `IOSListItem` - iOS 风格列表项，支持滑动操作
- `IOSButton` - iOS 风格按钮组件

---

## 🛠 技术栈

### 核心技术
| 技术 | 版本 | 用途 |
|------------|---------|---------|
| ![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin) | 2.0.21 | 主要开发语言 |
| ![Jetpack Compose](https://img.shields.io/badge/Compose-2024.10.01-4285F4?logo=jetpackcompose) | BOM 2024.10.01 | 声明式 UI |
| ![Material 3](https://img.shields.io/badge/Material%203-Latest-4285F4) | - | 设计系统 |
| ![Hilt](https://img.shields.io/badge/Hilt-2.52-4285F4) | 2.52 | 依赖注入 |
| ![Room](https://img.shields.io/badge/Room-2.6.1-4285F4) | 2.6.1 | 本地数据库 |
| ![Media3](https://img.shields.io/badge/Media3-1.9.0-4285F4) | 1.9.0 | 媒体播放 |
| ![Coil](https://img.shields.io/badge/Coil-3.1.0-4285F4) | 3.1.0 | 图片加载 |

### 架构组件
- **Clean Architecture** - 通过 domain/data/presentation 层实现关注点分离
- **MVI 模式** - Intent/State/Effect 单向数据流
- **Kotlin Coroutines** - 异步编程
- **Flow** - 响应式数据流
- **Navigation Compose** - 类型安全导航

---

## 🏗 架构设计

Sonic 遵循 **Clean Architecture** 原则和 **MVI (Model-View-Intent)** 模式：

```
┌─────────────────────────────────────────────────────────────┐
│                    展示层 (PRESENTATION)                     │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Compose UI │  │ ViewModels  │  │  Contract/State/    │  │
│  │             │◄─┤             │◄─┤  Intent             │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                      领域层 (DOMAIN)                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │  Use Cases  │  │  Models     │  │  Repository         │  │
│  │             │──┤             │──┤  Interfaces         │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                      数据层 (DATA)                           │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Repository  │  │    Room     │  │   MediaStore/       │  │
│  │  Impl       │──┤  Database   │──┤   Remote API        │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 数据流
```
用户操作 → Intent → ViewModel → UseCase → Repository → 数据源
                    ↓
               状态更新 → UI 渲染
```

---

## 📁 项目结构

```
Sonic/
├── app/                           # 主应用模块
│   ├── MainActivity.kt            # 单 Activity 入口
│   ├── SonicApplication.kt        # Hilt 入口
│   └── MainViewModel.kt           # 主题管理
│
├── presentation/                  # UI 层 (Compose + ViewModels)
│   ├── components/                # 可复用 UI 组件
│   │   ├── IOSLayout.kt          # iOS 风格布局
│   │   ├── IOSListItem.kt        # iOS 风格列表项
│   │   └── IOSButton.kt          # iOS 风格按钮
│   ├── player/                    # 播放器界面
│   ├── lyrics/                    # 歌词显示
│   ├── home/                      # 首页
│   ├── albums/                    # 专辑列表/详情
│   ├── artists/                   # 艺术家列表/详情
│   ├── songs/                     # 歌曲列表
│   ├── playlists/                 # 播放列表管理
│   ├── search/                    # 搜索功能
│   ├── settings/                  # 应用设置
│   └── navigation/                # 导航图
│
├── domain/                        # 业务逻辑层
│   ├── model/                     # 领域模型
│   ├── repository/                # 仓库接口
│   ├── usecase/                   # 业务用例
│   └── lyrics/                    # 歌词解析系统
│       ├── parser/                # LRC/TTML/Syllable 解析器
│       ├── model/                 # 歌词数据模型
│       └── exporter/              # 歌词导出工具
│
├── data/                          # 数据层
│   ├── local/                     # 本地数据源
│   │   ├── database/              # Room 数据库
│   │   ├── dao/                   # 数据访问对象
│   │   ├── entity/                # 数据库实体
│   │   ├── mediastore/            # MediaStore 查询
│   │   └── scanner/               # 音乐文件扫描器
│   ├── api/                       # 远程 API (DeepSeek)
│   ├── player/                    # 播放管理器
│   ├── repository/                # 仓库实现
│   ├── mapper/                    # 数据映射器
│   └── di/                        # Hilt 模块
│
└── gradle/
    └── libs.versions.toml         # 版本目录
```

---

## 🏃 快速开始

### 环境要求

- ![Android Studio](https://img.shields.io/badge/Android%20Studio-Ladybug%20%7C%202024.2.1-green?logo=androidstudio)
- ![JDK](https://img.shields.io/badge/JDK-17-orange?logo=openjdk)
- ![Android SDK](https://img.shields.io/badge/Android%20SDK-36-blue?logo=android)
- ![Min SDK](https://img.shields.io/badge/Min%20SDK-30-blue?logo=android) (Android 11+)

### 克隆与构建

```bash
# 克隆仓库
git clone https://github.com/cycling02/Sonic.git
cd sonic

# 构建项目
./gradlew build

# 安装到设备/模拟器
./gradlew installDebug
```

### 配置

1. **DeepSeek API Key** (可选 - 用于 AI 功能)
   
   在项目根目录创建 `local.properties`：
   ```properties
   DEEPSEEK_API_KEY=your_api_key_here
   ```

2. **构建变体**
   - `debug` - 开发构建，启用调试
   - `release` - 生产构建，启用优化

---

## 📥 下载

<p align="center">
  <a href="https://github.com/cycling02/Sonic/releases/latest">
    <img src="https://img.shields.io/github/v/release/cycling02/Sonic?label=下载最新版本&style=for-the-badge&color=blue" alt="Download Latest Release">
  </a>
</p>

| 版本 | 最低 Android 版本 | 目标 Android 版本 |
|---------|-------------|----------------|
| 最新版  | Android 11 (API 30) | Android 14 (API 36) |

---

## 🤝 参与贡献

欢迎参与贡献！提交 Pull Request 前请阅读我们的[贡献指南](CONTRIBUTING.md)。

### 开发流程

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: add amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 提交 Pull Request

### 提交规范

我们遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

| 类型 | 描述 |
|------|-------------|
| `feat` | 新功能 |
| `fix` | Bug 修复 |
| `docs` | 文档更新 |
| `style` | 代码风格（格式化） |
| `refactor` | 代码重构 |
| `test` | 添加测试 |
| `chore` | 维护任务 |

---

## 📄 许可证

本项目采用 MIT 许可证 - 详情请查看 [LICENSE](LICENSE) 文件。

```
MIT License

Copyright (c) 2025 cycling02

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## 🙏 致谢

- [Media3 ExoPlayer](https://developer.android.com/media/media3) - 强大的媒体播放器
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代 Android UI 工具包
- [Coil](https://coil-kt.github.io/coil/) - Android 图片加载库
- [DeepSeek](https://deepseek.com/) - AI 驱动的洞察功能

---

<p align="center">
  <strong>用 ❤️ 打造 by cycling02</strong>
</p>

<p align="center">
  <a href="https://github.com/cycling02/Sonic/stargazers">
    <img src="https://img.shields.io/github/stars/cycling02/Sonic?style=social" alt="Star this repo">
  </a>
  <a href="https://github.com/cycling02/Sonic/network/members">
    <img src="https://img.shields.io/github/forks/cycling02/Sonic?style=social" alt="Fork this repo">
  </a>
</p>
