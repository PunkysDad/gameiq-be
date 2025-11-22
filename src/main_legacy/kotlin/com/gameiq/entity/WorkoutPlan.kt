package com.gameiq.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "workout_plans")
data class WorkoutPlan(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Column(name = "title", nullable = false)
    val title: String,
    
    @Column(name = "description", columnDefinition = "TEXT")
    val description: String? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sport", nullable = false)
    val sport: Sport,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    val position: Position,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", nullable = false)
    val difficultyLevel: DifficultyLevel,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "training_phase", nullable = false)
    val trainingPhase: TrainingPhase,
    
    @Column(name = "estimated_duration_minutes")
    val estimatedDurationMinutes: Int? = null,
    
    @Column(name = "equipment_needed", columnDefinition = "TEXT")
    val equipmentNeeded: String? = null,
    
    @Column(name = "exercises_json", columnDefinition = "TEXT", nullable = false)
    val exercisesJson: String, // JSON string of exercise details
    
    @Column(name = "focus_areas", columnDefinition = "TEXT")
    val focusAreas: String? = null, // Comma-separated focus areas
    
    @Column(name = "generated_by_claude", nullable = false)
    val generatedByClaude: Boolean = true,
    
    @Column(name = "claude_prompt_used", columnDefinition = "TEXT")
    val claudePromptUsed: String? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class DifficultyLevel {
    BEGINNER, INTERMEDIATE, ADVANCED, ELITE
}

enum class TrainingPhase {
    OFF_SEASON, PRE_SEASON, IN_SEASON, POST_SEASON, INJURY_PREVENTION, REHABILITATION
}