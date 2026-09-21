import com.agriquest.data.repository.AgriQuestRepositoryImpl
import com.agriquest.ui.viewmodel.AgriQuestViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AwardsScreenUnitTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun awardsScreen_defaultUiState_containsPrePopulatedAchievementsAndStats() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        val state = viewModel.uiState.value

        // Pre-populated achievements list verification
        assertEquals(6, state.achievements.size)
        assertEquals(3, state.unlockedAchievementsCount)
        assertEquals(6, state.totalAchievementsCount)
        assertEquals(375, state.unlockedXpTotal)

        // Verify top 3 unlocked achievements match design specifications
        val ach1 = state.achievements[0]
        assertEquals("First Plant", ach1.title)
        assertEquals("Started your first tomato growing journey.", ach1.description)
        assertEquals(100, ach1.xpReward)
        assertTrue(ach1.isUnlocked)
        assertEquals("Jul 29", ach1.earnedDate)

        val ach2 = state.achievements[1]
        assertEquals("Water Hero", ach2.title)
        assertEquals("Watered your plant 10 times without missing a day.", ach2.description)
        assertEquals(150, ach2.xpReward)
        assertTrue(ach2.isUnlocked)
        assertEquals("Aug 12", ach2.earnedDate)

        val ach3 = state.achievements[2]
        assertEquals("Sun Seeker", ach3.title)
        assertEquals("Maintained optimal light for 5 consecutive days.", ach3.description)
        assertEquals(125, ach3.xpReward)
        assertTrue(ach3.isUnlocked)
        assertEquals("Aug 20", ach3.earnedDate)
    }

    @Test
    fun awardsScreen_resetStreakAction_resetsCareStreakToZero() = runTest {
        val repo = AgriQuestRepositoryImpl()
        val viewModel = AgriQuestViewModel(repo)

        // Initial streak state
        assertEquals(0, viewModel.uiState.value.plant.streakDays)

        // Increment streak first
        viewModel.incrementStreak()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.plant.streakDays)

        // Trigger streak reset action
        viewModel.resetStreak()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify care streak days is reset to 0!
        assertEquals(0, viewModel.uiState.value.plant.streakDays)
    }
}
