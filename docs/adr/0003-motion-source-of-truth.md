# 动效只有一个物理来源：MotionScheme

`SonMotion` 与 `MaterialTheme.motionScheme` 已经并存了一轮。核对结果是：`SonMotion` 的六条弹簧与官方
Expressive token 逐值相同（0.8/380、0.6/800、0.8/200、1.0/1600、1.0/3800、1.0/800），而它的时长、缓动、
按压常量里有近一半无人引用；与此同时，导航层还有一套库硬编码的、读不到 `motionScheme` 的 700ms 交叉
淡入淡出，成了没人认领的第三套参数。

我们决定把弹簧参数收敛到唯一来源：删除 `SonMotion`，所有 spatial / effects 弹簧改用
`MaterialTheme.motionScheme` 的六档；`MotionScheme` 表达不了的时长与缓动只作为**时序例外**保留一处
（歌词自动滚动与伴奏行进出），该处禁止出现任何弹簧参数。页面 push / pop 由 `NavDisplay` 的三个 spec
接管为空间弹簧，不使用库默认（predictive back 只换 spec，保留库的缩放收束形态）；tab 切换的转场语法留给「Expressive 页面语法」。共享元素当前不引入。

**Considered Options**:
- 保留 `SonMotion` 做薄壳 —— `MotionScheme` 在 CompositionLocal 里，object 薄壳只能变成 `@Composable`
  getter，歌词里非 composable 的 `Easing.transform` 调用链还得先改造，比删除更贵。
- 只冻结、存量不动 —— 两套物理参数长期并存，验收条件「不再出现两套物理参数」直接作废。
- 零例外，时序动画也全换弹簧 —— 歌词自动滚动会带过冲，且用「效果弹簧」驱动位移属于语义错配。

**Consequences**:
- 全 App 的页面转场都变了：其余页面从 700ms 交叉淡入淡出改为「横向位移 + 淡入」的空间弹簧
  （约 300–450ms），播放页保留竖向方向、把 450ms 曲线换成同一套弹簧。这是本决策最大的一次观感变化，
  必须真机复验。
- 歌词的自动滚动与伴奏行进出保持原时长与曲线，观感不变；今后新增此类时序动画必须先登记。
- 系统「移除动画」由平台自动降级（Compose 读 `animator_duration_scale`，finite 动画首帧落到终态），
  本项目不需要代码；引入无限动画时须自行处理。
- 迁移分两波：等价替换（观感不变）与导航转场（观感变化），代码在 #16。
