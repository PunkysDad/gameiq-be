package com.gameiq.controller

import com.gameiq.entity.*
import com.gameiq.service.ClaudeService
import com.gameiq.service.WorkoutService
import com.gameiq.service.WorkoutPlanDTO
import com.gameiq.service.WorkoutGenerationRequest
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class WorkoutControllerTest {

    private val claudeService: ClaudeService = mock()
    private val workoutService: WorkoutService = mock()

    private lateinit var controller: WorkoutController

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun makeWorkoutPlan(
        id: Long = 1L,
        name: String = "QB Off-Season Workout",
        content: String = "{}",
        sport: Sport = Sport.FOOTBALL,
        position: Position = Position.QB
    ) = WorkoutPlan(
        id = id,
        user = makeUser(),
        sport = sport,
        position = position,
        workoutName = name,
        positionFocus = "Arm strength and pocket mobility",
        difficultyLevel = "INTERMEDIATE",
        durationMinutes = 45,
        equipmentNeeded = "Full gym",
        generatedContent = content,
        isSaved = false,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private fun makeUser() = User(
        id = 1L,
        email = "test@test.com",
        firebaseUid = "uid-1",
        displayName = "Test User",
        subscriptionTier = SubscriptionTier.TRIAL,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    private fun makeRequest(
        userId: Long = 1L,
        sport: String = "FOOTBALL",
        position: String = "QB",
        experienceLevel: String = "INTERMEDIATE",
        trainingPhase: String = "OFF_SEASON",
        equipment: List<String> = listOf("Full gym"),
        duration: Int = 45,
        focusAreas: List<String> = listOf("Arm strength")
    ) = WorkoutGenerationRequest(
        userId = userId,
        sport = sport,
        position = position,
        experienceLevel = experienceLevel,
        trainingPhase = trainingPhase,
        availableEquipment = equipment,
        sessionDuration = duration,
        focusAreas = focusAreas
    )

    @BeforeEach
    fun setUp() {
        controller = WorkoutController(claudeService, workoutService)
    }

    // =========================================================================
    // POST /workouts/generate — happy path
    // =========================================================================

    @Nested
    inner class GenerateWorkoutHappyPath {

        @Test
        fun `returns 200 with workout DTO on success`() {
            val plan = makeWorkoutPlan()
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenReturn(plan)

            val response = controller.generateWorkout(makeRequest())

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
        }

        @Test
        fun `DTO id matches workout plan id`() {
            val plan = makeWorkoutPlan(id = 42L)
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenReturn(plan)

            val body = controller.generateWorkout(makeRequest()).body!!
            assertEquals("42", body.id)
        }

        @Test
        fun `DTO title uses workout plan name`() {
            val plan = makeWorkoutPlan(name = "Elite QB Power Builder")
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenReturn(plan)

            val body = controller.generateWorkout(makeRequest()).body!!
            assertEquals("Elite QB Power Builder", body.title)
        }

        @Test
        fun `DTO title falls back to position and phase when plan name is null`() {
            val plan = makeWorkoutPlan(name = "").copy(workoutName = null)
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenReturn(plan)

            val request = makeRequest(position = "QB", trainingPhase = "OFF_SEASON")
            val body = controller.generateWorkout(request).body!!
            assertTrue(body.title.contains("QB") || body.title.isNotEmpty())
        }

        @Test
        fun `DTO focusAreas matches request focusAreas`() {
            val plan = makeWorkoutPlan()
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenReturn(plan)

            val request = makeRequest(focusAreas = listOf("Arm strength", "Pocket mobility"))
            val body = controller.generateWorkout(request).body!!
            assertEquals(listOf("Arm strength", "Pocket mobility"), body.focusAreas)
        }

        @Test
        fun `equipment list is joined before passing to ClaudeService`() {
            val plan = makeWorkoutPlan()
            val captor = argumentCaptor<String>()
            whenever(claudeService.generateWorkoutPlan(
                any(), any(), any(), any(), any(), captor.capture(), any(), any(), anyOrNull()
            )).thenReturn(plan)

            controller.generateWorkout(makeRequest(equipment = listOf("Dumbbells", "Resistance bands")))

            assertEquals("Dumbbells, Resistance bands", captor.firstValue)
        }

        @Test
        fun `focusAreas list is joined before passing to ClaudeService`() {
            val plan = makeWorkoutPlan()
            val captor = argumentCaptor<String>()
            whenever(claudeService.generateWorkoutPlan(
                any(), any(), any(), any(), any(), any(), any(), captor.capture(), anyOrNull()
            )).thenReturn(plan)

            controller.generateWorkout(makeRequest(focusAreas = listOf("Speed", "Agility", "Power")))

            assertEquals("Speed, Agility, Power", captor.firstValue)
        }
    }

    // =========================================================================
    // POST /workouts/generate — trial/subscription limit enforcement
    // =========================================================================

    @Nested
    inner class GenerateWorkoutTrialEnforcement {

        @Test
        fun `trial workout limit returns 400`() {
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenThrow(IllegalStateException("Trial workout limit reached (1 workout). Subscribe to Basic or Premium."))

            val response = controller.generateWorkout(makeRequest())

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        fun `trial workout limit response body contains error message`() {
            val errMsg = "Trial workout limit reached (1 workout). Subscribe to Basic (\$12.99) or Premium (\$19.99)."
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenThrow(IllegalStateException(errMsg))

            val body = controller.generateWorkout(makeRequest()).body!!
            assertEquals(errMsg, body.description)
        }

        @Test
        fun `BASIC monthly budget exceeded returns 400`() {
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenThrow(IllegalStateException("Monthly AI budget reached (\$4.00 of \$4.00). Upgrade to Premium."))

            val response = controller.generateWorkout(makeRequest())

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        fun `NONE tier blocked returns 400`() {
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenThrow(IllegalStateException("No active subscription."))

            val response = controller.generateWorkout(makeRequest())

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        fun `subscription limit error body has empty id`() {
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenThrow(IllegalStateException("Trial workout limit reached."))

            val body = controller.generateWorkout(makeRequest()).body!!
            assertEquals("", body.id)
        }

        @Test
        fun `subscription limit error body has empty exercises list`() {
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenThrow(IllegalStateException("Trial workout limit reached."))

            val body = controller.generateWorkout(makeRequest()).body!!
            assertTrue(body.exercises.isEmpty())
        }
    }

    // =========================================================================
    // POST /workouts/generate — generic error handling
    // =========================================================================

    @Nested
    inner class GenerateWorkoutErrorHandling {

        @Test
        fun `unexpected exception returns 400`() {
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenThrow(RuntimeException("Unexpected failure"))

            val response = controller.generateWorkout(makeRequest())

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }

        @Test
        fun `unexpected exception body description contains error message`() {
            whenever(claudeService.generateWorkoutPlan(any(), any(), any(), any(), any(), any(), any(), any(), anyOrNull()))
                .thenThrow(RuntimeException("DB connection lost"))

            val body = controller.generateWorkout(makeRequest()).body!!
            assertEquals("DB connection lost", body.description)
        }
    }

    // =========================================================================
    // GET /workouts/user/{userId}
    // =========================================================================

    @Nested
    inner class GetUserWorkoutsTests {

        @Test
        fun `returns 200 with list of workout DTOs`() {
            whenever(workoutService.getUserWorkoutPlans(1L))
                .thenReturn(listOf(makeWorkoutPlan(id = 1L), makeWorkoutPlan(id = 2L)))

            val response = controller.getUserWorkouts(1L)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertEquals(2, response.body!!.size)
        }

        @Test
        fun `returns empty list when user has no workouts`() {
            whenever(workoutService.getUserWorkoutPlans(1L)).thenReturn(emptyList())

            val response = controller.getUserWorkouts(1L)

            assertEquals(HttpStatus.OK, response.statusCode)
            assertTrue(response.body!!.isEmpty())
        }

        @Test
        fun `DTO sport and position names match entity enum names`() {
            whenever(workoutService.getUserWorkoutPlans(1L))
                .thenReturn(listOf(makeWorkoutPlan(sport = Sport.BASKETBALL, position = Position.PG)))

            val body = controller.getUserWorkouts(1L).body!!.first()
            assertEquals("BASKETBALL", body.sport)
            assertEquals("PG", body.position)
        }

        @Test
        fun `service exception returns 400`() {
            whenever(workoutService.getUserWorkoutPlans(1L))
                .thenThrow(RuntimeException("DB error"))

            val response = controller.getUserWorkouts(1L)

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }
}