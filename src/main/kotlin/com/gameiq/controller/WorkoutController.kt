package com.gameiq.controller

import com.gameiq.entity.*
import com.gameiq.service.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/workouts")
class WorkoutController @Autowired constructor(
    private val workoutService: WorkoutService
) {
    
    @GetMapping("/test")
    fun testWorkouts(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "message" to "Workout controller is working",
            "availableSports" to listOf("FOOTBALL", "BASKETBALL", "BASEBALL", "SOCCER"),
            "availablePositions" to listOf("QB", "PG", "PITCHER", "GOALKEEPER")
        ))
    }
    
    @GetMapping
    fun getUserWorkouts(@RequestParam userId: Long): ResponseEntity<List<WorkoutPlan>> {
        val workouts = workoutService.getUserWorkoutPlans(userId)
        return ResponseEntity.ok(workouts)
    }
    
    @GetMapping("/sports")
    fun getSupportedSports(): ResponseEntity<List<String>> {
        val sports = listOf("football", "basketball", "baseball", "soccer")
        return ResponseEntity.ok(sports)
    }
    
    @PostMapping("/generate")
    fun generateWorkout(
        @RequestBody request: WorkoutGenerationRequest,  // Changed from GenerateWorkoutRequest
        @RequestParam(defaultValue = "1") userId: Long
    ): ResponseEntity<WorkoutGenerationResponse> {     // Changed return type
        try {
            // Map your frontend request to the service parameters
            val sport = Sport.valueOf(request.sport.uppercase())
            val position = when (request.position.uppercase().replace(" ", "")) {
                "WIDERECEIVER" -> Position.WR
                "QUARTERBACK" -> Position.QB
                "RUNNINGBACK" -> Position.RB
                "OFFENSIVELINE" -> Position.OL
                "TIGHTEND" -> Position.TE
                "LINEBACKER" -> Position.LB
                "DEFENSIVEBACK" -> Position.DB
                "DEFENSIVELINE" -> Position.DL
                "POINTGUARD" -> Position.PG
                "SHOOTINGGUARD" -> Position.SG
                "SMALLFORWARD" -> Position.SF
                "POWERFORWARD" -> Position.PF
                "CENTER" -> Position.CENTER
                "PITCHER" -> Position.PITCHER
                "CATCHER" -> Position.CATCHER
                "INFIELD" -> Position.INFIELD
                "OUTFIELD" -> Position.OUTFIELD
                "GOALKEEPER" -> Position.GOALKEEPER
                "DEFENDER" -> Position.DEFENDER
                "MIDFIELDER" -> Position.MIDFIELDER
                "FORWARD" -> Position.FORWARD
                "WINGER" -> Position.WINGER
                "DEFENSEMAN" -> Position.DEFENSEMAN
                "GOALIE" -> Position.GOALIE
                else -> throw IllegalArgumentException("Unknown position: ${request.position}")
            }
            val difficultyEnum = when (request.experienceLevel.lowercase()) {
                "beginner" -> DifficultyLevel.BEGINNER
                "intermediate" -> DifficultyLevel.INTERMEDIATE
                "advanced" -> DifficultyLevel.ADVANCED
                else -> DifficultyLevel.INTERMEDIATE
            }
            val trainingPhaseEnum = when (request.trainingPhase.lowercase()) {
                "off-season" -> TrainingPhase.OFF_SEASON
                "pre-season" -> TrainingPhase.PRE_SEASON
                "in-season" -> TrainingPhase.IN_SEASON
                "post-season" -> TrainingPhase.POST_SEASON
                else -> TrainingPhase.OFF_SEASON
            }
            
            val workoutPlan = workoutService.createWorkoutPlan(
                userId = userId,
                title = "${request.position} ${request.trainingPhase} Workout",
                sport = sport,
                position = position,
                difficultyLevel = difficultyEnum.name,
                trainingPhase = trainingPhaseEnum.name,
                description = "AI-generated workout focusing on: ${request.focusAreas.joinToString(", ")}",
                equipmentAvailable = request.availableEquipment.joinToString(", ")
            )
            
            // Convert to the response format your frontend expects
            val workoutPlanDTO = WorkoutPlanDTO(
                id = workoutPlan.id.toString(),
                title = workoutPlan.workoutName ?: "${request.position} ${request.trainingPhase} Workout",
                description = workoutPlan.positionFocus ?: "",
                estimatedDuration = workoutPlan.durationMinutes ?: request.sessionDuration,
                exercises = emptyList(), // You'll implement exercise parsing later
                focusAreas = request.focusAreas,
                createdAt = workoutPlan.createdAt.toString(),
                sport = request.sport,
                position = request.position
            )
            
            return ResponseEntity.ok(WorkoutGenerationResponse(
                success = true,
                data = workoutPlanDTO,
                cost = 0.07
            ))
            
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(WorkoutGenerationResponse(
                success = false,
                error = e.message ?: "Failed to generate workout"
            ))
        }
    }
    
    @GetMapping("/{workoutId}")
    fun getWorkoutById(@PathVariable workoutId: Long): ResponseEntity<WorkoutPlan?> {
        val workout = workoutService.getWorkoutPlanById(workoutId)
        return ResponseEntity.ok(workout)
    }
}