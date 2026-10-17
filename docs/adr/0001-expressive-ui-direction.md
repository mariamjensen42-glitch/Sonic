# 采用 Material 3 Expressive 作为 UI 方向

主题层已经接入 `MaterialExpressiveTheme`、`MotionScheme.expressive()`、Emphasized 字号与封面取色，但多数页面仍呈现标准 Material 3 的平面观感。我们决定以官方 Expressive 规范与官方示例为参照，先做全量审计，再以「歌单/专辑详情、首页、播放页」三屏试点，建立“每屏 1–2 个 expressive moment + 安静阅读面”的语法，之后逐屏铺开。允许在试点屏调整构图与交互，但保留「首页 / 音乐库」两级信息架构；保留封面取色作为签名色源，不换成固定品牌色；拉丁文采用 Roboto Flex，中文回退系统字体；验收用六条可勾选标记逐屏对比。

**Considered Options**:
- 只换 token、组件与动效，不动布局 —— 成本低，但官方 Expressive 的“感觉”很大一部分来自构图，结果会始终差一口气。
- 全 App 一次性重做 —— 快，但极易变成每屏像不同 App，且音乐播放器的阅读稳定性会被动画淹没。
- 固定品牌色 / 完全 Material You —— 更接近官方示例，但会丢掉这个应用“封面即主题”的签名。

**Consequences**:
- 试点屏会引入新的组件组合、字体资源与动效 token，需要逐屏截图对比与性能检查。
- `SonMotion` 与 `MaterialTheme.motionScheme` 会短期并存；新代码优先使用后者，避免两套物理参数继续分叉。
- Expressive 的边界会被写进 `CONTEXT.md`（expressive moment / quiet surface / hero / color field / shape morph），后续页面按这套词评审，而不是凭感觉加动画。

**Status**: 动效条款由 [ADR-0003](0003-motion-source-of-truth.md) 取代（`SonMotion` 删除，物理只从 `motionScheme` 取）。
