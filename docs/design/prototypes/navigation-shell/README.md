# 导航外壳原型（Sonic#5）

> **Source snapshot（2026-10-17）**：这是 Sonic#5 导航外壳原型的源码快照，不是 main 的编译目标。
> #17 已删除 live 的 `presentation/.../navigation/prototype/`、`MainActivity` debug 入口、
> `MiniPlayerContainer.ATTACHED` 与 `scripts/capture-nav-shell-prototype.ps1`；这里只保留
> primary source。快照依赖原型时期的 `MiniPlayerContainer` / debug 入口，不保证能对当前 main 编译。
>
> **决策**：选 **A 标准 `NavigationBar`**；B/C 被否。理由见
> [决策文档](../../navigation-shell.md)。

## 问题

底部导航继续用标准 `NavigationBar`，还是换成 Expressive 的
`ShortNavigationBar` / `FlexibleBottomAppBar` + FAB？

## 分支

**UI 分支**：同一个主壳路由上渲染三个变体，底部悬浮调试条切换；不是逻辑/状态模型分支。
三个变体共用真实的 Home / 音乐库内容、真实数据与 Navigation 3 backstack，只换外壳渲染。

## 三个变体

| key | 外壳 | 迷你播放器 | 标签 | FAB |
| --- | --- | --- | --- | --- |
| **A（选中）** | 标准 `NavigationBar`（现状对照） | 悬浮卡片，在底栏上方 | 常显 | 无 |
| B（被否） | `ShortNavigationBar` 悬浮容器，64dp | 悬浮卡片，在底栏上方 | 默认只显示选中项；可切“全部标签” | 无 |
| C（被否） | `FlexibleBottomAppBar` + 分段控件（首页/音乐库） | 贴附在底栏上，拼成一整块 | 常显 | 仅首页；FAB menu（AI 创建歌单 / 随机播放 / 扫描） |

C 的 FAB 可以切“悬浮在底栏上方”和“贴边（压在迷你播放器上）”两种位置，用来暴露叠放冲突。

## 状态外显

底部调试条始终显示当前 variant 与一行状态；展开后能看到并切换：

- mini player 来源：`real` / `demo` / `off`
- 滚动策略：`常显` / `藏迷你` / `藏底栏`，以及当前方向
- B 的标签策略：selected-only / all
- C 的 FAB 位置：悬浮 / 贴边
- 明暗主题
- 最近一次 FAB 动作
- 当前 variant 的 adb 重现命令
- “退出原型”

调试条不属于被评估的设计；判断时忽略它。

## 运行

构建/安装由人执行（AGENTS 禁止 Agent 跑 Gradle）：

```powershell
.\gradlew.bat :app:installDebug
adb shell am start -S -n com.cycling.sonic/.MainActivity `
  --ez prototype_nav_shell true `
  --es prototype_variant A `
  --es prototype_mini on `
  --es prototype_theme system
```

参数：

- `prototype_variant`：`A` / `B` / `C`
- `prototype_mini`：`on` / `off`
- `prototype_theme`：`system` / `light` / `dark`

**请用完整的 `installDebug`，不要只用 Android Studio 的 Apply Changes。** 实测 Apply Changes 的
overlay 只换部分 class，base APK 里没有原型 class；B 的 `ShortNavigationBar` 会只渲染一个 item。


进入后左右箭头切换变体；Release 构建里 `BuildConfig.DEBUG` 会把整条分支挡掉。

## 截图

安装好之后，一条命令抓 12 张（3 变体 × 明暗 × 迷你播放器开/关）：

```powershell
.\scripts\capture-nav-shell-prototype.ps1
```

输出到 `docs/design/screens/nav-shell/`：

```
a-light-mini-on.png   a-light-mini-off.png
a-dark-mini-on.png    a-dark-mini-off.png
b-...                 c-...
capture.txt
```

第一次安装后先手动打开一次并授予媒体权限，否则脚本第一张会拍到系统权限对话框。
脚本默认每次启动等 6 秒；如果 Home 仍在 loading，用 `-WaitSeconds 10` 再跑。
如果 OEM 忽略 `adb shell cmd uimode night`，用调试条里的“夜间”chip 手动切，再单独
`adb pull` 当前屏。

## 权限页

仓库里没有应用内权限页：只有系统运行时对话框，以及设置里的授权入口。原型**没有造假页面**。
主壳在非 tab 路由上由 `NavigationState.recomputeBottomBarVisibility()` 自动隐藏；系统对话框在
Activity 之上。评审时看 edge-to-edge 的系统栏 inset 和设置里的权限行即可。

## 评审时看什么

1. **标签可见性**：A 标签常显；B 未选中只剩图标。只有两个 tab 时，用户还能不能认出“音乐库”？
2. **图标/文字层级与选中态**：A 的官方指示器 vs B 的胶囊展开 vs C 的分段控件。
3. **迷你播放器与底栏的叠放**：A/B 的悬浮卡片 + 80/64dp 底栏；C 的贴附迷你播放器 + 64dp 底栏。
4. **FAB**：只在首页出现是否合理；悬浮与贴边哪个不挡迷你播放器；菜单展开时是否压住内容。
5. **遮挡**：滚到 Home / 音乐库底部，最后一行应停在底栏之上（`96dp + 64dp` 留白）。
6. **edge-to-edge**：状态栏、手势条、底栏 inset；内容不应钻到系统栏下面。
7. **转场**：本次固定现状（tab 淡入淡出、详情横推）；转场语法归 #7，不在这里比较。
8. **明暗**：两套都要读得清。

## 已知边界 / 不要误读

- `ShortNavigationBar` 在 material3 1.5.0-alpha29 里只要 `label != null` 就会一直显示标签；
  B 的“未选中只留图标”是原型手动对未选中项传 `label = null` 实现的，不是组件开关。
  点“B 全部标签”可以看到组件的默认行为。
- FAB 菜单项只关闭菜单并把动作写进调试条，不做真实 AI / 扫描 / 随机播放副作用。
- 滚动隐藏靠 `nestedScroll` 观察纵向滚动；如果当前屏没有可滚动列表，不会触发。
- 原型使用真实曲库；库为空时迷你播放器会显示演示曲目（不播音）。
- B 在 Android Studio Apply Changes overlay 的试跑里只渲染了一个 item；当时不是完整
  `installDebug`，所以没有把它当成完整构建结论，也没有继续追。决策已选 A，B 的截图仅作被否方案参考。
