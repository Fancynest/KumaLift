# FORGE – Product Requirements Document & Project Brief

**Tagline:** Minimalist high-precision strength telemetry and training logger

## 1. Overview

| Item | Detail |
|---|---|
| Product name | FORGE |
| Platform | Mobile-first (iOS / Android), ~390px viewport baseline |
| Target users | Dedicated gym-goers, powerlifters, bodybuilders and strength athletes who prioritize speed, low friction during training and quantitative data over social or gamified features |
| Core value | Instant logging with one-handed gym UX, zero latency during active sets, automated 1RM estimation, fatigue tracking (RPE) and deep progress analytics |

## 2. Design System

**Direction:** "Iron & Electric Minimalist Athletic". High-contrast, industrial, low-light dark UI with a single electric accent.

### Color palette

| Role | Value |
|---|---|
| Canvas / background | `#0C0E12` (lowest), `#111317` (dim) |
| Cards and elevated surfaces | `#1A1C20` (low), `#22252A` (default), `#37393E` (bright) |
| Primary accent (Electric Lime) | `#C6FF3D` |
| Text, primary | `#FFFFFF` |
| Text, muted / labels | `#94A3B8`, `#64748B` |
| Borders | `#2A2D35` |

The lime accent is reserved for primary actions, active telemetry highlights, PR breakthroughs and completed sets. Do not use it decoratively.

### Typography

- **Font:** Inter, with tabular numerals.
- **Weights:** 700/800 for telemetry values and key metrics.

### Shape and ergonomics

- **Corner radius:** 12px (`rounded-xl`) to 16px (`rounded-2xl`) for cards; fully rounded for status badges and primary pill buttons.
- **Tap targets:** minimum 48px.
- **Layout:** bottom-weighted controls, optimized for one-handed use while holding weights or wearing lifting straps.

## 3. Information Architecture

### Global shell: bottom tab navigation

| Tab | Route | Purpose |
|---|---|---|
| Home | `/home` | Dashboard, streak, today's workout, recent history |
| Workouts | `/routines` | Mesocycle planner, routine blueprints, custom templates |
| Progress | `/analytics` | Estimated 1RM trajectory, weekly volume, personal records |
| Profile | `/profile` | Athlete settings, equipment calibration, cloud sync |

### Focused fullscreen flow (no bottom tabs)

| Screen | Route | Purpose |
|---|---|---|
| Active Workout Logger | `/workout/active` | Distraction-free logging with persistent rest timer and set matrix |
| Workout Summary | `/workout/summary` | Post-session debrief, PR announcements, volume distribution |

## 4. Feature Specifications by Screen

### 4.1 Home Dashboard

**Athlete header**
- Contextual greeting and active training block banner (e.g. "Week 6 · Hypertrophy Phase").
- Metric chips: streak (e.g. 4-day streak), weekly tonnage (e.g. 18.4k kg), adherence target (e.g. 80% met).

**Weekly horizon (7-day tracker)**
- M-T-W-T-F-S-S matrix showing completed sessions (lime check), rest days (bed icon), today's indicator and planned sessions.

**Today's workout card (hero)**
- Recommended session (e.g. "Pull Day A") with exercise count, total planned sets, estimated duration and target RPE tier (e.g. Heavy, RPE 8–9).
- Preview chips for the first three main compound movements.
- Full-width lime CTA: **START WORKOUT**.

**Monthly quick glance**
- Three metric cards: Total Sessions (e.g. 18), Total Volume (e.g. 64.2k kg, +12%), Broken PRs (e.g. 3).

**Recent workouts feed**
- Chronological list with volume, duration and top PR badge.
- Fallback action: **+ Log Empty / Freestyle Workout**.

### 4.2 Routines and Templates

**Mesocycle banner**
- Current block (e.g. "PPL Hypertrophy Phase 2"), current week (e.g. "Week 3 of 6"), adherence bar (e.g. 75%), and quick actions: **+ Create Routine**, **Explore**.

**Filtering**
- Category pills: All Routines, PPL Split (Active), Upper / Lower, Full Body.

**Routine cards**
- Split metadata, target muscle focus, exercise count, estimated duration and target RPE.
- Exercise preview list with overflow tag (e.g. "+3 more").
- Last performed timestamp (e.g. "Yesterday", "3 days ago").
- Lime action: **Start Workout**.

**Secondary blueprints**
- Archived or inactive routines with contextual options: duplicate, edit, reorder.

**Floating CTA**
- **+ NEW ROUTINE TEMPLATE**, within thumb reach.

### 4.3 Exercise Library

**Catalog and search**
- Live count indicator (e.g. "142 Indexed Movements", "10 of 142 Telemetry Ready").
- Search bar with instant auto-focus and fuzzy matching on movement name, target muscle and equipment.

