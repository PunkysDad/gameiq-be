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
    
    // Simplified workout plan creation aligned with our entity
    fun createWorkoutPlan(
        userId: Long,
        title: String,
        sport: Sport,
        position: Position,
        difficultyLevel: String, // Changed from enum to String
        trainingPhase: String,   // This will map to our simplified structure
        description: String? = null,
        equipmentAvailable: String? = null
    ): WorkoutPlan {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        // Create workout plan with our simplified structure
        val workoutPlan = WorkoutPlan(
            user = user,
            sport = sport,
            position = position,
            workoutName = title,
            positionFocus = description,
            difficultyLevel = difficultyLevel,
            equipmentNeeded = equipmentAvailable,
            generatedContent = "Generated workout content placeholder", // Will be populated by Claude
            isSaved = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        return workoutPlanRepository.save(workoutPlan)
    }
    
    // Get workout plans for user
    fun getUserWorkoutPlans(userId: Long): List<WorkoutPlan> {
        return workoutPlanRepository.findByUserId(userId)
    }
    
    fun getWorkoutPlanById(planId: Long): WorkoutPlan? {
        return workoutPlanRepository.findById(planId).orElse(null)
    }
    
    // Position-specific recommendations (simplified)
    fun getRecommendedWorkoutPlans(
        userId: Long,
        sport: Sport,
        position: Position,
        difficultyLevel: String? = null
    ): List<WorkoutPlan> {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        // For now, return empty list - we'll implement this later when we have more data
        return emptyList()
    }
    
    fun getPopularWorkoutPlans(sport: Sport? = null): List<WorkoutPlan> {
        // For now, return empty list - we'll implement this later
        return emptyList()
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