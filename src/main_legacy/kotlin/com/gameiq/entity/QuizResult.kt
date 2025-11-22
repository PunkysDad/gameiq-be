package com.gameiq.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "quiz_results")
data class QuizResult(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sport", nullable = false)
    val sport: Sport,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "position")
    val position: Position? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type", nullable = false)
    val quizType: QuizType,
    
    @Column(name = "quiz_title", nullable = false)
    val quizTitle: String,
    
    @Column(name = "questions_json", columnDefinition = "TEXT", nullable = false)
    val questionsJson: String, // JSON array of questions and answers
    
    @Column(name = "user_answers_json", columnDefinition = "TEXT", nullable = false)
    val userAnswersJson: String, // JSON array of user's selected answers
    
    @Column(name = "correct_answers", nullable = false)
    val correctAnswers: Int,
    
    @Column(name = "total_questions", nullable = false)
    val totalQuestions: Int,
    
    @Column(name = "score_percentage", nullable = false)
    val scorePercentage: Double, // calculated field: (correctAnswers/totalQuestions) * 100
    
    @Column(name = "time_taken_seconds")
    val timeTakenSeconds: Int? = null,
    
    @Column(name = "difficulty_level", nullable = false)
    @Enumerated(EnumType.STRING)
    val difficultyLevel: DifficultyLevel,
    
    @Column(name = "generated_by_claude", nullable = false)
    val generatedByClaude: Boolean = true,
    
    @Column(name = "shared_to_facebook", nullable = false)
    val sharedToFacebook: Boolean = false,
    
    @Column(name = "shared_to_tiktok", nullable = false)
    val sharedToTiktok: Boolean = false,
    
    @Column(name = "completed_at", nullable = false)
    val completedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)

enum class QuizType {
    FORMATION_RECOGNITION,
    PLAY_CALLING,
    TACTICAL_DECISION,
    POSITION_KNOWLEDGE,
    RULES_AND_REGULATIONS,
    GAME_SITUATION
}