# 采用 Roboto Flex 全局 Latin + Material Symbols Rounded

**状态**：Accepted（2026-10-17）。决策票 #3；原型 `prototype/type-and-icons`（52d4685）。

官方 M3 Expressive 的观感很大一部分来自 Roboto Flex 与 Material Symbols。原型对比了四个组合：A 现状、B 只换 Latin 展示位字体、C 全局 Latin 字体 + Symbols、D 展示位字体 + Symbols。用户 verdict 是 **C 最接近官方，按官方感优先**，因此全 App 采用 C，但必须用子集化把字体/图标成本压到可接受范围，并接受中文正文回退系统字体的混排风险。

## 背景

研究（`docs/design/expressive-component-inventory.md`）确认：

- Roboto Flex 是 OFL 可变字体，全量约 1.7 MB，**没有 CJK 子集**；中文/日文/韩文必须回退系统字体。
- Material Symbols 是 Apache 2.0 的连字字体，全量约 10–14.5 MB；`material-icons-extended` ship 的是 Material Icons，不是 Symbols。
- 两者的主要成本来自全量字体；按实际用到的字形/码位子集化后可以小一个数量级。原型里 Roboto Flex Latin 子集 84 KB、14 个 Symbols 图标子集 12 KB。

## 决策

全 App 采用 **C**：

- **字体**：拉丁文字使用 Roboto Flex（display + body）；中文/日文/韩文回退系统字体。`SonFontFamily.Brand` 与 `.Plain` 都指向 Roboto Flex。
- **图标**：使用 Material Symbols Rounded；强调态 `FILL 1`，弱化态 `FILL 0`。
- **强制子集化**：不允许直接打包全量字体。字体目标 < 150 KB，图标目标 < 500 KB，APK 增量目标 < 1 MB，以实际测量为准。落地实测 150.0 KB（Flex 三份 122.2 KB + Symbols 两份 27.8 KB），APK 反而净减约 30.7 MB（移除 `material-icons-extended`）。
- **图标分阶段迁移**：P0 运输控件 / 底栏 / 顶栏 / 迷你播放器；P1 列表 / 详情 / 设置 / 歌词；P2 删除 `material-icons-extended`。
- **接受 CJK 正文混排风险**：中文仍走系统字体；用字重、行高、字距与明暗对比测试补偿，不伪造 CJK 粗体。

## Considered Options

- **A 现状**：系统字体 + 旧图标。零成本，但“官方感”没有变化。
- **B 只换 Latin 展示位字体 + 旧图标**：研究建议的最小改动；官方感有限，图标仍是旧的。
- **C 全局字体 + Symbols**：最接近官方；成本最高（CJK 混排、图标迁移、APK）。**选中**。
- **D 展示位字体 + Symbols**：保留图标升级，避开正文混排；仍要付图标迁移成本。

## Consequences

- 正面：一次解决字体与图标两个缺口，最接近官方 Expressive 的观感。
- 负面：中文正文无法使用 Roboto Flex，混排可能出现字重/宽度差；图标迁移触及所有 `Icons.*` 调用点；`AGENTS.md` / `CLAUDE.md` 中“只用 material-icons-extended”的规则需要改写。
- 缓解：字体/图标子集化；分阶段迁移；迁移完成前保留旧图标回退；明暗两套、中英混排截图验证；APK 体积作为 #15 的验收项。
- 实现修正（#15，2026-10-17）：Compose 1.13 的 `Font(resId, variationSettings = ...)` 在 API 33 上会静默回退系统字体（图标码位渲染成缺字方块），因此改为构建期 `fonttools varLib.instancer` 实例化：regular/medium/bold 三份 Flex + FILL 0/1 两份 Symbols。决策不变，只放弃运行时光学尺寸/字重微调；细节见 `docs/design/type-and-icons.md` 的「落地结果」。
- 后续：`docs/design/type-and-icons.md` 给出落地清单；#15 执行；#7 页面语法引用本决策。
