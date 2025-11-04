package com.gameiq.service

import com.gameiq.entity.*
import com.gameiq.repository.QuizResultRepository
import com.gameiq.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime

@Service
@Transactional
class QuizService(
    private val quizResultRepository: QuizResultRepository,
    private val userRepository: UserRepository,
    private val claudeService: ClaudeService,
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
    
    // Generate new quiz
    fun generateQuiz(
        userId: Long,
        sport: Sport,
        position: Position? = null,
        quizType: QuizType,
        difficultyLevel: DifficultyLevel,
        questionCount: Int = 10
    ): GeneratedQuiz {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        // Check rate limits for free users
        checkQuizRateLimit(user)
        
        // Generate quiz using Claude
        val quizContent = claudeService.generateQuiz(sport, position, quizType, difficultyLevel, questionCount)
        
        // Parse questions
        val questions = parseQuizQuestions(quizContent.questionsJson)
        
        return GeneratedQuiz(
            sport = sport,
            position = position,
            quizType = quizType,
            difficultyLevel = difficultyLevel,
            questions = questions,
            title = generateQuizTitle(sport, position, quizType)
        )
    }
    
    // Submit quiz answers and calculate results
    fun submitQuizAnswers(
        userId: Long,
        generatedQuiz: GeneratedQuiz,
        userAnswers: List<Int>,
        timeTakenSeconds: Int? = null
    ): QuizResult {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        if (userAnswers.size != generatedQuiz.questions.size) {
            throw IllegalArgumentException("Number of answers doesn't match number of questions")
        }
        
        // Calculate score
        val correctAnswers = userAnswers.zip(generatedQuiz.questions).count { (userAnswer, question) ->
            userAnswer == question.correctAnswer
        }
        
        val totalQuestions = generatedQuiz.questions.size
        val scorePercentage = (correctAnswers.toDouble() / totalQuestions) * 100
        
        // Create quiz result
        val quizResult = QuizResult(
            user = user,
            sport = generatedQuiz.sport,
            position = generatedQuiz.position,
            quizType = generatedQuiz.quizType,
            quizTitle = generatedQuiz.title,
            questionsJson = objectMapper.writeValueAsString(generatedQuiz.questions),
            userAnswersJson = objectMapper.writeValueAsString(userAnswers),
            correctAnswers = correctAnswers,
            totalQuestions = totalQuestions,
            scorePercentage = scorePercentage,
            timeTakenSeconds = timeTakenSeconds,
            difficultyLevel = generatedQuiz.difficultyLevel
        )
        
        return quizResultRepository.save(quizResult)
    }
    
    // User quiz history and analytics
    fun getUserQuizHistory(userId: Long, limit: Int = 20): List<QuizResult> {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        return quizResultRepository.findRecentResultsByUser(user).take(limit)
    }
    
    fun getUserQuizStats(userId: Long): QuizStats {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        val totalQuizzes = quizResultRepository.countQuizzesByUser(user)
        val averageScore = quizResultRepository.getAverageScoreByUser(user)
        val averageTime = quizResultRepository.getAverageTimeByUser(user)
        
        // Calculate current IQ streak
        val recentQuizzes = getUserRecentQuizzes(userId, 30)
        val currentStreak = calculateQuizStreak(recentQuizzes)
        
        // Get best score
        val bestScore = getUserBestScore(userId)
        
        // Get improvement trend
        val improvementTrend = calculateImprovementTrend(userId)
        
        return QuizStats(
            totalQuizzesCompleted = totalQuizzes,
            averageScore = averageScore ?: 0.0,
            bestScore = bestScore,
            averageTimeSeconds = averageTime,
            currentStreak = currentStreak,
            improvementTrend = improvementTrend
        )
    }
    
    fun getUserQuizStatsBySport(userId: Long, sport: Sport): SportQuizStats {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        val averageScore = quizResultRepository.getAverageScoreBySport(user, sport)
        val bestScores = quizResultRepository.findBestScoresBySport(user, sport).take(5)
        val quizzesBySport = quizResultRepository.findByUserAndSport(user, sport)
        
        // Calculate performance by quiz type
        val performanceByType = mutableMapOf<QuizType, Double>()
        QuizType.FORMATION_RECOGNITION.let { performanceByType[it] = quizResultRepository.getAverageScoreByQuizType(user, it) ?: 0.0 }
        QuizType.PLAY_CALLING.let { performanceByType[it] = quizResultRepository.getAverageScoreByQuizType(user, it) ?: 0.0 }
        QuizType.TACTICAL_DECISION.let { performanceByType[it] = quizResultRepository.getAverageScoreByQuizType(user, it) ?: 0.0 }
        QuizType.POSITION_KNOWLEDGE.let { performanceByType[it] = quizResultRepository.getAverageScoreByQuizType(user, it) ?: 0.0 }
        QuizType.RULES_AND_REGULATIONS.let { performanceByType[it] = quizResultRepository.getAverageScoreByQuizType(user, it) ?: 0.0 }
        QuizType.GAME_SITUATION.let { performanceByType[it] = quizResultRepository.getAverageScoreByQuizType(user, it) ?: 0.0 }
        
        return SportQuizStats(
            sport = sport,
            averageScore = averageScore ?: 0.0,
            totalQuizzes = quizzesBySport.size.toLong(),
            bestScores = bestScores,
            performanceByQuizType = performanceByType
        )
    }
    
    // Social sharing
    fun shareQuizResult(quizResultId: Long, platform: String): QuizResult {
        val quizResult = quizResultRepository.findById(quizResultId).orElseThrow { 
            IllegalArgumentException("Quiz result not found") 
        }
        
        val updatedResult = when (platform.lowercase()) {
            "facebook" -> quizResult.copy(sharedToFacebook = true)
            "tiktok" -> quizResult.copy(sharedToTiktok = true)
            else -> throw IllegalArgumentException("Unsupported platform: $platform")
        }
        
        return quizResultRepository.save(updatedResult)
    }
    
    fun generateShareableContent(quizResultId: Long): ShareableQuizContent {
        val quizResult = quizResultRepository.findById(quizResultId).orElseThrow { 
            IllegalArgumentException("Quiz result not found") 
        }
        
        val scoreLevel = when {
            quizResult.scorePercentage >= 90 -> "Expert"
            quizResult.scorePercentage >= 80 -> "Advanced"
            quizResult.scorePercentage >= 70 -> "Proficient"
            quizResult.scorePercentage >= 60 -> "Developing"
            else -> "Beginner"
        }
        
        val challengeText = "Can you beat my ${quizResult.sport} IQ score?"
        
        return ShareableQuizContent(
            title = "${quizResult.user.displayName}'s ${quizResult.sport} IQ Result",
            score = "${quizResult.scorePercentage.toInt()}%",
            scoreLevel = scoreLevel,
            sport = quizResult.sport,
            position = quizResult.position,
            challengeText = challengeText,
            quizType = quizResult.quizType
        )
    }
    
    // Leaderboards
    fun getSportLeaderboard(sport: Sport, days: Long = 30): List<LeaderboardEntry> {
        val since = LocalDateTime.now().minusDays(days)
        val results = quizResultRepository.getLeaderboardBySport(sport, since)
        
        return results.mapIndexed { index, result ->
            val user = result[0] as User
            val avgScore = result[1] as Double
            
            LeaderboardEntry(
                rank = index + 1,
                user = user,
                score = avgScore,
                sport = sport
            )
        }
    }
    
    // Rate limiting for free users
    private fun checkQuizRateLimit(user: User) {
        if (user.subscriptionTier == SubscriptionTier.FREE) {
            val weekStart = LocalDateTime.now().minusDays(7)
            val weeklyQuizzes = quizResultRepository.countQuizzesByUserSince(user, weekStart)
            
            if (weeklyQuizzes >= 3) {
                throw IllegalStateException("Weekly quiz limit exceeded. Upgrade to continue taking unlimited quizzes.")
            }
        }
    }
    
    // Helper methods
    private fun parseQuizQuestions(questionsJson: String): List<QuizQuestion> {
        return try {
            @Suppress("UNCHECKED_CAST")
            val questionsArray = objectMapper.readValue(questionsJson, Array::class.java) as Array<Map<String, Any>>
            
            questionsArray.map { questionMap ->
                QuizQuestion(
                    question = questionMap["question"] as String,
                    options = questionMap["options"] as List<String>,
                    correctAnswer = questionMap["correctAnswer"] as Int,
                    explanation = questionMap["explanation"] as String
                )
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse quiz questions: ${e.message}", e)
        }
    }
    
    private fun generateQuizTitle(sport: Sport, position: Position?, quizType: QuizType): String {
        val positionText = position?.let { " - $it" } ?: ""
        val typeText = when (quizType) {
            QuizType.FORMATION_RECOGNITION -> "Formation Recognition"
            QuizType.PLAY_CALLING -> "Play Calling"
            QuizType.TACTICAL_DECISION -> "Tactical Decisions"
            QuizType.POSITION_KNOWLEDGE -> "Position Knowledge"
            QuizType.RULES_AND_REGULATIONS -> "Rules & Regulations"
            QuizType.GAME_SITUATION -> "Game Situations"
        }
        
        return "$sport $typeText$positionText Quiz"
    }
    
    private fun getUserRecentQuizzes(userId: Long, days: Long): List<QuizResult> {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        val since = LocalDateTime.now().minusDays(days)
        
        return quizResultRepository.findUserQuizzesSince(user, since)
    }
    
    private fun calculateQuizStreak(recentQuizzes: List<QuizResult>): Int {
        if (recentQuizzes.isEmpty()) return 0
        
        val quizDates = recentQuizzes
            .map { it.completedAt.toLocalDate() }
            .distinct()
            .sortedDescending()
        
        var streak = 0
        var currentDate = LocalDateTime.now().toLocalDate()
        
        for (quizDate in quizDates) {
            val daysDiff = java.time.Period.between(quizDate, currentDate).days
            
            if (daysDiff <= 1) {
                streak++
                currentDate = quizDate
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun getUserBestScore(userId: Long): Double {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        return quizResultRepository.findByUser(user)
            .maxByOrNull { it.scorePercentage }
            ?.scorePercentage ?: 0.0
    }
    
    private fun calculateImprovementTrend(userId: Long): Double {
        val recentQuizzes = getUserRecentQuizzes(userId, 30)
        if (recentQuizzes.size < 2) return 0.0
        
        val sortedQuizzes = recentQuizzes.sortedBy { it.completedAt }
        val firstHalf = sortedQuizzes.take(sortedQuizzes.size / 2)
        val secondHalf = sortedQuizzes.drop(sortedQuizzes.size / 2)
        
        val firstHalfAvg = firstHalf.map { it.scorePercentage }.average()
        val secondHalfAvg = secondHalf.map { it.scorePercentage }.average()
        
        return secondHalfAvg - firstHalfAvg
    }
}