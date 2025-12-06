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

data class GenerateWorkoutRequest(
    val sport: String,
    val position: String,
    val equipmentAvailable: List<String>,
    val timeAvailable: Int, // in minutes
    val experience: String, // beginner, intermediate, advanced
    val goals: List<String> // e.g., ["arm_strength", "mobility", "power"]
)

data class WorkoutExercise(
    val name: String,
    val sets: Int,
    val reps: String, // "8-12" or "30 seconds" etc.
    val rest: String, // "60 seconds"
    val description: String,
    val positionBenefit: String // Why this helps the specific position
)

data class GeneratedWorkout(
    val workoutName: String,
    val duration: Int,
    val difficultyLevel: String,
    val positionFocus: String,
    val warmUp: List<WorkoutExercise>,
    val mainExercises: List<WorkoutExercise>,
    val coolDown: List<WorkoutExercise>,
    val equipmentUsed: List<String>,
    val notes: String
)

data class WorkoutPlanResponse(
    val id: Long,
    val sport: String,
    val position: String,
    val workoutName: String,
    val durationMinutes: Int,
    val difficultyLevel: String,
    val equipmentNeeded: List<String>,
    val positionFocus: String,
    val workout: GeneratedWorkout,
    val isSaved: Boolean,
    val createdAt: java.time.LocalDateTime
)

data class UserWorkoutPreferencesRequest(
    val availableEquipment: List<String>?,
    val preferredDuration: Int?,
    val experienceLevel: String?,
    val trainingGoals: List<String>?
)