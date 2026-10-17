# Triage Labels

The skills speak in terms of five canonical triage roles. This file maps those roles to the actual
label strings used in this repo's GitHub issue tracker (they are identical — no remapping needed).

| Label in mattpocock/skills | Label in our tracker | Meaning                                  |
| -------------------------- | -------------------- | ---------------------------------------- |
| `needs-triage`             | `needs-triage`       | Maintainer needs to evaluate this issue  |
| `needs-info`               | `needs-info`         | Waiting on reporter for more information |
| `ready-for-agent`          | `ready-for-agent`    | Fully specified, ready for an AFK agent  |
| `ready-for-human`          | `ready-for-human`    | Requires human implementation            |
| `wontfix`                  | `wontfix`            | Will not be actioned                     |

When a skill mentions a role (e.g. "apply the AFK-ready triage label"), use the corresponding label
string from this table. Every triaged issue carries exactly one category label (`bug` /
`enhancement`) and one state label from the table above.

## Wayfinder labels

The `wayfinder` skill (and the "Wayfinding operations" section of
[issue-tracker.md](./issue-tracker.md)) uses five further labels. They are orthogonal to the
triage roles above — never apply them as a triage state.

| Label                  | Applied to                        |
| ---------------------- | --------------------------------- |
| `wayfinder:map`        | The one map issue                 |
| `wayfinder:research`   | Research (AFK) child ticket       |
| `wayfinder:prototype`  | Prototype (HITL) child ticket     |
| `wayfinder:grilling`   | Grilling (HITL) child ticket      |
| `wayfinder:task`       | Task (HITL or AFK) child ticket   |

If a label string ever changes in the tracker, update the right-hand column here to match.
