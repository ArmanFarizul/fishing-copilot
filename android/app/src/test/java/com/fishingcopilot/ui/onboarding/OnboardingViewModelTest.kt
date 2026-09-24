package com.fishingcopilot.ui.onboarding

import com.fishingcopilot.data.local.SpotEntity
import com.fishingcopilot.data.profile.Avatar
import com.fishingcopilot.data.profile.FishingStyle
import com.fishingcopilot.data.profile.ProfileRepository
import com.fishingcopilot.data.profile.Species
import com.fishingcopilot.data.profile.UserProfile
import com.fishingcopilot.data.spots.CoastalArea
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private class FakeProfileRepository : ProfileRepository {
        override val profile = MutableStateFlow<UserProfile?>(null)
        var saveCount = 0

        override suspend fun save(profile: UserProfile) {
            saveCount++
            this.profile.value = profile
        }
    }

    private lateinit var repository: FakeProfileRepository
    private lateinit var viewModel: OnboardingViewModel
    private val savedSpots = mutableListOf<SpotEntity>()

    private val state get() = viewModel.uiState.value

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        repository = FakeProfileRepository()
        viewModel = OnboardingViewModel(repository) { spot ->
            savedSpots += spot
            42L
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `blank nickname blocks the first step`() {
        viewModel.onNicknameChange("   ")
        assertFalse(state.canContinue)

        viewModel.next()
        assertEquals(0, state.step)
    }

    @Test
    fun `nickname is capped at the maximum length`() {
        viewModel.onNicknameChange("a".repeat(OnboardingUiState.NICKNAME_MAX_LENGTH + 5))
        assertEquals(OnboardingUiState.NICKNAME_MAX_LENGTH, state.nickname.length)
    }

    @Test
    fun `each step requires its own answer before advancing`() {
        viewModel.onNicknameChange("Arman")
        viewModel.next()
        assertEquals(1, state.step)
        assertFalse(state.canContinue)

        viewModel.onFishingStyleSelect(FishingStyle.ESTUARY)
        viewModel.next()
        assertEquals(2, state.step)
        assertFalse(state.canContinue)

        viewModel.onSpeciesToggle(Species.SIAKAP)
        assertTrue(state.canContinue)
    }

    @Test
    fun `toggling a species twice deselects it`() {
        viewModel.onSpeciesToggle(Species.PARI)
        viewModel.onSpeciesToggle(Species.PARI)
        assertTrue(state.targetSpecies.isEmpty())
    }

    @Test
    fun `back never goes below the first step`() {
        viewModel.back()
        assertEquals(0, state.step)
    }

    private fun completeFirstThreeSteps() {
        viewModel.onNicknameChange("  Arman  ")
        viewModel.onAvatarSelect(Avatar.KAYAK)
        viewModel.next()
        viewModel.onFishingStyleSelect(FishingStyle.BOAT)
        viewModel.next()
        viewModel.onSpeciesToggle(Species.TENGGIRI)
        viewModel.onSpeciesToggle(Species.KERAPU)
        viewModel.next()
    }

    @Test
    fun `spot step needs a selection and a name`() {
        completeFirstThreeSteps()
        assertEquals(3, state.step)
        assertFalse(state.canContinue)

        viewModel.onSpotSelect(SpotSelection.Area(CoastalArea.KUKUP), defaultName = "Kukup")
        assertTrue(state.canContinue)

        viewModel.onSpotNameChange("   ")
        assertFalse(state.canContinue)
    }

    @Test
    fun `choosing another spot replaces the default name but keeps a typed one`() {
        completeFirstThreeSteps()
        viewModel.onSpotSelect(SpotSelection.Area(CoastalArea.KUKUP), defaultName = "Kukup")
        viewModel.onSpotSelect(SpotSelection.Area(CoastalArea.MUAR), defaultName = "Muar")
        assertEquals("Muar", state.spotName)

        viewModel.onSpotNameChange("Jeti Kukup")
        viewModel.onSpotSelect(SpotSelection.Area(CoastalArea.KUKUP), defaultName = "Kukup")
        assertEquals("Jeti Kukup", state.spotName)
    }

    @Test
    fun `finishing saves the spot, then the trimmed profile pointing at it, exactly once`() {
        completeFirstThreeSteps()
        viewModel.onSpotSelect(SpotSelection.Point(1.3301, 103.4402), defaultName = "Spot saya")
        viewModel.onSpotNameChange("  Jeti Kukup ")
        assertNull(repository.profile.value)

        viewModel.next()
        viewModel.next()

        assertEquals(
            listOf(SpotEntity(name = "Jeti Kukup", latitude = 1.3301, longitude = 103.4402, isFavorite = true, createdAt = savedSpots.single().createdAt)),
            savedSpots
        )
        assertEquals(
            UserProfile(
                nickname = "Arman",
                avatar = Avatar.KAYAK,
                fishingStyle = FishingStyle.BOAT,
                targetSpecies = setOf(Species.TENGGIRI, Species.KERAPU),
                homeSpotId = 42L
            ),
            repository.profile.value
        )
        assertEquals(1, repository.saveCount)
    }
}
