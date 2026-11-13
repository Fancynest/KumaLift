# KumaLift – Design Tokens (Dark and Light)

Single source of truth for colors and shared visual tokens. Supersedes the palette in the PRD for implementation purposes. The dark theme is the default and matches the Stitch mockups; the light theme is specified here.

## 1. Principles

1. **Never hardcode hex values in screens or components.** Use named role tokens from `core/designsystem` only.
2. **Lime is a fill color first.** `#C6FF3D` is almost invisible on light backgrounds (contrast about 1.2:1 against white), so in the light theme it is used only as a **fill with dark content on top**. Lime-colored text, icons, and lines use darker variants (`accentText`, `accentChart`).
3. **Lime stays rare in both themes.** Reserve it for primary actions, completed sets, PRs, active tab, and active chart series.
4. **Depth:** dark theme uses surface color steps. Light theme uses surface steps plus very soft shadows.
5. **Contrast targets (WCAG):** body text 4.5:1, large text (18sp+ or 14sp bold) 3:1, interactive component boundaries and meaningful graphics 3:1. Decorative dividers may be lower.

## 2. Color Tokens

### 2.1 Surfaces and borders

| Token | Dark | Light | Usage |
|---|---|---|---|
| `canvas` | `#0C0E12` | `#F4F5F2` | Screen background |
| `surface` | `#1A1C20` | `#FFFFFF` | Cards, sheets, bottom nav |
| `surfaceVariant` | `#22252A` | `#ECEEE8` | Inputs, nested containers, set-row background |
| `surfaceBright` | `#37393E` | `#E1E4DB` | Chips, pressed state, segmented-control selected background |
| `border` | `#2A2D35` | `#D8DCD1` | Dividers and card outlines (decorative) |
| `borderStrong` | `#6B7280` | `#8A9183` | Outlines of interactive controls (text fields, unchecked checkbox, outlined buttons) |
| `scrim` | `#000000` at 60% | `#0C0E12` at 40% | Modal and bottom-sheet overlay |

### 2.2 Text and icons

| Token | Dark | Light | Usage |
|---|---|---|---|
| `textPrimary` | `#FFFFFF` | `#0C0E12` | Titles, values, metrics |
| `textSecondary` | `#94A3B8` | `#475569` | Labels, descriptions |
| `textSubtle` | `#64748B` | `#64748B` | Hints, disabled text, axis labels. Large text or non-essential info only in the dark theme (contrast about 3.6:1 on `surface`). |

### 2.3 Accent (Electric Lime)

| Token | Dark | Light | Usage |
|---|---|---|---|
| `accentFill` | `#C6FF3D` | `#C6FF3D` | Primary buttons, FAB, active tab pill, completed-set check, PR badge background |
| `accentFillPressed` | `#B3EB2A` | `#B3EB2A` | Pressed state of `accentFill` |
| `onAccentFill` | `#0C0E12` | `#0C0E12` | Text and icons on `accentFill` (contrast 16.3:1) |
| `accentFillBorder` | transparent | `#9FD11F` | Thin border around lime fills on light backgrounds so the edge is visible (decorative) |
| `accentText` | `#C6FF3D` | `#4D7C0F` | Lime-colored text and icons on `canvas` or `surface` (light: 5.0:1 on white, 4.6:1 on canvas) |
| `accentTextOnTint` | `#C6FF3D` | `#3F6212` | Lime-colored text on `accentTint` or `surfaceVariant` (light: 6.3:1 on tint) |
| `accentChart` | `#C6FF3D` | `#65A30D` | Chart lines, active bars, active switch track, progress bars (light: 3.1:1 on white) |
| `accentTint` | `#C6FF3D` at 14% | `#EAF7C2` | Background of completed set rows and PR highlight cards |

Disabled `accentFill`: apply 38% opacity to both the fill and its content.

### 2.4 Status

| Token | Dark | Light | Usage |
|---|---|---|---|
| `error` | `#FF5C5C` | `#D92D20` | Destructive actions, validation errors, Danger Zone |
| `onError` | `#0C0E12` | `#FFFFFF` | Content on `error` fill |
| `errorTint` | `#FF5C5C` at 14% | `#FDE7E5` | Error banners |

### 2.5 Charts

| Token | Dark | Light | Usage |
|---|---|---|---|
| `chartSeriesPrimary` | `#C6FF3D` | `#65A30D` | 1RM line, current-week bar |
| `chartSeriesMuted` | `#37393E` | `#D5D9CF` | Past-week bars, inactive segments |
| `chartGrid` | `#2A2D35` | `#E1E4DB` | Grid lines |
| `chartThreshold` | `#64748B` | `#64748B` | Maintenance threshold line (dashed) |
| `chartPrMarker` | `#C6FF3D` with glow | `#4D7C0F` with white ring | PR peak marker |

For the volume distribution bar (several muscle groups), use opacity steps of `chartSeriesPrimary` (100%, 70%, 45%, 25%) instead of introducing new hues. Always pair color with a text label.

## 3. Shared Tokens (identical in both themes)

