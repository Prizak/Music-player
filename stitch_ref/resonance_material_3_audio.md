---
name: "Resonance Material 3 Audio"
description: "A refined Material Design 3 design system for a native Android music player app with dark-theme elevation, expressive tonal containers, and high-contrast vibrant accents."
---

## Overview

Resonance is a native Android music player following Material Design 3 (M3) specifications. Built for audiophiles and everyday music lovers who demand fluid local MP3 playback, deep rankings, and comprehensive audio settings. The aesthetic balances M3 tonal surfaces, pill shapes, floating action buttons, standard bottom navigation, and high-contrast accents (vibrant electric amber and dynamic cyan-violet).

## Colors

The color palette embraces Material Design 3's tonal container architecture, featuring deep neutral dark surfaces, dynamic electric violet-cyan accents, and vibrant neon amber for highlights and ranking badges.

- **Primary:** Electric Periwinkle / Violet accenting key active states and floating controls.
- **Secondary:** Cyan highlight for ranking metrics and active track badges.
- **Tertiary / Amber Accent:** Vibrant golden amber for leaderboards, top ranks, and favorite indicators.
- **Surface & Containers:** M3 surface elevation tiers ranging from deep obsidian base to elevated tonal containers.

```yaml
colors:
  accent: "#FFB800"
  accent-rank-1: "#FFD700"
  accent-rank-2: "#C0C0C0"
  accent-rank-3: "#CD7F32"
  cyan-glow: "#00E5FF"
  coral-glow: "#FF4D6D"
  amber-glow: "#FFAA00"
  light:
    primary: "#E63956"
    on-primary: "#121212"
    primary-container: "#FFE8EC"
    on-primary-container: "#5C0015"
    secondary: "#006874"
    on-secondary: "#FFFFFF"
    secondary-container: "#97F0FF"
    on-secondary-container: "#001F24"
    surface: "#FAF8FD"
    surface-dim: "#DCD8E0"
    surface-bright: "#FAF8FD"
    surface-container-lowest: "#FFFFFF"
    surface-container-low: "#F4F2F8"
    surface-container: "#EEEBF2"
    surface-container-high: "#E8E5EC"
    surface-container-highest: "#E2DFE7"
    on-surface: "#1B1B1F"
    on-surface-variant: "#47464F"
    outline: "#777680"
    outline-variant: "#C8C5D0"
  dark:
    primary: "#C3C0FF"
    on-primary: "#231B98"
    primary-container: "#3B35B4"
    on-primary-container: "#E3DFFF"
    secondary: "#4FD8EB"
    on-secondary: "#00363D"
    secondary-container: "#004F58"
    on-secondary-container: "#97F0FF"
    surface: "#121316"
    surface-dim: "#121316"
    surface-bright: "#38393D"
    surface-container-lowest: "#0D0E11"
    surface-container-low: "#1A1B1F"
    surface-container: "#1E1F24"
    surface-container-high: "#292A2F"
    surface-container-highest: "#34353A"
    on-surface: "#E4E2E6"
    on-surface-variant: "#C8C5D0"
    outline: "#918F9A"
    outline-variant: "#47464F"
```

## Typography

This system pairs **Jost** for expressive geometric headlines and display titles with **Instrument Sans** for legible metadata, durations, and UI labels. Monospace figures use **Source Code Pro**.

