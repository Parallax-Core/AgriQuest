# Project Plan

AgriQuest Mobile - Complete 5-Screen Gamified IoT Agriculture App with Home, Plant Details, Daily Quests, AI Agro-Assistant Diagnostics, and Achievements/Awards Screens.

## Project Brief

# Project Brief: AgriQuest Mobile

## Features

1. **5-Tab Navigation & Core Hub**: State-driven navigation powering five core screens (Home, Plant Details, Daily Quests, AI Diagnostics, and Progress/Achievements) with seamless persistent tab switching.
2. **Plant Details & Telemetry Tracking**: Detailed crop telemetry dashboard (e.g., Roma Rosso - 92% Health, Day 47, Stage 3) featuring live IoT sensor reading grids, moisture history timeline charts, and Stats/Journal toggle views.
3. **Gamified Daily Quests & Progression**: Interactive daily task system ("Prune Lower Leaves" +50 XP, "Deep Watering Session" +75 XP, "Stake the Main Stem" +100 XP) driving leveling ("LEVEL 7 Seedling Sage"), 14-day care streaks, rank tracking, and achievement badges.
4. **AI Agro-Assistant Diagnostics**: Camera leaf scanner supporting offline AI model readiness (PlantNet-Tomato v1.2), real-time IoT condition integration, and recent diagnosis history logs (e.g., "Healthy Leaf" 97%, "Early Blight" 89%).

## High-Level Tech Stack

- **Programming Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Navigation**: Jetpack Navigation 3 (state-driven)
- **Adaptive Strategy**: Compose Material Adaptive (`androidx.compose.material3.adaptive`)
- **State & Concurrency**: Kotlin Coroutines, `StateFlow`, Jetpack `ViewModel` (MVVM / Unidirectional Data Flow)

## Implementation Steps

### Task_1_PlantDetailsScreen: Construct Plant Details Screen (Plant tab) with Roma Rosso header, 92% plant health card, Day counter, Stats / Journal toggle tabs, live readings grid, and Moisture History graph.
- **Status:** COMPLETED
- **Updates:** PlantDetailsScreen created with Roma Rosso header, 92% plant health bar, Day 47 counter, Stage 3 Mature tag, Stats / Journal tab toggle, Live Readings grid, Moisture History chart/timeline, and Journal care log. Build and tests passed.
- **Acceptance Criteria:**
  - PlantDetailsScreen created with Roma Rosso header and 92% health indicator
  - Stats / Journal tab toggle functionality implemented
  - Live readings 2x2 grid and Moisture History chart rendered

### Task_2_DailyQuestsScreen: Construct standalone Daily Quests Screen (Quests tab) with +225 XP Available banner, Prune Lower Leaves, Deep Watering Session, and Stake Main Stem quest cards with Mark Complete actions.
- **Status:** COMPLETED
- **Updates:** QuestsScreen created with DAILY - Quests header, +225 XP Available Gold Banner, 3 specific quest cards (Prune Lower Leaves +50 XP, Deep Watering Session +75 XP, Stake Main Stem +100 XP) with difficulty badges, progress bars, Mark Complete actions, ViewModel XP awarding, and DataStore persistence. Build and unit tests passed.
- **Acceptance Criteria:**
  - QuestsScreen created with DAILY - Quests header and +225 XP banner
  - Quest items rendered with progress bars and Mark Complete buttons
  - Marking quests complete awards XP, updates level progression, and persists in state

### Task_3_AiDiagnosticsScreen: Construct AI Agro-Assistant Diagnostics Screen (AI tab) with Offline AI status chip, Scan Tomato Leaf camera card, IoT conditions summary, and Recent Scans list.
- **Status:** COMPLETED
- **Updates:** AiDiagnosticsScreen created with DIAGNOSTICS header, Offline AI Ready status chip (PlantNet-Tomato v1.2), Scan Tomato Leaf emerald camera card with interactive viewfinder scan simulation (+25 XP), Current IoT Conditions summary card, and Recent Scans list (Healthy Leaf 97%, Early Blight 89%). Build and tests passed.
- **Acceptance Criteria:**
  - AiDiagnosticsScreen created with Offline AI Ready chip (PlantNet-Tomato v1.2)
  - Scan Tomato Leaf camera hero card with interactive scan simulation implemented
  - Current IoT conditions summary and Recent Scans list rendered

### Task_4_AchievementsAwardsScreenAndStreak: Construct Achievements Screen (Awards tab) with Level 7 Seedling Sage card, stat chips, functional care streak with Reset Streak button, and unlocked achievements list.
- **Status:** COMPLETED
- **Updates:** AwardsScreen created with PROGRESS header, Dark Emerald Hero Card displaying level title and care streak, functional Reset Streak to 0 button, 3 quick stat chips (3/6 Unlocked, 375 Total XP, #42 Rank), and pre-populated achievements list (First Plant, Water Hero, Sun Seeker). Functional streak state connected to ViewModel and DataStore persistence. Build and 53 unit tests passed.
- **Acceptance Criteria:**
  - AwardsScreen created with LEVEL 7 Seedling Sage card and stat chips
  - Functional care streak tracking with Reset Streak to 0 button added
  - Achievements list (First Plant, Water Hero, Sun Seeker) rendered with unlocked badges

### Task_5_BottomNavAndNavigationHost: Update AgriQuestBottomNav to 5 tabs (Home, Plant, Quests, AI, Awards) and wire state-driven tab switching in DashboardScreen / MainActivity.
- **Status:** COMPLETED
- **Updates:** AgriQuestBottomNav updated to 5 tabs (Home, Plant, Quests, AI, Awards) with emerald active tab pill styling. State-driven screen switcher wired in DashboardScreen to seamlessly render all 5 screens. Capped at max 430dp width. Build and 55 unit tests passed.
- **Acceptance Criteria:**
  - AgriQuestBottomNav updated to 5 tabs matching design mockups
  - Tapping bottom nav tabs switches screens smoothly between Home, Plant, Quests, AI, and Awards
- **Duration:** N/A

### Task_6_RunAndVerify: Build, run unit test suite, and instruct critic_agent to verify all 5 screens and 5-tab navigation.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - ./gradlew assembleDebug and ./gradlew testDebugUnitTest pass cleanly
  - Application verified across all 5 screens with no crashes
- **StartTime:** 2026-09-19 11:14:32 CST

