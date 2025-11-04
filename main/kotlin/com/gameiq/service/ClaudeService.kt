package com.gameiq.service

import com.gameiq.entity.*
import com.gameiq.repository.ClaudeConversationRepository
import com.gameiq.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDateTime
import java.util.*

@Service
@Transactional
class ClaudeService(
    private val claudeConversationRepository: ClaudeConversationRepository,
    private val userRepository: UserRepository,
    private val restTemplate: RestTemplate = RestTemplate(),
    private val objectMapper: ObjectMapper = jacksonObjectMapper()
) {
    
    @Value("\${claude.api.key}")
    private lateinit var claudeApiKey: String
    
    @Value("\${claude.api.url:https://api.anthropic.com/v1/messages}")
    private lateinit var claudeApiUrl: String
    
    private val claudeModel = "claude-sonnet-4-20250514"
    
    // Main chat interface
    fun chatWithClaude(
        userId: Long,
        message: String,
        sessionId: String? = null,
        sport: Sport? = null,
        position: Position? = null,
        conversationType: ConversationType = ConversationType.GENERAL_SPORTS_QUESTION
    ): ClaudeConversation {
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        // Check rate limits for free users
        checkRateLimit(user)
        
        val actualSessionId = sessionId ?: UUID.randomUUID().toString()
        
        // Get conversation context if this is part of an ongoing session
        val conversationHistory = if (sessionId != null) {
            claudeConversationRepository.findConversationsBySessionOrdered(sessionId)
        } else {
            emptyList()
        }
        
        // Build system prompt based on sport and position
        val systemPrompt = buildSystemPrompt(sport, position, conversationType)
        
        // Make API call to Claude
        val startTime = System.currentTimeMillis()
        val claudeResponse = callClaudeApi(message, systemPrompt, conversationHistory)
        val responseTime = System.currentTimeMillis() - startTime
        
        // Calculate API cost (approximate)
        val apiCostCents = calculateApiCost(claudeResponse.tokensUsed.input, claudeResponse.tokensUsed.output)
        
        // Save conversation
        val conversation = ClaudeConversation(
            user = user,
            sessionId = actualSessionId,
            userMessage = message,
            claudeResponse = claudeResponse.content,
            sport = sport,
            position = position,
            conversationType = conversationType,
            systemPromptUsed = systemPrompt,
            claudeModel = claudeModel,
            tokensUsedInput = claudeResponse.tokensUsed.input,
            tokensUsedOutput = claudeResponse.tokensUsed.output,
            apiCostCents = apiCostCents,
            responseTimeMs = responseTime
        )
        
        return claudeConversationRepository.save(conversation)
    }
    
    // Position-specific workout plan generation
    fun generateWorkoutPlan(
        sport: Sport,
        position: Position,
        difficultyLevel: DifficultyLevel,
        trainingPhase: TrainingPhase,
        equipmentAvailable: String? = null
    ): WorkoutContent {
        val systemPrompt = buildWorkoutPlanPrompt(sport, position, difficultyLevel, trainingPhase)
        val userPrompt = buildWorkoutPlanUserPrompt(equipmentAvailable)
        
        val claudeResponse = callClaudeApi(userPrompt, systemPrompt)
        
        // Parse the structured response
        return parseWorkoutPlanResponse(claudeResponse.content, systemPrompt)
    }
    
    // Quiz generation
    fun generateQuiz(
        sport: Sport,
        position: Position? = null,
        quizType: QuizType,
        difficultyLevel: DifficultyLevel,
        questionCount: Int = 10
    ): QuizContent {
        val systemPrompt = buildQuizGenerationPrompt(sport, position, quizType, difficultyLevel)
        val userPrompt = "Generate $questionCount quiz questions."
        
        val claudeResponse = callClaudeApi(userPrompt, systemPrompt)
        
        return parseQuizResponse(claudeResponse.content)
    }
    
    // Rate limiting for free users
    private fun checkRateLimit(user: User) {
        if (user.subscriptionTier == SubscriptionTier.FREE) {
            val weekStart = LocalDateTime.now().minusDays(7)
            val weeklyConversations = claudeConversationRepository.countConversationsByUserSince(user, weekStart)
            
            if (weeklyConversations >= 5) {
                throw IllegalStateException("Weekly conversation limit exceeded. Upgrade to continue using AI coaching.")
            }
        }
    }
    
    // System prompt builders
    private fun buildSystemPrompt(
        sport: Sport?, 
        position: Position?, 
        conversationType: ConversationType
    ): String {
        val basePrompt = """
        You are an expert sports coach and trainer specializing in position-specific athletic development. 
        Your responses should be practical, actionable, and focused on helping athletes improve their performance.
        
        IMPORTANT GUIDELINES:
        - Stay focused on sports, training, and athletic development topics only
        - Provide position-specific advice when possible
        - Include injury prevention considerations
        - Be encouraging and motivational
        - If asked about non-sports topics, politely redirect to athletic training
        - Keep responses concise but comprehensive (200-400 words)
        """.trimIndent()
        
        val sportSpecific = sport?.let { 
            "You are specifically focused on $sport training and development." 
        } ?: ""
        
        val positionSpecific = position?.let { 
            "The athlete you're coaching plays the $position position, so tailor your advice accordingly." 
        } ?: ""
        
        val conversationSpecific = when (conversationType) {
            ConversationType.TRAINING_ADVICE -> "Focus on training methodologies and workout recommendations."
            ConversationType.POSITION_SPECIFIC_GUIDANCE -> "Provide position-specific skills and techniques."
            ConversationType.INJURY_PREVENTION -> "Emphasize injury prevention and recovery strategies."
            ConversationType.SKILL_DEVELOPMENT -> "Focus on skill development and technique improvement."
            ConversationType.GAME_STRATEGY -> "Provide tactical and strategic insights."
            ConversationType.WORKOUT_CUSTOMIZATION -> "Help customize workouts for specific needs."
            ConversationType.GENERAL_SPORTS_QUESTION -> "Answer general sports and training questions."
        }
        
        return "$basePrompt\n\n$sportSpecific\n$positionSpecific\n$conversationSpecific"
    }
    
    private fun buildWorkoutPlanPrompt(
        sport: Sport, 
        position: Position, 
        difficultyLevel: DifficultyLevel, 
        trainingPhase: TrainingPhase
    ): String {
        return """
        You are an expert strength and conditioning coach specializing in $sport training for $position players.
        
        Create a detailed workout plan with these specifications:
        - Sport: $sport
        - Position: $position  
        - Difficulty: $difficultyLevel
        - Training Phase: $trainingPhase
        
        Return your response as a JSON object with this exact structure:
        {
          "exercises": [
            {
              "name": "Exercise name",
              "sets": 3,
              "reps": "8-12",
              "restSeconds": 60,
              "description": "How to perform the exercise",
              "positionBenefit": "How this helps the specific position"
            }
          ],
          "equipmentNeeded": "List of required equipment",
          "focusAreas": "Primary muscle groups and skills targeted",
          "estimatedDuration": 45,
          "warmup": "Warm-up routine description",
          "cooldown": "Cool-down routine description"
        }
        
        Focus on exercises that are specifically beneficial for $position players in $sport.
        """.trimIndent()
    }
    
    private fun buildQuizGenerationPrompt(
        sport: Sport, 
        position: Position?, 
        quizType: QuizType, 
        difficultyLevel: DifficultyLevel
    ): String {
        val positionText = position?.let { " for $position players" } ?: ""
        
        return """
        You are a $sport expert creating educational quiz questions$positionText.
        
        Create quiz questions of type: $quizType
        Difficulty level: $difficultyLevel
        
        Return your response as a JSON object with this structure:
        {
          "questions": [
            {
              "question": "The question text",
              "options": ["Option A", "Option B", "Option C", "Option D"],
              "correctAnswer": 0,
              "explanation": "Why this answer is correct and educational context"
            }
          ]
        }
        
        Make questions challenging but fair for the $difficultyLevel level.
        Focus on practical game situations and real tactical knowledge.
        """.trimIndent()
    }
    
    private fun buildWorkoutPlanUserPrompt(equipmentAvailable: String?): String {
        val equipmentText = equipmentAvailable?.let { 
            "Available equipment: $it" 
        } ?: "Assume basic gym equipment is available (dumbbells, barbells, etc.)"
        
        return "Create a comprehensive workout plan. $equipmentText"
    }
    
    // API interaction
    private fun callClaudeApi(
        message: String, 
        systemPrompt: String, 
        conversationHistory: List<ClaudeConversation> = emptyList()
    ): ClaudeApiResponse {
        val headers = HttpHeaders().apply {
            set("x-api-key", claudeApiKey)
            set("anthropic-version", "2023-06-01")
            contentType = MediaType.APPLICATION_JSON
        }
        
        val messages = mutableListOf<Map<String, String>>()
        
        // Add conversation history
        conversationHistory.forEach { conversation ->
            messages.add(mapOf("role" to "user", "content" to conversation.userMessage))
            messages.add(mapOf("role" to "assistant", "content" to conversation.claudeResponse))
        }
        
        // Add current message
        messages.add(mapOf("role" to "user", "content" to message))
        
        val requestBody = mapOf(
            "model" to claudeModel,
            "max_tokens" to 1000,
            "system" to systemPrompt,
            "messages" to messages
        )
        
        val entity = HttpEntity(requestBody, headers)
        
        try {
            val response = restTemplate.exchange(
                claudeApiUrl,
                HttpMethod.POST,
                entity,
                Map::class.java
            )
            
            val responseBody = response.body as Map<String, Any>
            val content = (responseBody["content"] as List<Map<String, Any>>)[0]["text"] as String
            val usage = responseBody["usage"] as Map<String, Any>
            
            return ClaudeApiResponse(
                content = content,
                tokensUsed = TokenUsage(
                    input = usage["input_tokens"] as Int,
                    output = usage["output_tokens"] as Int
                )
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to call Claude API: ${e.message}", e)
        }
    }
    
    // Response parsers
    private fun parseWorkoutPlanResponse(content: String, promptUsed: String): WorkoutContent {
        return try {
            val jsonResponse = objectMapper.readValue(content, Map::class.java) as Map<String, Any>
            
            WorkoutContent(
                exercisesJson = objectMapper.writeValueAsString(jsonResponse["exercises"]),
                equipmentNeeded = jsonResponse["equipmentNeeded"] as String?,
                focusAreas = jsonResponse["focusAreas"] as String?,
                estimatedDuration = (jsonResponse["estimatedDuration"] as Number?)?.toInt(),
                promptUsed = promptUsed
            )
        } catch (e: Exception) {
            // Fallback if JSON parsing fails
            WorkoutContent(
                exercisesJson = """[{"name": "Custom workout", "description": "$content"}]""",
                equipmentNeeded = "Basic gym equipment",
                focusAreas = "General fitness",
                estimatedDuration = 45,
                promptUsed = promptUsed
            )
        }
    }
    
    private fun parseQuizResponse(content: String): QuizContent {
        return try {
            val jsonResponse = objectMapper.readValue(content, Map::class.java) as Map<String, Any>
            val questions = jsonResponse["questions"] as List<Map<String, Any>>
            
            QuizContent(
                questionsJson = objectMapper.writeValueAsString(questions)
            )
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse quiz response: ${e.message}", e)
        }
    }
    
    // Cost calculation
    private fun calculateApiCost(inputTokens: Int, outputTokens: Int): Int {
        // Claude Sonnet 4 pricing: $3 per M input tokens, $15 per M output tokens
        val inputCostCents = (inputTokens * 0.0003).toInt() // $3/1M = $0.000003 per token
        val outputCostCents = (outputTokens * 0.0015).toInt() // $15/1M = $0.000015 per token
        
        return inputCostCents + outputCostCents
    }

    suspend fun getCoachingAnalysis(prompt: String): ClaudeCoachingResponse {
        val request = ClaudeRequest(
            model = "claude-sonnet-4-20250514",
            maxTokens = 1500,
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = prompt
                )
            )
        )
        
        val response = claudeClient.sendRequest(request)
        return parseCoachingResponse(response.content)
    }

    private fun parseCoachingResponse(content: String): ClaudeCoachingResponse {
        // Parse the structured JSON response from Claude
        val jsonResponse = objectMapper.readValue(content, JsonNode::class.java)
        
        return ClaudeCoachingResponse(
            recommendation = jsonResponse["recommendation"].asText(),
            successProbability = jsonResponse["successProbability"].asText(),
            reasoning = jsonResponse["reasoning"].toList(),
            alternatives = jsonResponse["alternatives"].toList(),
            historicalContext = jsonResponse["historicalContext"].asText()
        )
    }
}