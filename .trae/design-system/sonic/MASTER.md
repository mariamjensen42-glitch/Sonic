# Design System Master File

> **LOGIC:** When building a specific page, first check `design-system/pages/[page-name].md`.
> If that file exists, its rules **override** this Master file.
> If not, strictly follow the rules below.

---

**Project:** Sonic
**Generated:** 2026-02-22
**Category:** Music Streaming
**Design System:** Material Design 3 Expressive

---

## Global Rules

### Color Palette (Material 3)

#### Light Theme
| Role | Hex | Usage |
|------|-----|-------|
| Primary | `#6750A4` | Primary actions, FAB |
| On Primary | `#FFFFFF` | Text on primary |
| Primary Container | `#EADDFF` | Tonal buttons, selection |
| Secondary | `#625B71` | Secondary actions |
| Secondary Container | `#E8DEF8` | Chips, selection |
| Tertiary | `#7D5260` | Accent elements |
| Error | `#B3261E` | Error states |
| Surface | `#FFFBFE` | Background |
| Surface Variant | `#E7E0EC` | Cards, elevated surfaces |
| Outline | `#79747E` | Borders, dividers |

#### Dark Theme
| Role | Hex | Usage |
|------|-----|-------|
| Primary | `#D0BCFF` | Primary actions, FAB |
| On Primary | `#381E72` | Text on primary |
| Primary Container | `#4F378B` | Tonal buttons, selection |
| Secondary | `#CCC2DC` | Secondary actions |
| Surface | `#1C1B1F` | Background |
| Surface Variant | `#49454F` | Cards, elevated surfaces |

#### Expressive Accent Colors
| Name | Hex | Usage |
|------|-----|-------|
| Red | `#FF375F` | Music, favorites, playing indicator |
| Orange | `#FF9F0A` | Warnings, highlights |
| Yellow | `#FFD60A` | Accents |
| Green | `#30D158` | Success, shuffle |
| Teal | `#64D2FF` | Albums |
| Blue | `#0A84FF` | Artists |
| Indigo | `#5E5CE6` | Folders |
| Purple | `#BF5AF2` | Playlists |
| Pink | `#FF453A` | My Music |

### Typography (Material 3)

- **Font Family:** System default (Roboto on Android)
- **Scale:** Material 3 Type Scale

| Style | Size | Weight | Line Height | Usage |
|-------|------|--------|-------------|-------|
| Display Large | 57sp | Regular | 64sp | Hero headlines |
| Display Medium | 45sp | Regular | 52sp | Large titles |
| Display Small | 36sp | Regular | 44sp | Section headers |
| Headline Large | 32sp | Regular | 40sp | Page titles |
| Headline Medium | 28sp | Regular | 36sp | Card titles |
| Headline Small | 24sp | Regular | 32sp | Dialog titles |
| Title Large | 22sp | Medium | 28sp | App bar titles |
| Title Medium | 16sp | Medium | 24sp | List item titles |
| Title Small | 14sp | Medium | 20sp | Small titles |
| Body Large | 16sp | Regular | 24sp | Main content |
| Body Medium | 14sp | Regular | 20sp | Secondary content |
| Body Small | 12sp | Regular | 16sp | Captions |
| Label Large | 14sp | Medium | 20sp | Button text |
| Label Medium | 12sp | Medium | 16sp | Navigation labels |
| Label Small | 11sp | Medium | 16sp | Small labels |

### Spacing System

| Token | Value | Usage |
|-------|-------|-------|
| `none` | 0dp | - |
| `extraSmall` | 4dp | Tight gaps, icon padding |
| `small` | 8dp | Icon gaps, inline spacing |
| `medium` | 16dp | Standard padding |
| `large` | 24dp | Section padding |
| `extraLarge` | 32dp | Large gaps |
| `extraExtraLarge` | 48dp | Section margins |
| `extraExtraExtraLarge` | 64dp | Hero padding |

### Shape System

| Token | Value | Usage |
|-------|-------|-------|
| `cornerSmall` | 8dp | Small components |
| `cornerMedium` | 12dp | Buttons, cards |
| `cornerLarge` | 16dp | Large cards, FAB |
| `cornerExtraLarge` | 28dp | Search bars |
| `cornerFull` | 50% | Pills, avatars |

