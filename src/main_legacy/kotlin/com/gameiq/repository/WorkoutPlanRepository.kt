package com.gameiq.repository

import com.gameiq.entity.WorkoutPlan
import com.gameiq.entity.User
import com.gameiq.entity.Sport
import com.gameiq.entity.Position
import com.gameiq.entity.DifficultyLevel
import com.gameiq.entity.TrainingPhase
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface WorkoutPlanRepository : JpaRepository<WorkoutPlan, Long> {
    
    // User-specific queries
    fun findByUser(user: User): List<WorkoutPlan>
    fun findByUserId(userId: Long): List<WorkoutPlan>
    
    // Sport and position-specific queries
    fun findBySport(sport: Sport): List<WorkoutPlan>
    fun findByPosition(position: Position): List<WorkoutPlan>
    fun findBySportAndPosition(sport: Sport, position: Position): List<WorkoutPlan>
    
    // User's position-specific plans
    fun findByUserAndSportAndPosition(user: User, sport: Sport, position: Position): List<WorkoutPlan>
    
    // Difficulty and phase queries
    fun findByDifficultyLevel(difficultyLevel: DifficultyLevel): List<WorkoutPlan>
    fun findByTrainingPhase(trainingPhase: TrainingPhase): List<WorkoutPlan>
    fun findBySportAndDifficultyLevel(sport: Sport, difficultyLevel: DifficultyLevel): List<WorkoutPlan>
    
    // Combined filters for recommendation engine
    fun findBySportAndPositionAndDifficultyLevel(
        sport: Sport, 
        position: Position, 
        difficultyLevel: DifficultyLevel
    ): List<WorkoutPlan>
    
    fun findBySportAndPositionAndTrainingPhase(
        sport: Sport, 
        position: Position, 
        trainingPhase: TrainingPhase
    ): List<WorkoutPlan>
    
    // Claude-generated plans
    fun findByGeneratedByClaudeTrue(): List<WorkoutPlan>
    fun findByGeneratedByClaudeFalse(): List<WorkoutPlan>
    
    // Duration-based queries
    fun findByEstimatedDurationMinutesBetween(minDuration: Int, maxDuration: Int): List<WorkoutPlan>
    
    // Recent plans
    fun findByCreatedAtAfter(createdAt: LocalDateTime): List<WorkoutPlan>
    fun findByUserAndCreatedAtAfter(user: User, createdAt: LocalDateTime): List<WorkoutPlan>
    
    // Popular plans (most used)
    @Query("""
        SELECT wp FROM WorkoutPlan wp 
        LEFT JOIN WorkoutSession ws ON wp.id = ws.workoutPlan.id 
        GROUP BY wp.id 
        ORDER BY COUNT(ws.id) DESC
    """)
    fun findMostPopularPlans(): List<WorkoutPlan>
    
    // User's recent plans
    @Query("""
        SELECT wp FROM WorkoutPlan wp 
        WHERE wp.user = :user 
        ORDER BY wp.createdAt DESC
    """)
    fun findRecentPlansByUser(@Param("user") user: User): List<WorkoutPlan>
    
    // Find similar plans for recommendations
    @Query("""
        SELECT wp FROM WorkoutPlan wp 
        WHERE wp.sport = :sport 
        AND wp.position = :position 
        AND wp.difficultyLevel = :difficulty
        AND wp.user != :user
        ORDER BY wp.createdAt DESC
    """)
    fun findSimilarPlans(
        @Param("sport") sport: Sport,
        @Param("position") position: Position, 
        @Param("difficulty") difficulty: DifficultyLevel,
        @Param("user") user: User
    ): List<WorkoutPlan>
    
    // Analytics queries
    @Query("SELECT COUNT(wp) FROM WorkoutPlan wp WHERE wp.createdAt >= :since")
    fun countPlansCreatedSince(@Param("since") since: LocalDateTime): Long
    
    @Query("SELECT wp.sport, COUNT(wp) FROM WorkoutPlan wp GROUP BY wp.sport")
    fun getWorkoutPlanCountBySport(): List<Array<Any>>
}