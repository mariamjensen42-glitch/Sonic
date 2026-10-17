# 动效规则

状态：已定（ADR-0003）。适用：`presentation` 下所有页面与组件。

## 物理

- **spatial** 管位移、尺寸、形状：常规 `defaultSpatialSpec`，要「立刻回应」用 `fastSpatialSpec`，
  整页或大面板用 `slowSpatialSpec`。
- **effects** 管颜色、透明度、模糊：对应 `defaultEffectsSpec` / `fastEffectsSpec` / `slowEffectsSpec`
  （阻尼 1.0，不过冲）。
- 位移与淡入淡出同时发生时，取**同一档位**的 spatial + effects 配对，不要自己拼第三条曲线。

## 场景表

| 场景 | 唯一推荐 | 什么时候不该用 |
| --- | --- | --- |
| 页面 push / pop | `defaultSpatialSpec` + `defaultEffectsSpec`，由 `NavDisplay` 的 enter / pop 两个 spec 注入（predictive pop 见下一行） | 不用库默认的 700ms crossfade（硬编码，不属于本体系）；不为单页手写第四条曲线 |
| 页面 predictive back | 淡入用 `defaultEffectsSpec`，收束用 `defaultSpatialSpec` 驱动 `scaleOut(0.7)`（保留库的形态） | 不改成位移，否则手势方向与内容方向打架 |
| 列表行进出、展开收起 | `defaultSpatialSpec`（位移 / 尺寸）+ `fastEffectsSpec`（淡入淡出） | 不逐行串行延迟制造「瀑布」 |
| 焦点与选中态 | `fastSpatialSpec` | 颜色、透明度属于 effects，不要拿 spatial 档去动 |
| 按压与形变 | 交给 Material 组件内部的 `*Shapes` | 不手写 scale 或 shape morph |
| 底栏、迷你播放器进出 | 进 `slowSpatialSpec` + `fastEffectsSpec`；出 `fastSpatialSpec` | 不与页面转场叠加成两段动画 |
| 加载与波形 | `LoadingIndicator` / `WavyProgressIndicator` 自带动效 | 不自绘加载动画、不另配 spec |
| 歌词自动滚动 | 时序例外：`tween(300, emphasizedDecelerate)` | 不换弹簧（会过冲） |
| 伴奏行进出 | 时序例外：`tween(600, emphasizedDecelerate / emphasizedAccelerate)` | 同上 |
| 逐帧进度曲线（呼吸点、逐字高亮） | Compose 内建 `LinearOutSlowInEasing` / `LinearEasing` | 不自定义 easing |

## 时序例外

唯一例外文件 `core/ui/theme/MotionTiming.kt`（`SonMotionTiming`）：只放两条 M3 强调缓动与 300 / 600
两个时长；**不得出现 `dampingRatio` / `stiffness` / `spring`**；只允许上表两处「时序例外」使用；
新增一项须先在本节登记用途，否则不进这个文件。

## 降级

Compose 读系统 `animator_duration_scale`，值为 0 时 finite 动画（tween 与 spring 一视同仁）首帧落到终态。
本项目没有自绘无限动画；歌词进度与呼吸点由播放时间驱动，不随动画缩放变化 —— 这是有意的。
今后若引入 `rememberInfiniteTransition` / `withInfiniteAnimationFrameNanos`，必须自己处理缩放为 0 的情况。

## 性能预算

- 一次转场最多一个 spatial 元素 + 一个 effects 元素，不叠三段以上。
- 不用模糊做整屏转场；模糊只做行级效果。
- 不按机型分叉实现：低端机降级 = 换低一档 spec，不写第二套物理。
- 判定靠真机目测 + `docs/design/acceptance.md` 的截图流程；当前没有 CI 视觉回归，不设数字门禁。

## 未覆盖

- 共享元素 / 共享容器：当前 0 处使用；将来出现跨页共享封面时再开票，首选 `SharedTransitionLayout` +
  `sharedBounds`。现有「迷你播放器 → 播放页」是页面转场，不在此列。
- tab 切换的转场语法：归 #7「Expressive 页面语法」；在 #7 定案前，接管导航转场时**不得把 tab 切换变成位移**（两个 tab 根之间维持现状）。
- 系统面（通知、锁屏、小组件）：不在本规则内。
