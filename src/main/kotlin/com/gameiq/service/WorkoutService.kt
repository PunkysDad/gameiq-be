package com.gameiq.service

import com.gameiq.entity.*
import com.gameiq.repository.WorkoutPlanRepository
import com.gameiq.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class WorkoutService(
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val userRepository: UserRepository,
    private val claudeService: ClaudeService
) {
    
    // Enhanced workout plan creation with Claude integration
    fun createWorkoutPlan(
        userId: Long,
        title: String,
        sport: Sport,
        position: Position,
        difficultyLevel: String, // String from our entity alignment
        trainingPhase: String,   // String for simplicity
        description: String? = null,
        equipmentAvailable: String? = null
    ): WorkoutPlan {
        // Convert string parameters back to enums for Claude API
        val difficultyLevelEnum = when (difficultyLevel.uppercase()) {
            "BEGINNER" -> DifficultyLevel.BEGINNER
            "INTERMEDIATE" -> DifficultyLevel.INTERMEDIATE
            "ADVANCED" -> DifficultyLevel.ADVANCED
            "ELITE" -> DifficultyLevel.ELITE
            else -> DifficultyLevel.INTERMEDIATE // Default fallback
        }
        
        val trainingPhaseEnum = when (trainingPhase.uppercase()) {
            "OFF_SEASON" -> TrainingPhase.OFF_SEASON
            "PRE_SEASON" -> TrainingPhase.PRE_SEASON
            "IN_SEASON" -> TrainingPhase.IN_SEASON
            "POST_SEASON" -> TrainingPhase.POST_SEASON
            "INJURY_PREVENTION" -> TrainingPhase.INJURY_PREVENTION
            "REHABILITATION" -> TrainingPhase.REHABILITATION
            "GENERAL" -> TrainingPhase.GENERAL
            else -> TrainingPhase.GENERAL // Default fallback
        }
        
        return try {
            // Generate workout using Claude with enhanced prompt
            // ClaudeService now handles user lookup and database saving
            claudeService.generateWorkoutPlan(
                userId = userId, // FIX: Add missing userId parameter
                sport = sport,
                position = position,
                difficultyLevel = difficultyLevelEnum,
                trainingPhase = trainingPhaseEnum,
                equipmentAvailable = equipmentAvailable
            )
            // FIX: No need for manual WorkoutPlan creation - ClaudeService handles it
            
        } catch (e: Exception) {
            // Fallback to basic workout if Claude API fails
            println("Claude API failed, creating fallback workout: ${e.message}")
            
            val user = userRepository.findById(userId).orElseThrow { 
                IllegalArgumentException("User not found") 
            }
            
            val fallbackWorkout = WorkoutPlan(
                user = user,
                sport = sport,
                position = position,
                workoutName = "$title (Basic)",
                positionFocus = description,
                difficultyLevel = difficultyLevel,
                equipmentNeeded = equipmentAvailable,
                generatedContent = createFallbackWorkout(sport, position, difficultyLevel),
                isSaved = false,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
            
            workoutPlanRepository.save(fallbackWorkout)
        }
    }
    
    // Format Claude workout content for storage
    // private fun formatWorkoutContent(workoutContent: WorkoutContent): String {
    //     // Use the enhanced content if available, otherwise fall back to original format
    //     return workoutContent.enhancedContent?.let { enhancedContent ->
    //         buildString {
    //             appendLine("=== GAMEIQ AI-GENERATED WORKOUT ===")
    //             appendLine()
    //             appendLine(enhancedContent)
    //             appendLine()
    //             appendLine("=== TECHNICAL DETAILS ===")
    //             appendLine("Equipment: ${workoutContent.equipmentNeeded}")
    //             appendLine("Focus Areas: ${workoutContent.focusAreas}")
    //             appendLine("Duration: ${workoutContent.estimatedDuration} minutes")
    //             appendLine("Generated with enhanced GameIQ position-specific prompts")
    //         }
    //     } ?: run {
    //         // Fallback to original format if enhanced content not available
    //         buildString {
    //             appendLine("=== CLAUDE-GENERATED WORKOUT ===")
    //             appendLine()
                
    //             workoutContent.equipmentNeeded?.let {
    //                 appendLine("Equipment: $it")
    //                 appendLine()
    //             }
                
    //             workoutContent.focusAreas?.let {
    //                 appendLine("Focus Areas: $it")
    //                 appendLine()
    //             }
                
    //             appendLine("Exercises:")
    //             appendLine(workoutContent.exercisesJson)
    //             appendLine()
                
    //             workoutContent.estimatedDuration?.let {
    //                 appendLine("Estimated Duration: $it minutes")
    //                 appendLine()
    //             }
                
    //             appendLine("--- Technical Info ---")
    //             appendLine("Generated using: ${workoutContent.promptUsed?.take(100)}...")
    //         }
    //     }
    // }
    
    // Fallback workout for when Claude API fails
    private fun createFallbackWorkout(sport: Sport, position: Position, difficultyLevel: String): String {
        return """
        === BASIC ${sport.name} ${position.name} WORKOUT ($difficultyLevel) ===
        
        This is a basic workout template. For AI-powered, position-specific training, 
        please ensure your Claude API is properly configured.
        
        Basic Exercises:
        1. Dynamic Warm-up - 10 minutes
        2. Sport-specific movements - 20 minutes  
        3. Strength training - 15 minutes
        4. Cool-down and stretching - 10 minutes
        
        Total Duration: ~55 minutes
        
        Note: This workout was generated as a fallback when the AI system was unavailable.
        """.trimIndent()
    }
    
    // Get workout plans for user
    fun getUserWorkoutPlans(userId: Long): List<WorkoutPlan> {
        return workoutPlanRepository.findByUserId(userId)
    }
    
    fun getWorkoutPlanById(planId: Long): WorkoutPlan? {
        return workoutPlanRepository.findById(planId).orElse(null)
    }
    
    // Save/unsave workout functionality
    fun saveWorkout(workoutId: Long, userId: Long): WorkoutPlan? {
        val workout = workoutPlanRepository.findById(workoutId).orElse(null)
        
        return if (workout != null && workout.user.id == userId) {
            val savedWorkout = workout.copy(
                isSaved = true,
                updatedAt = LocalDateTime.now()
            )
            workoutPlanRepository.save(savedWorkout)
        } else {
            null
        }
    }
    
    fun deleteWorkout(workoutId: Long, userId: Long) {
        val workout = workoutPlanRepository.findById(workoutId).orElse(null)
        
        if (workout != null && workout.user.id == userId) {
            workoutPlanRepository.delete(workout)
        } else {
            throw IllegalArgumentException("Workout not found or does not belong to user")
        }
    }
    
    // Get user's saved workouts only
    fun getUserSavedWorkouts(userId: Long): List<WorkoutPlan> {
        return workoutPlanRepository.findByUserId(userId).filter { it.isSaved }
    }
}