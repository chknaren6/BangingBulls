# Banging Bulls — Skill Games + Stocks Playground

Banging Bulls is an Android app that blends quick, skill‑based mini‑games with a lightweight stocks playground. It features a slick, immersive UI, passwordless authentication, daily rewards, and a modern Kotlin + Jetpack Compose architecture.

---

## Platform
- Android (min SDK as per project)

## Language
- Kotlin

## UI
- Jetpack Compose, Material 3

## Auth
- Google/Firebase (passwordless flow)

## Data
- Firebase Firestore

## Async
- Kotlin Coroutines

## Navigation
- Compose Navigation

## Background Work
- WorkManager (periodic leaderboard refresh)

## Media
- Asset-based audio (intro, coin/lose sounds)

---

## Screens Include
- Splash with animated logo, loader, and intro sound
- Auth (passwordless) with branded background
- Username input (first login)
- One‑time intro card (explains the game in two lines)
- Home hub (leaderboard, portfolio, quick links)
- Games: Crash/Rocket, Dice (Hi‑Lo), Limbo, Coin Flip
- Stocks: list, detail, user holdings portfolio
- Profile: avatar, username edit, coins, quick actions

---

## Download


Upload the signed release APK/AAB to GitHub Releases and ensure the link above points to the latest tag.

---

## Demo Images

Place these images in the repo under `docs/images/` and reference them below:

| Screen | Image |
|--------|-------|
| Splash | `docs/images/splash.png` |
| Auth | `docs/images/auth.png` |
| Username | `docs/images/username.png` |
| Intro Card | `docs/images/intro.png` |
| Home + Leaderboard | `docs/images/home.png` |
| Game — Crash | `docs/images/game_crash.png` |
| Game — Dice | `docs/images/game_dice.png` |
| Stocks — List | `docs/images/stocks_list.png` |
| Stocks — Detail | `docs/images/stocks_detail.png` |
| Profile | `docs/images/profile.png` |

Example gallery:

![Splash](docs/images/splash.png)  
![Game — Crash](docs/images/game_crash.png)  
![Stocks — List](docs/images/stocks_list.png)

---

## Features

### Authentication
- Google sign‑in, auto‑routing based on user profile existence and `introSeen` flag.

### One-Time Intro
- After username creation, a single intro card is shown and never again (`introSeen` in Firestore).

### Immersive UI
- Full‑bleed backgrounds, transparent overlays, white iconography, custom top/bottom bars with image backgrounds.

### Games
- Crash/Rocket with frame‑synchronized audio, win/loss sounds triggered after UI state applies.
- Dice/Hi‑Lo, Limbo, Coin Flip with coin burst animations on win.

### Daily Limits + Rewards
- Plays/day limit with countdown and daily coin claims.

### Stocks
- Compact cards with responsive typography, holdings tracking, portfolio table in a cream card.

### Profile
- Avatar, edit username, coins/lifetime, quick actions.

### Background Work
- Periodic leaderboard refresh via WorkManager.

### Audio
- `intro.mp3` on splash; coin/lose sounds; preloaded MediaPlayer with safe reuse logic.

---

## Tech Stack
- Kotlin, Coroutines, Flow
- Jetpack Compose (Material 3)
- Navigation Compose (animated transitions for immersive routes)
- Firebase Authentication (Google)
- Firebase Firestore
- WorkManager
- MediaPlayer (asset sounds)

---

## Project Structure (high-level)
- Splash/ — splash screen and audio helpers
- Authentication/
- V/ — Auth UI (AuthScreen, UsernameInputScreen, IntroCardScreen)
- VM/ — Auth view model
- Home/
- Game/V/ — game UIs
- Stocks/StockFiles/
- V/ — stocks screens
- VM/ — stocks view model
- M/ — repository and models
- Profile/ — profile UI
- Navigation/ — app nav host(s)
- UserViewModel — user session, listeners, clearSession

