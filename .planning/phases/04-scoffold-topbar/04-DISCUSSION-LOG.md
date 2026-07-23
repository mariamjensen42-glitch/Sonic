# Phase 4: 很多页面都是scoffold布局的，topbar和内容的，应该写一个布局的复用组件，方便统一。 - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-07-24
**Phase:** 4-很多页面都是scoffold布局的，topbar和内容的，应该写一个布局的复用组件，方便统一。
**Areas discussed:** System bar insets strategy

---

## 系统栏 Inset 策略

| Option | Description | Selected |
|--------|-------------|----------|
| 纯委托 Scaffold（默认） | 完全依赖 Material3 Scaffold 的内置行为 | ✓ |
| 主动设置 WindowInsets | 在 M3Scaffold 内部显式传递 windowInsets | |
| 让调用方通过 Modifier 控制 | 不处理 inset，由调用方在 content 内按需添加 | |

**User's choice:** 纯委托 Scaffold（默认）
**Notes:** AiInfoScreen 移除多余的 .statusBarsPadding() 以避免双重内边距。其余 5 个 Pattern A 屏幕原本就使用 Scaffold，inset 行为不变。

---

## the agent's Discretion

- **Component API surface:** Keep as currently planned (containerColor, contentColor, topBar, bottomBar, modifier, content — no extra params).
- **Default containerColor:** Keep SpotifyColors.Background. SongsScreen override proves the override path works.
- **Excluded screens:** AlbumDetail, ArtistDetail, PlaylistDetail, Lyrics, Home, Search, MyMusic, Player — confirmed out of scope.

## Deferred Ideas

None.
