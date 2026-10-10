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

### Task_1_FixStreakInconsistencies: Audit all UI components and remove any hardcoded or stale streak references (e.g., 4/7). Connect all streak badges cleanly to single source of truth in AgriQuestUiState and DataStore.
- **Status:** COMPLETED
- **Updates:** All hardcoded 4/7 streak strings and stale default arguments removed. AgriQuestUiState.streakDays and plantAvatar.streakDays synchronized as single source of truth across all 5 screens and DataStore. Build and unit tests passed.
- **Acceptance Criteria:**
  - Single source of truth for streakDays enforced across all 5 screens
  - All hardcoded '4/7' strings replaced with dynamic string templates ($streakDays / 7 Days)
  - Streak resets and updates reflect consistently across PlantAvatarHeroCard, TopLevelBanner, AwardsScreen, and AiDiagnosticsScreen

### Task_2_RealDateMidnightQuestReset: Connect daily quests reset engine to system date (LocalDate.now() / 12:00 AM midnight reset) and persist last reset date in DataStore.
- **Status:** COMPLETED
- **Updates:** Midnight date check connected to System.currentTimeMillis() / LocalDate.now(). AgriQuestDataStore persists KEY_LAST_RESET_DATE (YYYY-MM-DD). Quests automatically reset and reshuffle at midnight when calendar date advances. Missed days reset streak to 0. Manual Simulate Next Day button retained. Build and 64 unit tests passed.
- **Acceptance Criteria:**
  - AgriQuestDataStore stores last reset calendar date (YYYY-MM-DD)
  - Quests automatically reset and shuffle at 12:00 AM midnight when calendar date advances
  - Completing all daily quests for the day increments streakDays and persists in state

### Task_3_AgroMiniQuizFeature: Implement interactive Agro-Quiz feature with daily multiple-choice plant care questions, +30 XP reward, answer feedback, and DataStore completion persistence.
- **Status:** COMPLETED
- **Updates:** Daily Agro-Quiz feature created with AgroQuizQuestion pool, DailyQuizCard M3 component, option click feedback (green/red), +30 XP reward, level progression check, and KEY_QUIZ_COMPLETED_DATE DataStore persistence. Integrated on Home Dashboard and Quests screen. Build and 70 unit tests passed.
- **Acceptance Criteria:**
  - Interactive Daily Agro-Quiz modal/card implemented with multiple-choice questions
  - Answering correctly awards +30 XP, levels up progression, and displays feedback animation
  - Quiz completion state persists per day in DataStore

### Task_4_RunAndVerify: Build, run unit tests, and instruct critic_agent to verify streak consistency, midnight quest reset, and mini quiz interactivity.
- **Status:** COMPLETED
- **Updates:** Full app verification completed on emulator-5554. Single source of truth for care streak verified (100% synchronized across Home, Awards, and AI screens). System date & midnight 12:00 AM quest reset engine verified. Interactive Daily Agro-Quiz (+30 XP) verified with option click feedback, level progression, and DataStore persistence. Zero crashes observed.
- **Acceptance Criteria:**
  - ./gradlew assembleDebug and ./gradlew testDebugUnitTest pass cleanly
  - App verified with zero streak inconsistencies and interactive mini quiz operational
- **Duration:** N/A