```yaml
typography:
  display-lg:
    fontFamily: "Jost"
    fontSize: 40px
    fontWeight: 700
    lineHeight: 48px
    letterSpacing: -0.25px
  display-lg-mobile:
    fontFamily: "Jost"
    fontSize: 32px
    fontWeight: 700
    lineHeight: 38px
    letterSpacing: -0.25px
  headline-lg:
    fontFamily: "Jost"
    fontSize: 34px
    fontWeight: 600
    lineHeight: 40px
    letterSpacing: 0px
  headline-lg-mobile:
    fontFamily: "Jost"
    fontSize: 28px
    fontWeight: 600
    lineHeight: 34px
    letterSpacing: 0px
  headline-md:
    fontFamily: "Jost"
    fontSize: 24px
    fontWeight: 600
    lineHeight: 30px
    letterSpacing: 0px
  headline-sm:
    fontFamily: "Jost"
    fontSize: 20px
    fontWeight: 600
    lineHeight: 26px
    letterSpacing: 0px
  title-lg:
    fontFamily: "Jost"
    fontSize: 18px
    fontWeight: 600
    lineHeight: 24px
    letterSpacing: 0px
  title-md:
    fontFamily: "Instrument Sans"
    fontSize: 16px
    fontWeight: 600
    lineHeight: 22px
    letterSpacing: 0.15px
  title-sm:
    fontFamily: "Instrument Sans"
    fontSize: 14px
    fontWeight: 600
    lineHeight: 20px
    letterSpacing: 0.1px
  body-lg:
    fontFamily: "Instrument Sans"
    fontSize: 16px
    fontWeight: 400
    lineHeight: 24px
    letterSpacing: 0.25px
  body-md:
    fontFamily: "Instrument Sans"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 20px
    letterSpacing: 0.25px
  body-sm:
    fontFamily: "Instrument Sans"
    fontSize: 12px
    fontWeight: 400
    lineHeight: 16px
    letterSpacing: 0.4px
  label-lg:
    fontFamily: "Instrument Sans"
    fontSize: 14px
    fontWeight: 600
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: "Instrument Sans"
    fontSize: 12px
    fontWeight: 500
    lineHeight: 16px
    letterSpacing: 0.5px
  label-sm:
    fontFamily: "Instrument Sans"
    fontSize: 11px
    fontWeight: 500
    lineHeight: 14px
    letterSpacing: 0.5px
```

## Layout & Spacing

Mobile Android layout follows a responsive single-column 390px viewport baseline with 16px lateral padding and M3 8dp/4dp modular spacing increments.

```yaml
spacing:
  gutter-xs: 4px
  gutter-sm: 8px
  gutter-md: 12px
  gutter-lg: 16px
  gutter-xl: 24px
  section: 32px
  page-margin: 16px
```

## Elevation & Depth

Material 3 uses surface tonal color shifts rather than heavy drop shadows. Surfaces elevate through `surface-container-low`, `surface-container`, `surface-container-high`, and `surface-container-highest`. FABs and bottom sheets use ambient subtle diffusion for crisp separation.

## Shapes

M3 signature shapes include pill-shaped search bars (`rounded-full`), rounded container cards (`rounded-2xl` to `rounded-3xl`), squircle album thumbnails (`rounded-xl`), and full-rounded floating action buttons.

```yaml
rounded:
  sm: 8px
  DEFAULT: 12px
  md: 16px
  lg: 20px
  xl: 24px
  full: 9999px
```

## Motion & Micro-interaction

Micro-interactions feel crisp and physical according to Android 14+ Predictive Back and M3 standard easing curves.

```yaml
motion:
  state-change:
    duration: 150ms
    delay: 0ms
    timingFunction: cubic-bezier(0.2, 0, 0, 1)
  surface-enter:
    duration: 250ms
    delay: 0ms
    timingFunction: cubic-bezier(0.05, 0.7, 0.1, 1)
  surface-exit:
    duration: 150ms
    delay: 0ms
    timingFunction: cubic-bezier(0.3, 0, 0.8, 0.15)
```

## Voice & Content Strategy

The app communicates in concise, clear, modern English. Technical audio settings express transparent bitrates, codec labels, and exact storage quantities. Songs display authentic metadata (artist, title, durations like 03:42, play counts like 1.4M).

## Components

Component implementations live as individual HTML sticker sheet files in
`components/`. Each file contains the canonical, styled HTML components with
`<!-- COMPONENT: name -->` comment markers. Read each file before use.

- **Button** → `@components/button.html`: `button-primary`, `button-icon`, `button-with-icon`
- **Card** → `@components/card.html`: `card-elevated`, `card-action`, `card-interactive`, `card-interactive-2`
- **List** → `@components/list.html`: `list-single`, `list-two-line`, `list-leading-icon`, `list-trailing-action`, `list-interactive`, `list-interactive-2`
- **Searchbar** → `@components/searchbar.html`: `searchbar-standard`, `searchbar-with-suggestions`
