# 字体与图标：C 方案落地

- 状态：已决策（2026-10-17）
- 决策票：[#3 Roboto Flex 与 Material Symbols 选型](https://github.com/mariamjensen42-glitch/Sonic/issues/3)
- ADR：[0002-type-and-icons.md](../adr/0002-type-and-icons.md)
- 研究：[expressive-component-inventory.md](expressive-component-inventory.md)
- 原型：[type-and-icons.html](prototypes/type-and-icons.html)（分支 `prototype/type-and-icons`，52d4685）

## 结论

选 **C**：**Roboto Flex 全局 Latin + Material Symbols Rounded**；中文/日文/韩文回退系统字体；字体与图标**强制子集化**；图标分阶段迁移。

C 是四个变体里最接近官方、也是成本最高的一张。这个决定把“官方感”放在 APK 体积之前，但用子集化把成本从“全量 1.7 MB + 14.5 MB”压到“字体 <150 KB + 图标 <500 KB、APK 增量 <1 MB”的目标区间，实际以 #15 的测量为准。

## 四个变体

| key | 字体 | 图标 | 官方感 | 成本 | 结论 |
| --- | --- | --- | --- | --- | --- |
| A 现状 | System / FontFamily.Default | Material Icons Rounded | ★★☆☆☆ | 0 | 基线 |
| B 研究建议 | Roboto Flex 仅 Latin 展示位 | Material Icons Rounded | ★★★☆☆ | 字体 +1.7 MB；图标 0 | 最小改动，但图标仍是旧的 |
| **C 选中** | **Roboto Flex 全局 Latin** | **Material Symbols Rounded** | **★★★★★** | 字体 +1.7 MB；Symbols ~14.5 MB（全量） | **官方感优先；必须子集化** |
| D 折中 | Roboto Flex 仅 Latin 展示位 | Material Symbols Rounded | ★★★★☆ | 字体 +1.7 MB；Symbols ~14.5 MB | 避开正文混排，仍付图标成本 |

原型样张：`docs/design/prototypes/type-and-icons-{A,B,C,D}.png` + `type-and-icons-B-light.png`；分支里也有。C 的侧栏字体样张、图标样张与“全局 vs 展示位”的英文歌词差异是这次 verdict 的主要依据。

## C 具体是什么

### 字体

- `SonFontFamily.Brand` 与 `.Plain` 都改为 Roboto Flex（variable），替换 `FontFamily.Default`。
- 字重沿用 `SonTypography` 的 400 / 500 / 600 / 650 / 700；Emphasized 变体不改语义，只换字体族。
- 资源：Roboto Flex variable **TTF 子集**（Latin + Latin-ext），放 `presentation/src/main/res/font/`；不要打包全量 1.7 MB；目标 <150 KB。
- 中文 / 日文 / 韩文：系统 fallback；不伪造 CJK 粗体；混排行必须真机检查字重、基线、行高。
- 可变轴：拉丁用 `wght` + `opsz`；不为每屏手写 `fontSize`/`fontWeight`，仍只从 `MaterialTheme.typography` 取。

### 图标

- 使用 Material Symbols Rounded（Apache 2.0），可变轴 `FILL` / `wght` / `GRAD` / `opsz`。
- Symbols 是**连字字体，不是 `ImageVector`**；新增 `M3SymbolIcon` 组合，放在 `core/ui/components/`：
  - 入参：`name: SymbolIconName`、`filled: Boolean`、`contentDescription`、`modifier`、`tint`、`size`。
  - 内部用 `Text` + `FontFamily` + `FontVariation.Settings`（`FILL`/`wght`/`opsz`）；不新增自绘 vector drawable。
  - `SymbolIconName` 用 `enum`/`sealed class` 持有 **PUA 码位**，不用字符串 ligature，避免拼写错误与 ligature 表膨胀。
- 资源：Material Symbols Rounded **TTF 子集**，只含实际用到的 PUA 码位；目标 <500 KB；不要打包全量 14.5 MB。
- 映射：
  - `Icons.Rounded.*` → `M3SymbolIcon(name, filled = true)`
  - `Icons.Outlined.*` → `M3SymbolIcon(name, filled = false)`
  - 导航选中 `FILL 1`，未选中 `FILL 0`。
- 第一批 P0 码位（与原型一致）：

  | 语义 | 码位 | 说明 |
  | --- | --- | --- |
  | play_arrow | U+E037 | 播放 |
  | pause | U+E034 | 暂停 |
  | skip_previous / skip_next | U+E045 / U+E044 | 上一首 / 下一首 |
  | shuffle / repeat | U+E043 / U+E040 | 随机 / 循环 |
  | favorite / favorite_border | U+E87D / U+E87E | 收藏 |
  | home / library_music | U+E88A / U+E030 | 底栏 |
  | queue_music | U+E03D | 队列 |
  | article | U+EF42 | 歌词入口（原型用 article 代替 lyrics，避免旧字体缺字） |
  | more_vert / expand_more | U+E5D4 / U+E5CF | 更多 / 收起 |

### 子集化

- 字体：`fonttools` 先实例化再子集 —— Roboto Flex 的 regular / medium / bold 各一份 Latin 子集，`opsz` 固定在 24。
- 图标：按 `SymbolIconName` 的 PUA 码位集合做子集，FILL 0 / FILL 1 各一份静态 TTF。
- 子集化脚本与码位清单在 `scripts/subset-fonts.py`，可重复；license notice 在 `docs/licenses/`。
- 目标：字体 <150 KB、图标 <500 KB、APK 增量 <1 MB；超出则回退到按需加载/进一步裁剪，不硬塞全量。

## 落地清单（#15 字体与图标落地）

- [x] 字体：Roboto Flex 子集（regular / medium / bold）+ `res/font/` + OFL notice；`SonFontFamily` 指向新字体。
- [x] 图标：Material Symbols Rounded 子集（fill 0 / fill 1）+ Apache notice；新增 `M3SymbolIcon` 与 `SymbolIconName`。
- [x] P0 迁移：`PlayerScreen`、`MiniPlayer`、`M3BottomNavigationBar`、`CollectionDetailScreen` 的运输/底栏/顶栏/收藏/队列图标。
- [x] P1 迁移：列表族、详情、设置、歌词、搜索、文件夹。
- [x] P2 清理：删除 `material-icons-extended`；`grep` 确认无 `Icons.` 残留。
- [x] 更新 `AGENTS.md` / `CLAUDE.md` 的图标规则：从“只用 material-icons-extended”改为“只用 Material Symbols Rounded + M3SymbolIcon”。
- [x] APK 体积测量：`assembleDebug` 前后对比（见下）；目标 <1 MB。
- [x] 明暗两套 + 中英混排截图；CJK fallback 检查；`./gradlew build` + `lint`。
- [x] 六条标记中与字体/图标相关的验收；截图存 `docs/design/screens/<screen>/`。

## 落地结果（2026-10-17，#15）

### 字体资源

| 文件 | 轴 | 体积 |
| --- | --- | --- |
| `roboto_flex_regular.ttf` | wght 400, opsz 24 | 40.7 KB |
| `roboto_flex_medium.ttf` | wght 500, opsz 24 | 40.8 KB |
| `roboto_flex_bold.ttf` | wght 700, opsz 24 | 40.7 KB |
| `material_symbols_rounded.ttf` | FILL 0 | 11.8 KB |
| `material_symbols_rounded_filled.ttf` | FILL 1 | 16.0 KB |
| **合计** | | **150.0 KB**（字体 122.2 KB + 图标 27.8 KB） |

字体目标 <150 KB、图标目标 <500 KB 达成；三份 Flex 合计 122 KB，即使按“字体/图标分开算”也都低于目标。

### 与选型稿的偏差：构建期实例化，而非运行时可变轴

选型与 ADR 写的是“保留可变轴、运行时用 `FontVariation.Settings` 选 FILL/wght/opsz”。实际落地在 API 33（M2104K10AC）上验证发现：Compose 1.13 的 `Font(resId, variationSettings = ...)` 会让资源字体静默回退到系统字体，图标码位渲染成缺字方块（`GLYPH` 框）；去掉 variation settings 后同一份字体立即正常。

因此改为在 `scripts/subset-fonts.py` 里用 `fonttools varLib.instancer` 把轴钉死，产出 5 份静态 TTF。决策本身（Roboto Flex + Material Symbols Rounded、子集化、分阶段迁移）不变；代价是放弃运行时光学尺寸与字重微调，换来确定性渲染和更小的体积。上表 150 KB 是这次实测值。

### 代码落地

- `SymbolIconName`：59 个语义条目 / 58 个唯一码位，覆盖迁移前全部 69 处 `Icons.*` 引用（Rounded→FILL 1，Outlined→FILL 0，`FavoriteBorder`→FILL 0）。
- `M3SymbolIcon`：`BoxWithConstraints` + `Text`（码位直出，不用连字），`filled` 切换字体族；语义按 `Icon` 对齐（`contentDescription = null` 时移出无障碍树）。
- 图标居中：`Text` 用字体自然行高（ascent 1056 + |descent 96| / upem 960 = 1.2em）并做无界测量后再居中；若把 `lineHeight` 压成 1em，基线仍按 ascent 放，字形会整体下沉约 0.1em。真机像素测量：底部导航字形中心与指示器中心差 1px（≈0.36dp），此前为 8px（≈2.9dp）。
- `material-icons-extended` 已从 `gradle/libs.versions.toml` 与 `presentation/build.gradle.kts` 删除；全仓 `grep` 无 `Icons.` / `material.icons`。
- `AGENTS.md` / `CLAUDE.md` 的图标与字体规则已同步改写。
- License：`docs/licenses/OFL-RobotoFlex.txt`、`docs/licenses/Apache-2.0-MaterialSymbols.txt`。

### 验收证据

- `./gradlew :app:assembleDebug`、各模块单测：通过。
- 全量 `./gradlew build` / `lint` 目前被 `:data` 的**既有** lint 错误阻塞（`MediaStoreBitmapLoader.kt` 等 5 处 `UnstableApi` opt-in，与本票改动无关，且数据层属于地图 Out of scope）；`:app` 的 lint 报告只有 warning。
- 真机 M2104K10AC / API 33，明暗两套：`docs/design/screens/{home,library,player,settings}/{light,dark}-after.png`。
- 中英混排：Home 的 `Pretty Boy` / `传奇`、Player 的 `Cruel Summer` / 中文标签同屏；拉丁走 Flex，CJK 走系统 fallback，未见字重跳变或缺字。
- 图标：底栏选中（FILL 1）与未选中（FILL 0）、播放控制、设置列表、迷你播放器逐屏检查，无错位/缺字；字形在容器内垂直居中（像素量测见上）。
- APK（debug）：迁移前 87,670,967 B → 迁移后 55,817,710 B（净减 31,853,257 B ≈ 30.4 MiB，主要来自移除 `material-icons-extended`；新增字体资源仅 150 KB）。

## 风险与缓解

| 风险 | 缓解 |
| --- | --- |
| CJK 正文混排字重/宽度不一致 | 用 Emphasized 字重映射测试；避免 faux-bold；明暗 + 中英混排截图；必要时只在展示位使用 Flex（回退到 B/D） |
| Symbols 全量 14.5 MB | 强制 PUA 子集化；目标 <500 KB；APK 体积作为验收项 |
| 图标语义映射错 | `SymbolIconName` 枚举 + 逐屏视觉检查；P0 先迁移高频图标；保留旧图标回退直到 P1 完成 |
| 迁移遗漏 | `grep Icons.` / `material-icons-extended`；P2 清理；编译期检查 |
| AGENTS.md 规则冲突 | #15 迁移完成后同步更新规则；迁移中明确“旧图标仅作回退” |
| Roboto Flex 中文缺字 | 已决定回退系统；不在 CJK 上强行使用 Flex；字体族 fallback 测试 |

## 不在本决策内

- 具体字重/字距的逐屏调参（属于 #7 页面语法与各屏方案）。
- 图标形状（Rounded vs Outlined）的逐屏选择（#7 统一）。
- 自适应布局、系统面（通知/锁屏/小组件）的字体/图标（地图 Out of scope）。
