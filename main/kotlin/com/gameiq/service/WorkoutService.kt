package com.gameiq.service

import com.gameiq.entity.*
import com.gameiq.repository.WorkoutPlanRepository
import com.gameiq.repository.WorkoutSessionRepository  
import com.gameiq.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class WorkoutService(
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val userRepository: UserRepository,
    private val claudeService: ClaudeService
) {
    
    // Workout Plan Creation
    fun createWorkoutPlan(
        userId: Long,
        title: String,
        sport: Sport,
        position: Position,
        difficultyLevel: DifficultyLevel,
        trainingPhase: TrainingPhase,
        description: String? = null,
        equipmentAvailable: String? = null
    ): WorkoutPlan {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        // Generate workout plan using Claude
        val workoutContent = claudeService.generateWorkoutPlan(
            sport = sport,
            position = position,
            difficultyLevel = difficultyLevel,
            trainingPhase = trainingPhase,
            equipmentAvailable = equipmentAvailable
        )
        
        val workoutPlan = WorkoutPlan(
            user = user,
            title = title,
            description = description,
            sport = sport,
            position = position,
            difficultyLevel = difficultyLevel,
            trainingPhase = trainingPhase,
            equipmentNeeded = workoutContent.equipmentNeeded,
            exercisesJson = workoutContent.exercisesJson,
            focusAreas = workoutContent.focusAreas,
            estimatedDurationMinutes = workoutContent.estimatedDuration,
            claudePromptUsed = workoutContent.promptUsed
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
    
    // Position-specific recommendations
    fun getRecommendedWorkoutPlans(
        userId: Long,
        sport: Sport,
        position: Position,
        difficultyLevel: DifficultyLevel? = null
    ): List<WorkoutPlan> {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        val allPlans = if (difficultyLevel != null) {
            workoutPlanRepository.findBySportAndPositionAndDifficultyLevel(sport, position, difficultyLevel)
        } else {
            workoutPlanRepository.findBySportAndPosition(sport, position)
        }
        
        // Filter out user's own plans
        return allPlans.filter { plan -> plan.user.id != userId }
    }
    
    fun getPopularWorkoutPlans(sport: Sport? = null): List<WorkoutPlan> {
        val popularPlans = workoutPlanRepository.findMostPopularPlans()
        
        return if (sport != null) {
            popularPlans.filter { plan -> plan.sport == sport }
        } else {
            popularPlans
        }
    }
    
    // Workout Session Management
    fun startWorkoutSession(userId: Long, workoutPlanId: Long): WorkoutSession {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        val workoutPlan = workoutPlanRepository.findById(workoutPlanId).orElseThrow { 
            IllegalArgumentException("Workout plan not found") 
        }
        
        // Check for existing active sessions
        val activeSessions = workoutSessionRepository.findByUserAndStatusIn(
            user, listOf(WorkoutStatus.PLANNED, WorkoutStatus.IN_PROGRESS)
        )
        
        if (activeSessions.isNotEmpty()) {
            throw IllegalStateException("User already has an active workout session")
        }
        
        val session = WorkoutSession(
            user = user,
            workoutPlan = workoutPlan,
            status = WorkoutStatus.IN_PROGRESS,
            startedAt = LocalDateTime.now()
        )
        
        return workoutSessionRepository.save(session)
    }
    
    fun completeWorkoutSession(
        sessionId: Long,
        exercisesCompletedJson: String?,
        notes: String? = null,
        difficultyRating: Int? = null,
        effectivenessRating: Int? = null
    ): WorkoutSession {
        val session = workoutSessionRepository.findById(sessionId).orElseThrow { 
            IllegalArgumentException("Workout session not found") 
        }
        
        val now = LocalDateTime.now()
        val durationMinutes = if (session.startedAt != null) {
            java.time.Duration.between(session.startedAt, now).toMinutes().toInt()
        } else null
        
        val updatedSession = session.copy(
            status = WorkoutStatus.COMPLETED,
            completedAt = now,
            durationMinutes = durationMinutes,
            exercisesCompletedJson = exercisesCompletedJson,
            notes = notes,
            difficultyRating = difficultyRating,
            effectivenessRating = effectivenessRating,
            updatedAt = now
        )
        
        return workoutSessionRepository.save(updatedSession)
    }
    
    fun cancelWorkoutSession(sessionId: Long): WorkoutSession {
        val session = workoutSessionRepository.findById(sessionId).orElseThrow { 
            IllegalArgumentException("Workout session not found") 
        }
        
        val updatedSession = session.copy(
            status = WorkoutStatus.CANCELLED,
            updatedAt = LocalDateTime.now()
        )
        
        return workoutSessionRepository.save(updatedSession)
    }
    
    // User workout history and analytics
    fun getUserWorkoutSessions(userId: Long, limit: Int = 20): List<WorkoutSession> {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        return workoutSessionRepository.findRecentSessionsByUser(user).take(limit)
    }
    
    fun getUserWorkoutStats(userId: Long): WorkoutStats {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        val totalCompleted = workoutSessionRepository.countCompletedSessionsByUser(user)
        val totalMinutes = workoutSessionRepository.getTotalTrainingMinutesByUser(user) ?: 0
        val averageDuration = workoutSessionRepository.getAverageWorkoutDurationByUser(user)
        val averageDifficulty = workoutSessionRepository.getAverageDifficultyRatingByUser(user)
        val averageEffectiveness = workoutSessionRepository.getAverageEffectivenessRatingByUser(user)
        
        // Calculate current streak
        val recentSessions = getUserCompletedWorkoutsInLastDays(userId, 30)
        val currentStreak = calculateWorkoutStreak(recentSessions)
        
        return WorkoutStats(
            totalWorkoutsCompleted = totalCompleted,
            totalTrainingMinutes = totalMinutes,
            averageWorkoutDuration = averageDuration,
            averageDifficultyRating = averageDifficulty,
            averageEffectivenessRating = averageEffectiveness,
            currentStreak = currentStreak
        )
    }
    
    fun getUserCompletedWorkoutsInLastDays(userId: Long, days: Long = 7): List<WorkoutSession> {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        val since = LocalDateTime.now().minusDays(days)
        
        return workoutSessionRepository.findUserCompletedSessionsSince(user, since)
    }
    
    // Training recommendations based on history
    fun getPersonalizedWorkoutRecommendations(userId: Long): List<WorkoutPlan> {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        // Get user's primary sport and position
        val sport = user.primarySport ?: return emptyList()
        val position = user.primaryPosition ?: return emptyList()
        
        // Get user's recent sessions to determine difficulty level
        val recentSessions = getUserCompletedWorkoutsInLastDays(userId, 14)
        val difficultyRatings = recentSessions.mapNotNull { it.difficultyRating?.toDouble() }
        
        val averageDifficulty = if (difficultyRatings.isNotEmpty()) {
            difficultyRatings.average()
        } else {
            2.5 // Default to intermediate
        }
        
        val recommendedDifficulty = when {
            averageDifficulty >= 4.0 -> DifficultyLevel.ADVANCED
            averageDifficulty >= 3.0 -> DifficultyLevel.INTERMEDIATE
            else -> DifficultyLevel.BEGINNER
        }
        
        // Find similar workout plans from other users
        return workoutPlanRepository.findSimilarPlans(sport, position, recommendedDifficulty, user)
            .take(5)
    }
    
    // Helper methods
    private fun calculateWorkoutStreak(completedSessions: List<WorkoutSession>): Int {
        if (completedSessions.isEmpty()) return 0
        
        val sessionDates = completedSessions
            .mapNotNull { it.completedAt?.toLocalDate() }
            .distinct()
            .sortedDescending()
        
        var streak = 0
        var currentDate = LocalDateTime.now().toLocalDate()
        
        for (sessionDate in sessionDates) {
            val daysDiff = java.time.Period.between(sessionDate, currentDate).days
            
            if (daysDiff <= 1) {
                streak++
                currentDate = sessionDate
            } else {
                break
            }
        }
        
        return streak
    }
}