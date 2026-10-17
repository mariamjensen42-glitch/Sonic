# 截图与验收流水线

每张 Expressive 落地票都必须留下可复查的证据：**明暗两套截图 + 六条标记勾选 + build/lint 结果**。
这份文档和 `scripts/ui-verify.ps1` 是唯一入口，不依赖记忆里的手工步骤。

- 状态：已建立（2026-10-17）
- 适用：`presentation` 下所有 App 内页面；系统面（通知 / 小组件 / 锁屏）不在本流水线
- 设备：真机或模拟器均可；本仓库当前验证设备为 `M2104K10AC`（Android 13 / API 33）

## 快速开始

```powershell
# 1. 体检：找 adb、列设备、看 app 是否安装、当前夜间模式
.\scripts\ui-verify.ps1 -Action doctor

# 2. 安装 + 打开
.\scripts\ui-verify.ps1 -Action install
.\scripts\ui-verify.ps1 -Action launch

# 3. 在设备上导航到目标屏，然后各截一张
.\scripts\ui-verify.ps1 -Action capture -Screen player -Theme dark  -State after
.\scripts\ui-verify.ps1 -Action capture -Screen player -Theme light -State after

# 4. 一条命令跑最小验证（debug 构建 + lint）
.\scripts\ui-verify.ps1 -Action check
```

`-State` 用 `before` / `after` 区分改版前后；不传时文件名是 `dark.png` / `light.png`。

## 目录与命名

```
docs/design/screens/
├── _template/
│   └── checklist.md              # 六条标记模板，脚本按屏复制
├── home/
│   ├── dark-before.png
│   ├── light-before.png
│   ├── dark-after.png
│   ├── light-after.png
│   ├── capture.txt               # 设备 / API / commit / app 版本 / 时间
│   └── checklist.md              # 该屏的六条勾选与结论
└── player/
    └── ...
```

- 目录名用小写 kebab-case：`collection-detail`、`song-detail`、`ai-info`。
- 截图文件名：`<theme>-<state>.png`；`state=current` 时省略后缀。
- `capture.txt` 由脚本追加，不手工维护；`checklist.md` 首次生成后由人编辑结论。
- 这些截图**不**加入 `.gitignore`，它们是验收证据；单张约 1 MB，提交时按屏挑选，不要把整批 64 张一次性塞进一个 commit。

## 脚本动作

| Action | 作用 |
| --- | --- |
| `doctor` | 找 adb / 列设备 / 查 app 是否安装 / 查当前夜间模式；不修改任何东西 |
| `build` | `:app:assembleDebug`；加 `-FullBuild` 改为 `build` |
| `lint` | `:app:lint` |
| `install` | `:app:installDebug`（多设备时用 `-Serial`） |
| `launch` | 打开 `com.cycling.sonic/.MainActivity` |
| `capture` | 截当前屏；需要 `-Screen`；可带 `-Theme` / `-State` |
| `capture-set` | 按 `-Screens` 逐屏、逐主题提示你导航后截图（HITL，适合 before/after 批量） |
| `check` | doctor + build + lint；带 `-Screen` 时再截一张 |

常用参数：

- `-Serial <device>`：多设备时指定；默认取唯一在线设备。
- `-AdbPath <path>`：adb 不在 PATH / 不在 SDK 默认位置时显式指定。
- `-Theme light|dark|current`：**先切主题，再导航，再截图**；切主题会让 Activity 重建。
- `-State before|after|current`：只影响文件名与 `capture.txt`，不做任何版本控制。
- `-Screens home,player,...`：仅 `capture-set` 使用。
- `-WaitSeconds 2`：切主题/启动后的等待秒数，慢设备可调大。

## 明暗切换

脚本用 `adb shell cmd uimode night yes|no`。部分 OEM 会忽略这个命令，脚本会 `Write-Warning` 并继续；
这时在设备「设置 → 显示 → 深色模式」手动切换，再重新 `capture`。**明暗两套都必须有截图，不能只交一套。**

## 六条标记（逐屏勾选）

每屏至少 4 条；Home / Player / CollectionDetail 三张试点必须 6 条全过。

1. 一个超大 Emphasized 标题时刻（display/headline 级，不是 body 放大）。
2. 一块真正饱和的色面（container 角色），而不是近黑底上的小色点。
3. 一个按下会形变的主控件（shape morph，不是 ripple 换色）。
4. 一次有物理感的转场（空间弹簧，允许轻微过冲）。
5. 一个 Expressive 组件，或官方组件组成的控制簇（FAB menu / ButtonGroup / SplitButton / Toolbar / Short nav / Loading / Wavy / 共享容器的动作簇）。
6. 正文区是 quiet surface：无分割线、少装饰、留白优先。

勾选模板在 `docs/design/screens/_template/checklist.md`，脚本复制到每屏目录。结论必须写证据
（哪张截图、哪个交互、哪条转场），不能只写「感觉不错」。

## 一张落地票的完成定义

- [ ] `-Action check` 通过（debug 构建 + lint）。
- [ ] 明暗两套截图都在 `docs/design/screens/<screen>/`，命名符合约定。
- [ ] `checklist.md` 的六条标记逐条有结论；试点 6/6，其余 ≥4。
- [ ] 未通过项写明理由与后续票，不藏。
- [ ] 把截图路径与勾选结论贴到该票的 resolution comment，再 close。

## 故障排查

| 现象 | 处理 |
| --- | --- |
| `adb` 找不到 | 安装 platform-tools；设 `ANDROID_HOME` / `ANDROID_SDK_ROOT`；或 `-AdbPath E:\SoftWare\SDK\platform-tools\adb.exe` |
| 没有设备 | 启动模拟器（`emulator -avd Pixel_10`）或连 USB；`adb devices` 里状态必须是 `device` 而不是 `unauthorized` |
| 多个设备 | `-Serial <serial>` |
| 夜间模式切不动 | 手动切系统深色模式；脚本会警告但不阻断 |
| 截图全黑 | 当前是安全窗口（权限/支付等）；先导航到普通页面 |
| app 没装 | `-Action install` |
| Gradle 失败 | 先直接跑 `.\gradlew.bat :app:assembleDebug` 看完整错误；流水线只负责调用，不掩盖构建失败 |

## 不在本流水线内

- 系统面：通知、媒体控制、锁屏、桌面小组件（见地图 Out of scope）。
- 自适应布局：平板 / 折叠 / 横屏 / 多窗口（另开 effort）。
- 自动化视觉回归 / CI 截图对比：当前没有模拟器 CI，截图作为本地人工证据；等有稳定 emulator runner 再单开票。
