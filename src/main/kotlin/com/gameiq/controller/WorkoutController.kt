// WorkoutController.kt - place in src/main/kotlin/com/gameiq/controller/
package com.gameiq.controller

import com.gameiq.service.WorkoutService
import com.gameiq.service.WorkoutValidationService
import com.gameiq.service.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/workouts")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:19006"])
class WorkoutController(
    private val workoutService: WorkoutService,
    private val workoutValidationService: WorkoutValidationService
) {

    @PostMapping("/generate")
    fun generateWorkout(
        @RequestBody request: GenerateWorkoutRequest,
        @RequestParam userId: Long
    ): ResponseEntity<WorkoutPlanResponse> {
        return try {
            val workoutPlan = workoutService.generateWorkout(userId, request)
            ResponseEntity.ok(workoutPlan)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping
    fun getUserWorkouts(
        @RequestParam userId: Long,
        @RequestParam(name = "saved_only", defaultValue = "false") savedOnly: Boolean
    ): ResponseEntity<List<WorkoutPlanResponse>> {
        return try {
            val workouts = workoutService.getUserWorkouts(userId, savedOnly)
            ResponseEntity.ok(workouts)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{workoutId}")
    fun getWorkout(
        @PathVariable workoutId: Long,
        @RequestParam userId: Long
    ): ResponseEntity<WorkoutPlanResponse> {
        return try {
            val workout = workoutService.getWorkoutById(userId, workoutId)
            ResponseEntity.ok(workout)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/{workoutId}/save")
    fun saveWorkout(
        @PathVariable workoutId: Long,
        @RequestParam userId: Long
    ): ResponseEntity<WorkoutPlanResponse> {
        return try {
            val savedWorkout = workoutService.saveWorkout(userId, workoutId)
            ResponseEntity.ok(savedWorkout)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @DeleteMapping("/{workoutId}/save")
    fun unsaveWorkout(
        @PathVariable workoutId: Long,
        @RequestParam userId: Long
    ): ResponseEntity<WorkoutPlanResponse> {
        return try {
            val unsavedWorkout = workoutService.unsaveWorkout(userId, workoutId)
            ResponseEntity.ok(unsavedWorkout)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @PostMapping("/preferences")
    fun updateWorkoutPreferences(
        @RequestBody request: UserWorkoutPreferencesRequest,
        @RequestParam userId: Long
    ): ResponseEntity<com.gameiq.entity.UserWorkoutPreferences> {
        return try {
            val preferences = workoutService.saveUserWorkoutPreferences(userId, request)
            ResponseEntity.ok(preferences)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/preferences")
    fun getWorkoutPreferences(
        @RequestParam userId: Long
    ): ResponseEntity<com.gameiq.entity.UserWorkoutPreferences?> {
        return try {
            val preferences = workoutService.getUserWorkoutPreferences(userId)
            ResponseEntity.ok(preferences)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    // Helper endpoints for frontend
    @GetMapping("/sports")
    fun getSupportedSports(): ResponseEntity<List<String>> {
        return try {
            val sports = workoutValidationService.getSupportedSports()
            ResponseEntity.ok(sports)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/sports/{sport}/positions")
    fun getPositionsForSport(@PathVariable sport: String): ResponseEntity<List<String>> {
        return try {
            val positions = workoutValidationService.getSupportedPositions(sport)
            ResponseEntity.ok(positions)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/equipment")
    fun getSupportedEquipment(): ResponseEntity<List<String>> {
        return try {
            val equipment = workoutValidationService.getSupportedEquipment()
            ResponseEntity.ok(equipment)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/goals")
    fun getValidGoals(): ResponseEntity<List<String>> {
        return try {
            val goals = workoutValidationService.getValidGoals()
            ResponseEntity.ok(goals)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/test")
    fun testWorkouts(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "message" to "Workout controller is working",
            "availableSports" to com.gameiq.entity.Sport.values().joinToString(", "),
            "availablePositions" to com.gameiq.entity.Position.values().take(5).joinToString(", ") + "..."
        ))
    }
}