**Filters**
- Muscle group: All, Chest, Back, Legs, Shoulders, Arms, Core.
- Equipment: Barbell, Dumbbell, Cable, Bodyweight, Machine.

**List items**
- Equipment badge (BB, DB, CB, BW).
- Movement name and primary muscle group.
- Current PR (e.g. "PR 265 LBS") and last logged date.
- One-tap **+** button to add to a routine or swap in-workout.

**Floating action**
- **+ Custom Movement** for niche exercises and gym-specific variations.

### 4.4 Active Workout Logger

**Top bar**
- Persistent workout timer, exercise index (e.g. "Exercise 1 of 5") and quick **Finish Workout** button.

**Movement card**
- Movement name, equipment, muscle category.
- Previous-session reference (e.g. "Prev: 115 kg × 3 @ RPE 8.5").

**Set matrix**
- Columns: Set #, Previous, Weight (kg/lbs), Reps, RPE (optional), Complete checkbox.
- Set types: Warmup (W), Working, Drop (D), Failure (F).
- Steppers for weight and reps with large tap targets and auto-increment.
- One-tap check marks the set complete (lime highlight) and starts the rest timer.

**Rest timer**
- Starts automatically on set completion, displayed as a floating countdown with **+30s / −15s** adjustments.

**Bottom actions**
- **+ Add Set**, **+ Add Exercise**, **Replace Exercise**, **Workout Notes**.

### 4.5 Workout Summary

**Header**
- "SESSION LOGGED" pill, session title (e.g. "Push Day Complete"), date/time window and overall session RPE (e.g. "RPE 8.5 · Peak Drive").

**Core metrics (2×2 grid)**
- Duration (e.g. 47:20, with delta vs target pace).
- Total volume (e.g. 14,850 kg, with delta vs previous identical split).
- Sets completed (e.g. 16 / 16, with adherence %).
- Pacing: average rest interval (e.g. 88 sec) and estimated calorie burn.

**PR showcase**
- Highlighted card for broken records with comparison to the previous best (e.g. "Barbell Bench Press: 118.0 kg, 3RM PR, +3.0 kg").

**Volume distribution**
- Segmented horizontal bar by muscle group (e.g. Chest 52%, Shoulders 28%, Triceps 20%).

**Movement breakdown**
- Per exercise: sets, top set weight and tonnage.

**Recovery guidance**
- Banner with post-session protein and hydration targets.

**Actions**
- Primary: **SAVE & FINISH WORKOUT** (lime).
- Secondary: **Share Story** (image export for social) and **Export Telemetry** (CSV / JSON).

### 4.6 Progress and Analytics

**Timeframe selector**
- Segmented control: 1M, 3M, 6M, 1Y, ALL.

**Estimated 1RM chart**
- Movement dropdown (e.g. Barbell Bench Press).
- Current estimated 1RM (e.g. 118.0 kg / 260 lbs) with delta (e.g. +7.5 kg / +6.8%).
- SVG spline graph of 1RM growth with milestone dots and a highlighted PR peak marker.

**Weekly tonnage and volume**
- Weekly average tonnage (e.g. 54,200 kg, +14% vs previous cycle).
- Split filters: All Splits, Push Focus, Pull Focus, Legs / Posterior.
- Six-week bar chart with a maintenance-threshold line and the current week highlighted.

**Hall of Fame (personal records)**
- Ranked cards for top compound and accessory records with rep range, estimated 1RM and date achieved.
- Link: **View All 14 Hall of Fame Milestones**.

## 5. Technical Requirements

### 5.1 Recommended stack

| Area | Recommendation |
|---|---|
| Frontend | React 19 / React Native, or a PWA, with Tailwind CSS, Lucide icons and Canvas/SVG charts for smooth rendering |
| Local storage | Local-first: IndexedDB / SQLite / WatermelonDB, so no workout data is lost in gyms without cell signal |
| Cloud sync | Background sync to PostgreSQL / Supabase / Firebase when connectivity returns |
| Calculations | Real-time estimated 1RM using Epley/Brzycki; volume = sets × reps × weight |

### 5.2 Phasing

| Phase | Scope | Status |
|---|---|---|
| 1. Design | Core 6 screens, unified design system, end-to-end user loop verified | Done |
| 2. Logging engine | Local workout state machine, background rest-timer notifications, watchOS / Wear OS companion widget | Planned |
| 3. Advanced | Coach dashboard sharing, video bar-path velocity tracking, 3D muscle fatigue heatmaps | Planned |

## 6. Notes for the Implementing Agent

- Treat the Stitch export (HTML and screenshots) as a **visual and design-system reference**. Re-implement in the target stack instead of copying the HTML directly.
- Start with design tokens (colors, typography, spacing, radii), then reusable components, then screens.
- Suggested build order: Active Workout Logger first (core loop), then Home, Routines, Exercise Library, Summary, Progress.
- Units (kg / lbs) should be a user setting; the mockups currently mix both.
