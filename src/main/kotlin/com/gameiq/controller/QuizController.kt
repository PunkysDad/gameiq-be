package com.gameiq.controller

import com.gameiq.service.QuizService
import com.gameiq.entity.* // Import entities which contain the enums
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus

data class QuizRequest(
    val sport: String,
    val position: String? = null,
    val quizType: String,
    val difficultyLevel: String,
    val questionCount: Int = 10
)

data class QuizSubmissionRequest(
    val quizId: String, // Generated quiz identifier
    val userAnswers: List<Int>, // Selected answer indices
    val timeTakenSeconds: Int?
)

data class QuizResultResponse(
    val quizId: Long,
    val sport: String,
    val position: String?,
    val quizType: String,
    val score: Int,
    val totalQuestions: Int,
    val scorePercentage: Double,
    val timeTakenSeconds: Int?,
    val completedAt: String,
    val correctAnswers: List<Int>,
    val explanations: List<String>
)

data class LeaderboardResponse(
    val rank: Int,
    val userId: Long,
    val displayName: String?,
    val sport: String,
    val averageScore: Double,
    val totalQuizzes: Long
)

// Add missing data classes that the controller expects
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

@RestController
@RequestMapping("/quizzes")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:19006"])
class QuizController(
    private val quizService: QuizService
) {
    
    @PostMapping("/generate")
    fun generateQuiz(
        @RequestParam userId: Long,
        @RequestBody request: QuizRequest
    ): ResponseEntity<GeneratedQuiz> {
        return try {
            // Simplified approach - use existing ClaudeService for quiz generation
            val sport = Sport.valueOf(request.sport.uppercase())
            val position = request.position?.let { Position.valueOf(it.uppercase()) }
            val quizType = QuizType.valueOf(request.quizType.uppercase())
            val difficultyLevel = DifficultyLevel.valueOf(request.difficultyLevel.uppercase())
            
            // Create a simple generated quiz (you can enhance this later)
            val quiz = GeneratedQuiz(
                sport = sport,
                position = position,
                quizType = quizType,
                difficultyLevel = difficultyLevel,
                questions = listOf(
                    QuizQuestion(
                        question = "Sample ${sport.name} question for ${position?.name ?: "general"} position",
                        options = listOf("Option A", "Option B", "Option C", "Option D"),
                        correctAnswer = 0,
                        explanation = "This is a sample explanation"
                    )
                ),
                title = "${sport.name} ${quizType.name} Quiz"
            )
            
            ResponseEntity.ok(quiz)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }
    
    @GetMapping("/user/{userId}/results")
    fun getUserQuizResults(
        @PathVariable userId: Long,
        @RequestParam(required = false) sport: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<List<QuizResultResponse>> {
        return try {
            // Simplified - return empty list for now
            // This would use actual QuizService methods when they exist
            ResponseEntity.ok(emptyList<QuizResultResponse>())
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/user/{userId}/stats")
    fun getUserQuizStats(@PathVariable userId: Long): ResponseEntity<QuizStats> {
        return try {
            val stats = QuizStats(
                totalQuizzesCompleted = 0,
                averageScore = 0.0,
                bestScore = 0.0,
                averageTimeSeconds = null,
                currentStreak = 0,
                improvementTrend = 0.0
            )
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/available-options")
    fun getAvailableQuizOptions(): ResponseEntity<Map<String, List<String>>> {
        return ResponseEntity.ok(mapOf(
            "sports" to Sport.values().map { it.name },
            "quizTypes" to QuizType.values().map { it.name },
            "difficultyLevels" to DifficultyLevel.values().map { it.name },
            "positions" to Position.values().map { it.name }
        ))
    }
    
    @PostMapping("/test")
    fun testQuizController(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "message" to "Quiz controller is working",
            "availableSports" to Sport.values().joinToString(", ")
        ))
    }
}