package com.gameiq.service

import com.gameiq.entity.*

// Data classes for Claude API responses
data class ClaudeApiResponse(
    val content: String,
    val tokensUsed: TokenUsage
)

data class TokenUsage(
    val input: Int,
    val output: Int
)

// Data classes for service responses
data class WorkoutContent(
    val exercisesJson: String,
    val equipmentNeeded: String?,
    val focusAreas: String?,
    val estimatedDuration: Int?,
    val promptUsed: String?
)

data class QuizContent(
    val questionsJson: String
)

data class WorkoutStats(
    val totalWorkoutsCompleted: Long,
    val totalTrainingMinutes: Long,
    val averageWorkoutDuration: Double?,
    val averageDifficultyRating: Double?,
    val averageEffectivenessRating: Double?,
    val currentStreak: Int
)

data class GeneratedQuiz(
    val sport: Sport,
    val position: Position?,
    val quizType: QuizType,
    val difficultyLevel: DifficultyLevel,
    val questions: List<QuizQuestion>,
    val title: String
)

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int,
    val explanation: String
)

data class QuizStats(
    val totalQuizzesCompleted: Long,
    val averageScore: Double,
    val bestScore: Double,
    val averageTimeSeconds: Double?,
    val currentStreak: Int,
    val improvementTrend: Double
)

data class SportQuizStats(
    val sport: Sport,
    val averageScore: Double,
    val totalQuizzes: Long,
    val bestScores: List<QuizResult>,
    val performanceByQuizType: Map<QuizType, Double>
)

data class ShareableQuizContent(
    val title: String,
    val score: String,
    val scoreLevel: String,
    val sport: Sport,
    val position: Position?,
    val challengeText: String,
    val quizType: QuizType
)

data class LeaderboardEntry(
    val rank: Int,
    val user: User,
    val score: Double,
    val sport: Sport
)