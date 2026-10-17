# Domain Docs

How the engineering skills should consume this repo's domain documentation when exploring the
codebase.

**Layout: single-context.** One `CONTEXT.md` at the repo root, with architecture decisions under
`docs/adr/`. There is no `CONTEXT-MAP.md` and no per-module `CONTEXT.md`: all four Gradle modules
(`:app`, `:domain`, `:data`, `:presentation`) share one domain vocabulary. `CONTEXT.md` now holds
the design-language terms (expressive moment / quiet surface / hero / color field / shape morph),
and `docs/adr/0001-expressive-ui-direction.md` is the first recorded decision.

## Before exploring, read these

- **`CONTEXT.md`** at the repo root.
- **`docs/adr/`**: read ADRs that touch the area you're about to work in.

If any of these files don't exist, **proceed silently**. Don't flag their absence; don't suggest
creating them upfront. The `/domain-modeling` skill (reached via `/grill-with-docs` and
`/improve-codebase-architecture`) creates them lazily when terms or decisions actually get resolved.

## File structure

Single-context repo:

```
/
├── CONTEXT.md
├── docs/
│   ├── agents/          ← tracker, triage and domain config
│   └── adr/
│       ├── 0001-....md
│       └── 0002-....md
├── app/
├── domain/
├── data/
└── presentation/
```

## Use the glossary's vocabulary

When your output names a domain concept (in an issue title, a refactor proposal, a hypothesis, a
test name), use the term as defined in `CONTEXT.md`. Don't drift to synonyms the glossary
explicitly avoids.

If the concept you need isn't in the glossary yet, that's a signal: either you're inventing
language the project doesn't use (reconsider) or there's a real gap (note it for
`/domain-modeling`).

## Flag ADR conflicts

If your output contradicts an existing ADR, surface it explicitly rather than silently overriding:

> _Contradicts ADR-0007 (event-sourced orders), but worth reopening because…_