### Elevation System

| Level | Value | Usage |
|-------|-------|-------|
| Level 0 | 0dp | Flat surfaces |
| Level 1 | 1dp | Cards (rest) |
| Level 2 | 3dp | Cards (hover) |
| Level 3 | 6dp | FAB, menus |
| Level 4 | 8dp | Dialogs |
| Level 5 | 12dp | Modals |

---

## Component Specs

### Buttons

**Filled Button (Primary)**
- Shape: cornerMedium (12dp)
- Min Height: 40dp
- Padding: 16dp horizontal, 10dp vertical
- Colors: Primary container
- Animation: Scale 0.95x on press (spring)

**Extended FAB**
- Shape: cornerLarge (16dp)
- Height: 56dp (standard) / 48dp (small)
- Shadow: 6dp elevation
- Animation: Expand/collapse spring animation

**Segmented Button**
- Shape: cornerExtraLarge (28dp, pill)
- Selected: Secondary container background

### Cards

**Elevated Card**
- Shape: cornerMedium (12dp)
- Shadow: 1dp (rest) / 2dp (hover)
- Animation: Scale 0.95x on press

**Media Card (Album/Playlist)**
- Width: 160dp (standard)
- Aspect Ratio: 1:1
- Shape: cornerMedium (12dp)
- Animation: Scale 0.95x on press

**Artist Card**
- Avatar Size: 100dp
- Shape: Circle (cornerFull)
- Animation: Scale 0.95x on press

### Navigation

**Top App Bar**
- Height: 64dp (standard) / 152dp (large)
- Background: Surface
- Title: Title Large
- Actions: 24dp icons

**Navigation Bar**
- Height: 80dp
- Background: Surface Container
- Items: 3-5
- Selected Indicator: Pill shape

### Lists

**List Item**
- Height: 56dp (one line) / 72dp (two lines) / 88dp (three lines)
- Padding: 16dp horizontal
- Leading Icon: 24dp
- Trailing Icon: 24dp
- Divider: Full width or indented

### Player

**Mini Player**
- Height: 64dp
- Background: Surface Container High
- Progress Bar: 4dp, top
- Artwork: 48dp, cornerSmall

**Full Player**
- Artwork: 85% width, cornerLarge
- Play Button: 56dp, circle, accent color
- Progress Bar: 4dp, draggable

---

## Motion System

### Spring Animations

| Type | Damping Ratio | Stiffness | Usage |
|------|---------------|-----------|-------|
| Default | 1.0 | Medium | Standard transitions |
| Expressive | 0.8 | 400 | Buttons, cards |
| Bouncy | 0.6 | 200 | Playful interactions |

### Duration Guidelines

| Type | Duration | Usage |
|------|----------|-------|
| Short | 150ms | Micro-interactions |
| Medium | 300ms | Page transitions |
| Long | 500ms | Complex animations |

### Common Animations

- **Button Press:** Scale 0.95x with spring
- **Card Hover:** Scale 1.02x + elevation change
- **Page Transition:** Slide + fade
- **List Item Enter:** Staggered animation
- **FAB Expand:** Morph animation

---

## Style Guidelines

**Style:** Material Design 3 Expressive

**Keywords:** Emotional, dynamic, personalized, natural motion, bold typography, rich shapes

**Key Effects:**
- Spring-based physics animations
- Dynamic color from wallpaper
- Large touch targets (48dp minimum)
- Expressive typography scale
- Smooth state transitions

---

## Anti-Patterns (Do NOT Use)

- ❌ iOS-style navigation patterns
- ❌ Fixed pixel colors (use Material theme colors)
- ❌ Instant state changes (always animate)
- ❌ Small touch targets (< 48dp)
- ❌ Low contrast text
- ❌ Missing hover/focus states

---

## Pre-Delivery Checklist

Before delivering any UI code, verify:

- [ ] Uses Material 3 theme colors
- [ ] All touch targets ≥ 48dp
- [ ] Spring animations on interactive elements
- [ ] Dark mode support
- [ ] Dynamic color support (Material You)
- [ ] Proper elevation hierarchy
- [ ] Consistent shape usage
- [ ] Typography follows Material 3 scale
- [ ] Accessibility: contrast ratios met
- [ ] Responsive layouts tested
