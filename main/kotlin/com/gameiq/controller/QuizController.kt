package com.gameiq.controller

import com.gameiq.service.QuizService
import com.gameiq.service.data.*
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

@RestController
@RequestMapping("/api/v1/quizzes")
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
            val quiz = quizService.generateQuiz(
                userId = userId,
                sport = Sport.valueOf(request.sport.uppercase()),
                position = request.position?.let { Position.valueOf(it.uppercase()) },
                quizType = QuizType.valueOf(request.quizType.uppercase()),
                difficultyLevel = DifficultyLevel.valueOf(request.difficultyLevel.uppercase()),
                questionCount = request.questionCount
            )
            ResponseEntity.ok(quiz)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build()
        } catch (e: Exception) {
            ResponseEntity.internalServerError().build()
        }
    }
    
    @PostMapping("/submit")
    fun submitQuiz(
        @RequestParam userId: Long,
        @RequestBody submission: QuizSubmissionRequest
    ): ResponseEntity<QuizResultResponse> {
        return try {
            // Note: You'll need to store the GeneratedQuiz temporarily or 
            // restructure to include quiz data in the submission
            // For now, this is a placeholder structure
            
            ResponseEntity.ok(QuizResultResponse(
                quizId = 0L, // This would be the actual saved quiz result ID
                sport = "FOOTBALL", // From stored quiz data
                position = "QUARTERBACK", // From stored quiz data
                quizType = "FORMATION_RECOGNITION", // From stored quiz data
                score = 8, // Calculated score
                totalQuestions = 10,
                scorePercentage = 80.0,
                timeTakenSeconds = submission.timeTakenSeconds,
                completedAt = java.time.LocalDateTime.now().toString(),
                correctAnswers = listOf(1, 0, 2, 1, 3, 0, 2, 1, 0, 2), // Correct answer indices
                explanations = listOf() // Quiz explanations
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
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
            val results = quizService.getUserQuizResults(
                userId = userId,
                sport = sport?.let { Sport.valueOf(it.uppercase()) },
                page = page,
                size = size
            )
            
            val response = results.map { result ->
                QuizResultResponse(
                    quizId = result.id,
                    sport = result.sport.name,
                    position = result.position?.name,
                    quizType = result.quizType.name,
                    score = result.score,
                    totalQuestions = result.totalQuestions,
                    scorePercentage = result.scorePercentage,
                    timeTakenSeconds = result.timeTakenSeconds,
                    completedAt = result.completedAt.toString(),
                    correctAnswers = listOf(), // Parse from stored data
                    explanations = listOf() // Parse from stored data
                )
            }
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/user/{userId}/stats")
    fun getUserQuizStats(@PathVariable userId: Long): ResponseEntity<QuizStats> {
        return try {
            val stats = quizService.getUserQuizStats(userId)
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/user/{userId}/stats/sport/{sport}")
    fun getSportQuizStats(
        @PathVariable userId: Long,
        @PathVariable sport: String
    ): ResponseEntity<SportQuizStats> {
        return try {
            val sportEnum = Sport.valueOf(sport.uppercase())
            val stats = quizService.getSportQuizStats(userId, sportEnum)
            ResponseEntity.ok(stats)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().build()
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/leaderboard")
    fun getLeaderboard(
        @RequestParam(required = false) sport: String?,
        @RequestParam(defaultValue = "10") limit: Int
    ): ResponseEntity<List<LeaderboardResponse>> {
        return try {
            val sportEnum = sport?.let { Sport.valueOf(it.uppercase()) }
            val leaderboard = quizService.getLeaderboard(sportEnum, limit)
            
            val response = leaderboard.map { entry ->
                LeaderboardResponse(
                    rank = entry.rank,
                    userId = entry.user.id,
                    displayName = entry.user.displayName,
                    sport = entry.sport.name,
                    averageScore = entry.score,
                    totalQuizzes = 0L // You'd calculate this from the service
                )
            }
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @PostMapping("/share")
    fun generateShareContent(
        @RequestParam userId: Long,
        @RequestParam quizResultId: Long
    ): ResponseEntity<ShareableQuizContent> {
        return try {
            val shareContent = quizService.generateShareableContent(userId, quizResultId)
            ResponseEntity.ok(shareContent)
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
}