package com.gameiq.entity

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "workout_plans")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class WorkoutPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val sport: String,

    @Column(nullable = false)
    val position: String,

    @Column(name = "workout_name", nullable = false)
    val workoutName: String,

    @Column(name = "duration_minutes", nullable = false)
    val durationMinutes: Int,

    @Column(name = "difficulty_level", nullable = false)
    val difficultyLevel: String,

    @Column(name = "equipment_needed", columnDefinition = "text[]")
    val equipmentNeeded: Array<String>,

    @Column(name = "position_focus", nullable = false)
    val positionFocus: String,

    @Column(name = "generated_content", nullable = false, columnDefinition = "TEXT")
    val generatedContent: String, // JSON string

    @Column(name = "is_saved", nullable = false)
    val isSaved: Boolean = false,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "user_workout_preferences")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class UserWorkoutPreferences(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false, unique = true)
    val userId: Long,

    @Column(name = "available_equipment", columnDefinition = "text[]")
    val availableEquipment: Array<String>? = null,

    @Column(name = "preferred_duration", nullable = false)
    val preferredDuration: Int = 45,

    @Column(name = "experience_level", nullable = false)
    val experienceLevel: String = "intermediate",

    @Column(name = "training_goals", columnDefinition = "text[]")
    val trainingGoals: Array<String>? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

@Entity
@Table(name = "workout_generation_requests")
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class WorkoutGenerationRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(nullable = false)
    val sport: String,

    @Column(nullable = false)
    val position: String,

    @Column(name = "input_parameters", nullable = false, columnDefinition = "JSON")
    val inputParameters: String,

    @Column(name = "tokens_used", nullable = false)
    val tokensUsed: Int,

    @Column(name = "cost_cents", nullable = false)
    val costCents: Int,

    @Column(name = "generation_time_ms", nullable = false)
    val generationTimeMs: Int,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)