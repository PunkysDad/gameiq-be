package com.gameiq.controller

import com.gameiq.entity.*
import com.gameiq.service.WorkoutService
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
        @RequestBody request: GenerateWorkoutRequest,
        @RequestParam userId: Long
    ): ResponseEntity<WorkoutPlan?> {
        try {
            // Convert string parameters to enum types
            val sport = Sport.valueOf(request.sport.uppercase())
            val position = Position.valueOf(request.position.uppercase())
            val difficultyEnum = DifficultyLevel.valueOf(request.experienceLevel.uppercase())
            val trainingPhaseEnum = TrainingPhase.valueOf(request.trainingPhase?.uppercase() ?: "GENERAL")
            val difficultyLevel = difficultyEnum.name
            val trainingPhase = trainingPhaseEnum.name
            
            // Convert equipment list to string (comma-separated)
            val equipmentString = request.equipmentAvailable.joinToString(", ")
            
            // Generate a title based on request
            val title = "${sport.name.lowercase().capitalize()} ${position.name.lowercase().capitalize()} Workout"
            
            val workoutPlan = workoutService.createWorkoutPlan(
                userId = userId,
                title = title,
                sport = sport,
                position = position,
                difficultyLevel = difficultyLevel,
                trainingPhase = trainingPhase,
                description = request.specificGoals,
                equipmentAvailable = equipmentString
            )
            
            return ResponseEntity.ok(workoutPlan)
            
        } catch (e: IllegalArgumentException) {
            // Handle enum conversion errors
            throw IllegalArgumentException("Invalid sport, position, or experience level: ${e.message}")
        }
    }
    
    @GetMapping("/{workoutId}")
    fun getWorkoutById(@PathVariable workoutId: Long): ResponseEntity<WorkoutPlan?> {
        val workout = workoutService.getWorkoutPlanById(workoutId)
        return ResponseEntity.ok(workout)
    }
}

// Updated data class to match what the service expects
data class GenerateWorkoutRequest(
    val sport: String,                    // Will convert to Sport enum
    val position: String,                 // Will convert to Position enum
    val equipmentAvailable: List<String>, // Will convert to comma-separated String
    val experienceLevel: String,          // Will convert to DifficultyLevel enum
    val trainingPhase: String? = "GENERAL", // Will convert to TrainingPhase enum
    val specificGoals: String? = null     // Maps to description parameter
)