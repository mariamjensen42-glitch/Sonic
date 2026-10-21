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
| 加载与波形 | `LoadingIndicator` / `WavyProgressIndicator` 自带动效 | 不自绘加载动画、不另配 spec；**波浪指示器是常驻动画，不得放进常驻可见的容器**（迷你播放器已改用静态 `LinearProgressIndicator`：实测 123 fps → 1 fps） |
| 歌词自动滚动 | 时序例外：`tween(300, emphasizedDecelerate)`；**首次对齐 / 跨越多行（拖进度条、切歌）用瞬移**（`scrollToItem`），只有逐行推进才动画 | 不换弹簧（会过冲）；不要给「进页面就跳几十行」做动画 —— 那会边滚边组合中间每一行，是最典型的卡顿 |
| 伴奏行进出 | 时序例外：`tween(600, emphasizedDecelerate / emphasizedAccelerate)` | 同上 |
| 单行标题溢出（播放页标题） | `Modifier.basicMarquee()`：库自带动效，只在文字超出容器时运动，参数用默认值（不登记 `SonMotionTiming`） | 不用自写 tween 做横向滚动；不给多行文本或常驻容器加（它会一直转） |
| 播放页 / 歌词页的动态色面 | 时序例外：`SonMotionTiming.BACKDROP_CYCLE_MS`（16s 一轮）+ `BACKDROP_FRAME_MS`（30fps）；暂停即停止请求帧（ADR-0005） | 不放进底栏 / 迷你播放器等常驻容器；不在其它页面铺 |
| 逐帧进度曲线（呼吸点、逐字高亮） | Compose 内建 `LinearOutSlowInEasing` / `LinearEasing` | 不自定义 easing |

## 时序例外

唯一例外文件 `core/ui/theme/MotionTiming.kt`（`SonMotionTiming`）：两条 M3 强调缓动、
300 / 600 两个时长，以及动态色面的 16s / 33ms 两个节奏 token；**不得出现
`dampingRatio` / `stiffness` / `spring`**；只允许上表三处「时序例外」使用；
新增一项须先在本节登记用途，否则不进这个文件。

> 动态色面的周期与振幅必须一起看（2026-10-20 真机修订）：32s 配 0.05 的扭曲振幅时，
> 一秒内采样点只移动不到半个纹素，播放中的背景在真机上看起来完全是静止的。现在 16s 配
> `WARP = 0.22` + `DRIFT = 0.05`，`SonMeshGradientTest` 的「一秒内动得出来」用例钉住下限。

## 降级

Compose 读系统 `animator_duration_scale`，值为 0 时 finite 动画（tween 与 spring 一视同仁）首帧落到终态。
本项目有两处**自绘无限动画**：播放页 / 歌词页的动态色面（ADR-0005），以及歌词页的逐字高亮
帧循环（`KaraokeLyricsView` 的 `frameTick`：只有当前行读它，所以静态行与它们的离屏层不失效）。
两者都自己读 `ValueAnimator.getDurationScale()`，为 0 时停在第一帧、不再请求帧；色面另外在
暂停时停止请求帧，逐字 tick 同样只在播放中跑 —— 暂停时位置不再推进，高亮本来就是静止的。
歌词进度与呼吸点由播放时间驱动，不随动画缩放变化 —— 这是有意的。

今后若再引入 `rememberInfiniteTransition` / `withInfiniteAnimationFrameNanos` / 自绘帧循环，
同样必须自己处理缩放为 0、以及暂停（界面静止）时停止请求帧。

## 性能预算

- 一次转场最多一个 spatial 元素 + 一个 effects 元素，不叠三段以上。
- 不用模糊做整屏转场；模糊只做行级效果。
- 不按机型分叉实现：低端机降级 = 换低一档 spec，不写第二套物理。
- **常驻可见的容器（底栏、迷你播放器）里只放不会自发重绘的组件**。判定标准很硬：暂停播放、
  界面静止时 `dumpsys gfxinfo <pkg>` 的帧数应为 0；≥1 fps 就说明有常驻动画在烧电。
- `LinearWavyProgressIndicator` 的波浪也是常驻动画：`waveSpeed > 0` 就按屏幕刷新率无限重绘整页。
  所以**会长时间停留在屏幕上的进度一律用静态 `LinearProgressIndicator`** —— 迷你播放器与播放页
  的播放进度都已如此（把 `waveSpeed` 归零只是让波形不流，仍然是波形，不值得为此换一套视觉）；
  只有扫描进度这种「等结果」的短生命周期界面可以直接用波浪版。
- 动态色面（播放页 / 歌词页，ADR-0005）是允许的自发重绘，但**暂停时必须停止请求帧**；
  它不进底栏与迷你播放器，也不在其它页面铺开。
- 播放页底部的频谱条（`BeatSpectrum`）是这一屏唯一的律动：只在 `Canvas`
  的绘制阶段读 `beat()` 快照，`BeatState.isSilent`（暂停 / 静音）时**整条不画**，
  所以暂停后同样要能回到 0 帧；柱形分布用 `sin(i)` 计算，不在绘制路径里分配对象。
  封面本身静态 —— 原先画在封面背后的同心环已删除（半径超出屏幕被裁、内侧被封面盖住，看不见）。
  频谱条可以在播放页「播放设置」面板里关掉（`playerSpectrumEnabled`，默认开）；关掉后这一屏
  不再有频谱条的重绘，播放页剩余的自发重绘只有动态色面。
- 判定靠真机目测 + `docs/design/acceptance.md` 的截图流程；当前没有 CI 视觉回归，不设数字门禁。

## 未覆盖

- 共享元素 / 共享容器：当前 0 处使用；将来出现跨页共享封面时再开票，首选 `SharedTransitionLayout` +
  `sharedBounds`。现有「迷你播放器 → 播放页」是页面转场，不在此列。
- tab 切换的转场语法：归 #7「Expressive 页面语法」；在 #7 定案前，接管导航转场时**不得把 tab 切换变成位移**（两个 tab 根之间维持现状）。
- 系统面（通知、锁屏、小组件）：不在本规则内。