| Group | Tokens |
|---|---|
| Font | Inter. Tabular numerals for all metrics and set tables. |
| Weights | Regular 400, Medium 500, Semibold 600, Bold 700, Extrabold 800 (large telemetry values) |
| Radius | `radiusSm` 8dp, `radiusMd` 12dp, `radiusLg` 16dp, `radiusFull` 999dp (pills, badges) |
| Spacing scale (dp) | 4, 8, 12, 16, 20, 24, 32, 40 |
| Tap target | Minimum 48dp |
| Borders | 1dp default, 1.5dp for `borderStrong` outlines |
| Motion | Standard 200ms ease-out. If `appearance.reduceMotion` is on, use 0ms or simple fades. |

### Elevation

| Level | Dark | Light |
|---|---|---|
| 0 (canvas) | none | none |
| 1 (cards) | `surface` color only, 1dp `border` | `0 1px 2px rgba(12,14,18,0.06)` |
| 2 (sheets, FAB, floating timer) | `surfaceVariant` color plus `border` | `0 4px 12px rgba(12,14,18,0.08)` |
| 3 (dialogs) | `surfaceBright` color plus `border` | `0 8px 24px rgba(12,14,18,0.12)` |

## 4. Component Rules

| Component | Rule |
|---|---|
| Primary button | `accentFill` background, `onAccentFill` text, `accentFillBorder` in light. Full pill radius. |
| Secondary button | `surfaceVariant` background (or transparent) with `borderStrong` outline, `textPrimary` text. |
| Destructive button | `error` outline and text; filled `error` only in the final confirmation dialog. |
| Set row (default) | `surfaceVariant` background. |
| Set row (completed) | `accentTint` background. Checkbox is a circle filled with `accentFill` and a `onAccentFill` check mark. |
| Checkbox (unchecked) | Circle with 1.5dp `borderStrong` outline. |
| Bottom navigation | `surface` background with a 1dp top `border`. Active tab: `accentFill` pill with `onAccentFill` icon and label. Inactive: `textSubtle` icon, `textSecondary` label. |
| Switch | On: track `accentChart` in light, `accentFill` in dark; thumb white in light, `onAccentFill` in dark. Off: track `surfaceBright`, thumb `borderStrong`. |
| Text field | `surfaceVariant` fill, `borderStrong` outline; focused outline `accentChart` (light) or `accentFill` (dark). |
| Segmented control | Container `surfaceVariant`; selected segment `surfaceBright` with `textPrimary`, or `accentFill` for the primary selector. |
| Rest timer (floating) | Elevation 2. Progress ring in `accentChart`. |
| PR badge | `accentFill` background, `onAccentFill` text, optional trophy icon. |
| Progress bars | Track `surfaceBright`, indicator `accentChart`. |
| Status bar and nav bar | Transparent over `canvas`. Icons light on dark theme, dark on light theme. |

## 5. Contrast Reference (calculated)

| Pair | Ratio | Use |
|---|---|---|
| `#0C0E12` on `#C6FF3D` | 16.3:1 | `onAccentFill` on `accentFill` |
| `#C6FF3D` on `#0C0E12` (dark) | 16.3:1 | Lime on dark canvas |
| `#C6FF3D` on `#1A1C20` (dark) | 14.4:1 | Lime on dark surface |
| `#94A3B8` on `#1A1C20` (dark) | 6.7:1 | `textSecondary` |
| `#64748B` on `#1A1C20` (dark) | 3.6:1 | `textSubtle`: large or non-essential only |
| `#FF5C5C` on `#1A1C20` (dark) | 5.6:1 | `error` text |
| `#0C0E12` on `#F4F5F2` (light) | 17.7:1 | `textPrimary` |
| `#475569` on `#FFFFFF` (light) | 7.6:1 | `textSecondary` |
| `#64748B` on `#FFFFFF` (light) | 4.8:1 | `textSubtle` |
| `#4D7C0F` on `#FFFFFF` (light) | 5.0:1 | `accentText` |
| `#4D7C0F` on `#F4F5F2` (light) | 4.6:1 | `accentText` on canvas |
| `#3F6212` on `#EAF7C2` (light) | 6.3:1 | `accentTextOnTint` |
| `#65A30D` on `#FFFFFF` (light) | 3.1:1 | `accentChart` (graphics only, not text) |
| `#8A9183` on `#FFFFFF` (light) | 3.3:1 | `borderStrong` |
| `#D92D20` on `#FFFFFF` (light) | 4.8:1 | `error` text |
| `#C6FF3D` on `#FFFFFF` (light) | 1.2:1 | **Do not use for text, icons, or lines in the light theme** |
| `#4D7C0F` on `#ECEEE8` (light) | 4.3:1 | **Below 4.5:1**: use `accentTextOnTint` instead on `surfaceVariant` or tint |

## 6. Implementation

### 6.1 Token class

```kotlin
@Immutable
data class KumaColors(
    val canvas: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceBright: Color,
    val border: Color,
    val borderStrong: Color,
    val scrim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textSubtle: Color,
    val accentFill: Color,
    val accentFillPressed: Color,
    val onAccentFill: Color,
    val accentFillBorder: Color,
    val accentText: Color,
    val accentTextOnTint: Color,
    val accentChart: Color,
    val accentTint: Color,
    val error: Color,
    val onError: Color,
    val errorTint: Color,
    val chartSeriesMuted: Color,
    val chartGrid: Color,
    val chartThreshold: Color,
    val isDark: Boolean,
)

val DarkColors = KumaColors(
    canvas = Color(0xFF0C0E12),
    surface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFF22252A),
    surfaceBright = Color(0xFF37393E),
    border = Color(0xFF2A2D35),
    borderStrong = Color(0xFF6B7280),
    scrim = Color(0x99000000),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF94A3B8),
    textSubtle = Color(0xFF64748B),
    accentFill = Color(0xFFC6FF3D),
    accentFillPressed = Color(0xFFB3EB2A),
    onAccentFill = Color(0xFF0C0E12),
    accentFillBorder = Color.Transparent,
    accentText = Color(0xFFC6FF3D),
    accentTextOnTint = Color(0xFFC6FF3D),
    accentChart = Color(0xFFC6FF3D),
    accentTint = Color(0x24C6FF3D),
    error = Color(0xFFFF5C5C),
    onError = Color(0xFF0C0E12),
    errorTint = Color(0x24FF5C5C),
    chartSeriesMuted = Color(0xFF37393E),
    chartGrid = Color(0xFF2A2D35),
    chartThreshold = Color(0xFF64748B),
    isDark = true,
)

val LightColors = KumaColors(
    canvas = Color(0xFFF4F5F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECEEE8),
    surfaceBright = Color(0xFFE1E4DB),
    border = Color(0xFFD8DCD1),
    borderStrong = Color(0xFF8A9183),
    scrim = Color(0x660C0E12),
    textPrimary = Color(0xFF0C0E12),
    textSecondary = Color(0xFF475569),
    textSubtle = Color(0xFF64748B),
    accentFill = Color(0xFFC6FF3D),
    accentFillPressed = Color(0xFFB3EB2A),
    onAccentFill = Color(0xFF0C0E12),
    accentFillBorder = Color(0xFF9FD11F),
    accentText = Color(0xFF4D7C0F),
    accentTextOnTint = Color(0xFF3F6212),
    accentChart = Color(0xFF65A30D),
    accentTint = Color(0xFFEAF7C2),
    error = Color(0xFFD92D20),
    onError = Color(0xFFFFFFFF),
    errorTint = Color(0xFFFDE7E5),
    chartSeriesMuted = Color(0xFFD5D9CF),
    chartGrid = Color(0xFFE1E4DB),
    chartThreshold = Color(0xFF64748B),
    isDark = false,
)

val LocalKumaColors = staticCompositionLocalOf { DarkColors }
```

### 6.2 Theme wrapper

```kotlin
enum class ThemeMode { DARK, LIGHT, SYSTEM }

@Composable
fun KumaTheme(mode: ThemeMode, highContrast: Boolean = false, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = (if (dark) DarkColors else LightColors).let { if (highContrast) it.toHighContrast() else it }
    CompositionLocalProvider(LocalKumaColors provides colors) {
        // Map to MaterialTheme if Material 3 components are used, then provide typography and shapes.
        content()
    }
}

object KumaTheme {
    val colors: KumaColors @Composable get() = LocalKumaColors.current
}
```

- `toHighContrast()` should raise `border` to `borderStrong`, `textSecondary` and `textSubtle` to `textPrimary` equivalents at 85% and 70% strength, and keep accent tokens unchanged.
- Read `appearance.theme` and `appearance.highContrast` from `SettingsRepository` and pass them into `KumaTheme` at the app root.
- Update system bar icon appearance (Android: `WindowInsetsControllerCompat.isAppearanceLightStatusBars`; iOS: status bar style) whenever `isDark` changes.

## 7. Logo Usage per Theme

| Context | Asset |
|---|---|
| Dark theme, app icon, splash | Icon on dark tile and horizontal logo as designed (white "KUMA", lime "LIFT") |
| Light theme, app headers | Keep the dark-tile icon. For the horizontal logo use `#0C0E12` for "KUMA" and `#4D7C0F` for "LIFT". |
| Android adaptive icon | Foreground: bear and barbell in `#C6FF3D`. Background: solid `#0C0E12`. |

## 8. Rules and Checks for the Agent

1. Remove or replace any existing hardcoded colors; `grep` for `Color(0x` outside `core/designsystem`.
2. Add `@Preview` for each screen in both themes. Add screenshot tests if the project has them.
3. Add a unit test that computes the contrast of the critical token pairs in section 5 and fails if a text pair drops below 4.5:1, or a graphic/outline pair below 3:1.
4. Never use `accentFill` as a text, icon, or line color on a light surface. Use `accentText`, `accentTextOnTint`, or `accentChart`.
5. When a new color is needed, add a role token to `KumaColors` for **both** themes in the same change.
6. The theme must switch live without restarting the app, and must respect `SYSTEM` mode changes while the app is running.
