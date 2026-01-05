package com.gameiq.service

import com.gameiq.entity.*
import com.gameiq.repository.WorkoutPlanRepository
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
    private val workoutPlanRepository: WorkoutPlanRepository,
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
        println("DEBUG: Starting chatWithClaude for userId: $userId")
        
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        println("DEBUG: User found: ${user.email}")
        
        // Check rate limits for free users
        checkRateLimit(user)
        println("DEBUG: Rate limit check passed")
        
        val actualSessionId = sessionId ?: UUID.randomUUID().toString()
        println("DEBUG: Session ID: $actualSessionId")
        
        // Get conversation context if this is part of an ongoing session
        val conversationHistory = if (sessionId != null) {
            claudeConversationRepository.findConversationsBySessionOrdered(sessionId)
        } else {
            emptyList()
        }
        println("DEBUG: Conversation history size: ${conversationHistory.size}")
        
        // Build system prompt based on sport and position
        val systemPrompt = buildSystemPrompt(sport, position, conversationType)
        println("DEBUG: System prompt built, length: ${systemPrompt.length}")
        
        // Make API call to Claude
        println("DEBUG: About to call Claude API")
        val startTime = System.currentTimeMillis()
        val claudeResponse = callClaudeApi(message, systemPrompt, conversationHistory)
        println("DEBUG: Claude API call completed")
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
        
        println("DEBUG: About to save conversation")
        return claudeConversationRepository.save(conversation)
    }
    
    // Position-specific workout plan generation
    fun generateWorkoutPlan(
        userId: Long,
        sport: String,
        position: String,
        experienceLevel: String,
        trainingPhase: String,
        availableEquipment: String,
        sessionDuration: Int,
        focusAreas: String,
        specialRequirements: String? = null
    ): WorkoutPlan {
        // Get user entity
        val user = userRepository.findById(userId).orElseThrow { 
            IllegalArgumentException("User not found") 
        }
        
        // Convert string parameters to enums
        val sportEnum = Sport.valueOf(sport.uppercase())
        val positionEnum = when (position.uppercase().replace(" ", "")) {
            "WIDERECEIVER" -> Position.WR
            "QUARTERBACK" -> Position.QB
            "RUNNINGBACK" -> Position.RB
            "OFFENSIVELINE" -> Position.OL
            "TIGHTEND" -> Position.TE
            "LINEBACKER" -> Position.LB
            "DEFENSIVEBACK" -> Position.DB
            "DEFENSIVELINE" -> Position.DL
            "POINTGUARD" -> Position.PG
            "SHOOTINGGUARD" -> Position.SG
            "SMALLFORWARD" -> Position.SF
            "POWERFORWARD" -> Position.PF
            "CENTER" -> Position.CENTER
            "PITCHER" -> Position.PITCHER
            "CATCHER" -> Position.CATCHER
            "INFIELD" -> Position.INFIELD
            "OUTFIELD" -> Position.OUTFIELD
            "GOALKEEPER" -> Position.GOALKEEPER
            "DEFENDER" -> Position.DEFENDER
            "MIDFIELDER" -> Position.MIDFIELDER
            "FORWARD" -> Position.FORWARD
            "WINGER" -> Position.WINGER
            "DEFENSEMAN" -> Position.DEFENSEMAN
            "GOALIE" -> Position.GOALIE
            else -> throw IllegalArgumentException("Unknown position: $position")
        }
        
        // Create position-specific system prompt
        val systemPrompt = createWorkoutSystemPrompt(sportEnum, positionEnum, experienceLevel, trainingPhase)
        
        // Create user message - NO JSON requirement, just natural workout request
        val userMessage = """
            Generate a comprehensive $sessionDuration-minute workout for a $sport $position player.
            
            Experience Level: $experienceLevel
            Training Phase: $trainingPhase
            Available Equipment: $availableEquipment
            Focus Areas: $focusAreas
            ${specialRequirements?.let { "Special Requirements: $it" } ?: ""}
            
            Please provide a detailed, well-formatted workout plan with:
            - Clear exercise names and descriptions
            - Sets, reps, and rest periods for each exercise
            - Coaching cues and technique tips
            - How each exercise benefits the $position position
            - Injury prevention considerations
            
            Format the workout clearly with sections and proper structure.
        """.trimIndent()
        
        // Call Claude API using existing chatWithClaude method
        val conversation = chatWithClaude(
            userId = userId,
            message = userMessage,
            sessionId = null,
            sport = sportEnum,
            position = positionEnum,
            conversationType = ConversationType.WORKOUT_CUSTOMIZATION
        )
        
        // Extract workout title from the response (simple text extraction)
        val workoutTitle = extractSimpleWorkoutTitle(conversation.claudeResponse) 
            ?: "$position $trainingPhase Workout"
        
        // Create and save WorkoutPlan entity - NO PARSING NEEDED
        val workoutPlan = WorkoutPlan(
            user = user,
            sport = sportEnum,
            position = positionEnum,
            workoutName = workoutTitle,
            positionFocus = "Position-specific training for $position in $sport",
            difficultyLevel = experienceLevel.uppercase(),
            durationMinutes = sessionDuration, // Use requested duration
            equipmentNeeded = availableEquipment, // Use requested equipment
            generatedContent = conversation.claudeResponse, // Store full markdown response
            isSaved = false,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        // Save and return the entity
        return workoutPlanRepository.save(workoutPlan)
    }

    private fun extractSimpleWorkoutTitle(content: String): String? {
        return try {
            // Look for common workout title patterns
            val titlePatterns = listOf(
                Regex("""(?i)(?:workout|training|session):\s*([^\n]+)"""),
                Regex("""(?i)# ([^\n]*(?:workout|training|session)[^\n]*)"""),
                Regex("""(?i)## ([^\n]*(?:workout|training|session)[^\n]*)"""),
                Regex("""(?i)### ([^\n]*(?:workout|training|session)[^\n]*)""")
            )
            
            for (pattern in titlePatterns) {
                val match = pattern.find(content)
                if (match != null && match.groupValues.size > 1) {
                    val title = match.groupValues[1].trim()
                    if (title.isNotEmpty()) {
                        return title
                    }
                }
            }
            
            // Fallback: look for the first header in the content
            val headerMatch = Regex("""(?i)#+\s*([^\n]+)""").find(content)
            headerMatch?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            
        } catch (e: Exception) {
            null
        }
    }

    // Helper method to create position-specific system prompt
    private fun createWorkoutSystemPrompt(
        sport: Sport,
        position: Position,
        experienceLevel: String,
        trainingPhase: String
    ): String {
        return """
            You are an elite $sport coach specializing in $position training. Your expertise includes:
            
            - Position-specific movement patterns and physical demands for $position
            - Injury prevention strategies for common $position injuries  
            - Performance enhancement that directly translates to $sport competition
            - Training periodization for $trainingPhase phase
            - Exercise selection based on $experienceLevel level athletes
            
            Create highly specific workouts that directly improve $position performance on the field.
            Every exercise should have a clear connection to game situations and position requirements.
            
            Focus Areas:
            - Functional movement patterns specific to $position
            - Strength and power development for position demands
            - Injury prevention for vulnerable areas
            - Game-specific conditioning and endurance
            
            Always respond with valid JSON containing detailed exercise information including:
            - Specific coaching cues for proper execution
            - How each exercise benefits position performance
            - Game application examples
            - Safety and injury prevention notes
            
            Make each workout challenging but appropriate for $experienceLevel level.
        """.trimIndent()
    }

    // Helper function to extract workout title from Claude's JSON response
    private fun extractWorkoutTitle(content: String): String? {
        return try {
            val jsonStart = content.indexOf("{")
            val jsonEnd = content.lastIndexOf("}")
            if (jsonStart != -1 && jsonEnd != -1) {
                val json = content.substring(jsonStart, jsonEnd + 1)
                val jsonResponse = objectMapper.readValue(json, Map::class.java) as Map<String, Any>
                jsonResponse["workoutTitle"] as? String
            } else null
        } catch (e: Exception) {
            null
        }
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
    
    // Rate limiting based on monthly API cost
    private fun checkRateLimit(user: User) {
        when (user.subscriptionTier) {
            SubscriptionTier.NONE -> {
                println("DEBUG: User has no active subscription")
                throw IllegalStateException("Active subscription required. Choose Basic ($9.99) or Premium ($14.99) to start AI coaching.")
            }
            
            SubscriptionTier.BASIC -> {
                println("DEBUG: User is BASIC tier, checking monthly cost limit")
                val monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                
                try {
                    val monthlyCostCents = claudeConversationRepository.getCostByUserSince(user, monthStart) ?: 0
                    println("DEBUG: Monthly cost in cents: $monthlyCostCents")
                    
                    // Basic tier: $3.00 API budget per month = 300 cents (~150 coaching sessions)
                    if (monthlyCostCents >= 300) {
                        println("DEBUG: Monthly cost limit exceeded for BASIC: $monthlyCostCents >= 300 cents")
                        throw IllegalStateException("Monthly AI coaching budget exceeded (\$${monthlyCostCents/100.0}). Upgrade to Premium for higher limits.")
                    }
                    println("DEBUG: Monthly cost OK for BASIC: $monthlyCostCents < 300 cents")
                } catch (e: Exception) {
                    println("DEBUG: Exception in BASIC cost check: ${e.message}")
                    throw e
                }
            }
            
            SubscriptionTier.PREMIUM -> {
                println("DEBUG: User is PREMIUM tier, checking monthly cost limit")
                val monthStart = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0)
                
                try {
                    val monthlyCostCents = claudeConversationRepository.getCostByUserSince(user, monthStart) ?: 0
                    println("DEBUG: Monthly cost in cents: $monthlyCostCents")
                    
                    // Premium tier: $10.00 API budget per month = 1000 cents (~500 coaching sessions)
                    if (monthlyCostCents >= 1000) {
                        throw IllegalStateException("Monthly usage limit reached (\$${monthlyCostCents/100.0}). Contact support if you need higher limits.")
                    }
                } catch (e: Exception) {
                    throw e
                }
            }
        }
        
        println("DEBUG: Rate limit check completed successfully")
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
        val positionSpecificContext = getPositionSpecificContext(sport, position)
        
        return """
            You are an elite position-specific performance coach for GameIQ - the premier AI-powered sports intelligence platform. Your expertise lies in creating training programs that develop both the PHYSICAL capabilities and TACTICAL INTELLIGENCE required to master the ${position.name.lowercase()} position in $sport.

            GameIQ Philosophy: "Master YOUR Position" - Every exercise should directly translate to on-field performance and tactical advantage.

            ATHLETE PROFILE:
            - Sport: $sport
            - Position: $position
            - Current Level: $difficultyLevel
            - Training Phase: $trainingPhase
            - Focus: Position-specific performance optimization

            POSITION-SPECIFIC CONTEXT:
            ${positionSpecificContext.keyMovements}
            ${positionSpecificContext.commonInjuries}
            ${positionSpecificContext.performanceMetrics}

            Create a workout that embodies "Sports IQ Training" - where every exercise has a clear PURPOSE for position mastery. Return your response as a JSON object with this exact structure:

            {
                "workoutTitle": "Position-specific title that emphasizes mastery (e.g., 'QB Pocket Presence Power Builder')",
                "positionFocus": "2-3 sentence explanation of how this workout specifically improves on-field position performance",
                "exercises": [
                    {
                    "name": "Exercise name",
                    "sets": 3,
                    "reps": "8-12",
                    "restSeconds": 60,
                    "description": "Clear, technical instruction on proper form",
                    "positionBenefit": "Specific explanation of how this translates to better position performance",
                    "gameApplication": "Real game situation where this strength/movement is crucial",
                    "injuryPrevention": "How this exercise prevents common position-specific injuries (if applicable)",
                    "coachingCue": "One key mental cue athletes should focus on during execution"
                    }
                ],
                "equipmentNeeded": "List of required equipment",
                "focusAreas": "Primary muscle groups and movement patterns targeted",
                "estimatedDuration": 45,
                "warmup": "Position-specific warm-up that prepares for the movement patterns in training",
                "cooldown": "Recovery routine emphasizing areas that get stressed in this position",
                "intelligenceNote": "2-3 sentences connecting this physical training to tactical advantages",
                "progressionTip": "How athletes can advance this workout as they master their position",
                "nextLevelUnlock": "What position-specific skill this workout prepares them to tackle next"
            }

            CRITICAL REQUIREMENTS:
            1. Every exercise must have clear position-specific justification
            2. Include 6-8 exercises that target the most important movement patterns for this position
            3. Balance strength, power, mobility, and injury prevention based on position demands
            4. Use coaching language that builds confidence and emphasizes mastery
            5. Reference specific game situations, not just generic "functional movement"
            6. Make the athlete feel like they're training with PURPOSE, not just getting tired

            Remember: This isn't just a workout - it's a step toward position mastery. Make every rep count toward game-day dominance.
        """.trimIndent()
    }

    // Helper function to provide position-specific context
    private fun getPositionSpecificContext(sport: Sport, position: Position): PositionContext {
        return when (sport) {
            Sport.FOOTBALL -> when (position) {
                Position.QB -> PositionContext(
                    keyMovements = "KEY MOVEMENTS: Pocket mobility, quick 3-step drops, throwing mechanics under pressure, hip rotation for torque generation, core stability for accuracy",
                    commonInjuries = "INJURY PREVENTION FOCUS: Shoulder stability, lower back protection, knee valgus prevention during scrambles, rotator cuff health",
                    performanceMetrics = "PERFORMANCE TARGETS: Arm strength for deep throws, pocket awareness (reaction time), accuracy under pressure, scramble mobility"
                )
                Position.RB -> PositionContext(
                    keyMovements = "KEY MOVEMENTS: Explosive first step, lateral cutting, contact balance, vision through holes, stiff-arm power, pass protection stance",
                    commonInjuries = "INJURY PREVENTION FOCUS: ACL protection, shoulder stability for contact, ankle stability for cutting, hamstring flexibility",
                    performanceMetrics = "PERFORMANCE TARGETS: 40-yard dash time, lateral agility, contact balance, vision processing speed"
                )
                Position.WR -> PositionContext(
                    keyMovements = "KEY MOVEMENTS: Route precision, release technique, catch point optimization, RAC ability, contested catch strength",
                    commonInjuries = "INJURY PREVENTION FOCUS: ACL protection during cuts, shoulder stability for catches, hamstring flexibility for speed",
                    performanceMetrics = "PERFORMANCE TARGETS: Route running precision, catch radius, separation speed, contested catch success"
                )
                // Add more football positions...
                else -> PositionContext.default()
            }
            Sport.BASKETBALL -> when (position) {
                Position.PG -> PositionContext(
                    keyMovements = "KEY MOVEMENTS: Court vision processing, quick decision-making under pressure, change of pace dribbling, defensive pressure navigation",
                    commonInjuries = "INJURY PREVENTION FOCUS: Ankle stability for cuts, core strength for contact, shoulder health for ball security",
                    performanceMetrics = "PERFORMANCE TARGETS: Court vision speed, assist-to-turnover ratio, defensive pressure handling, leadership presence"
                )
                // Add more basketball positions...
                else -> PositionContext.default()
            }
            Sport.BASEBALL -> when (position) {
                Position.PITCHER -> PositionContext(
                    keyMovements = "KEY MOVEMENTS: Kinetic chain sequencing, hip-shoulder separation, deceleration mechanics, leg drive efficiency",
                    commonInjuries = "INJURY PREVENTION FOCUS: Rotator cuff health, UCL protection, lower back stability, hip mobility",
                    performanceMetrics = "PERFORMANCE TARGETS: Velocity consistency, command accuracy, stamina through pitch counts, injury resilience"
                )
                // Add more baseball positions...
                else -> PositionContext.default()
            }
            Sport.SOCCER -> when (position) {
                Position.GOALKEEPER -> PositionContext(
                    keyMovements = "KEY MOVEMENTS: Explosive diving saves, quick feet for positioning, distribution accuracy, aerial command presence",
                    commonInjuries = "INJURY PREVENTION FOCUS: Shoulder stability for diving, core strength for distribution, hip mobility for low saves",
                    performanceMetrics = "PERFORMANCE TARGETS: Reaction time, distribution accuracy, aerial dominance, shot-stopping percentage"
                )
                // Add more soccer positions...
                else -> PositionContext.default()
            }
            else -> PositionContext.default()
        }
    }

    // Data class for position-specific context
    private data class PositionContext(
        val keyMovements: String,
        val commonInjuries: String,
        val performanceMetrics: String
    ) {
        companion object {
            fun default() = PositionContext(
                keyMovements = "KEY MOVEMENTS: Sport-specific movement patterns and athletic requirements",
                commonInjuries = "INJURY PREVENTION FOCUS: Common sport-related injury prevention",
                performanceMetrics = "PERFORMANCE TARGETS: Position-specific performance indicators"
            )
        }
    }

    data class WorkoutContent(
        val exercisesJson: String,           // JSON array of exercise objects
        val equipmentNeeded: String? = null,  // Equipment required for workout
        val focusAreas: String? = null,      // Primary focus areas/muscle groups
        val estimatedDuration: Int? = null,   // Estimated workout duration in minutes
        val promptUsed: String,              // System prompt used for generation
        val enhancedContent: String? = null  // NEW: Formatted, human-readable workout content
    )
    
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
            // Strip markdown code blocks FIRST
            val cleanedContent = content
                .replace("```json", "")
                .replace("```", "")
                .trim()
            
            println("DEBUG: Cleaned content preview: ${cleanedContent.take(200)}...")
            
            // Extract JSON for structured data, but preserve full content for frontend
            val jsonStartIndex = cleanedContent.indexOf("{")
            val jsonEndIndex = cleanedContent.lastIndexOf("}")
            
            if (jsonStartIndex != -1 && jsonEndIndex != -1 && jsonEndIndex > jsonStartIndex) {
                val extractedJson = cleanedContent.substring(jsonStartIndex, jsonEndIndex + 1)
                println("DEBUG: Extracted JSON preview: ${extractedJson.take(200)}...")
                
                val jsonResponse = objectMapper.readValue(extractedJson, Map::class.java) as Map<String, Any>
                
                // Extract structured data for database fields
                val workoutTitle = jsonResponse["workoutTitle"] as String? ?: "Position-Specific Workout"
                val exercises = jsonResponse["exercises"] as List<Map<String, Any>>? ?: emptyList()
                val equipmentNeeded = jsonResponse["equipmentNeeded"] as String? ?: "Basic equipment"
                val focusAreas = jsonResponse["focusAreas"] as String? ?: "Position-specific training"
                val estimatedDuration = (jsonResponse["estimatedDuration"] as Number?)?.toInt() ?: 45
                
                println("DEBUG: Successfully parsed structured data. Workout: $workoutTitle")
                println("DEBUG: Exercises count: ${exercises.size}")
                
                // Return both structured data AND preserve the full Claude response with markdown
                WorkoutContent(
                    exercisesJson = objectMapper.writeValueAsString(exercises),
                    equipmentNeeded = equipmentNeeded,
                    focusAreas = focusAreas,
                    estimatedDuration = estimatedDuration,
                    promptUsed = promptUsed,
                    enhancedContent = content // Store the FULL Claude response with markdown intact
                )
                
            } else {
                throw IllegalArgumentException("No valid JSON found in Claude response")
            }
            
        } catch (e: Exception) {
            println("DEBUG: JSON parsing failed, but preserving full Claude response: ${e.message}")
            println("DEBUG: Content preview: ${content.take(200)}...")
            
            // Even if JSON parsing fails, preserve Claude's full response
            // The frontend can still display the formatted content
            WorkoutContent(
                exercisesJson = """[{"name": "AI-Generated Workout", "sets": 3, "reps": "varies"}]""",
                equipmentNeeded = "As specified by user",
                focusAreas = "Position-specific training", 
                estimatedDuration = 45,
                promptUsed = promptUsed,
                enhancedContent = content // Always preserve Claude's full response
            )
        }
    }

    private fun extractWorkoutName(enhancedContent: String?): String? {
        return enhancedContent?.let { content ->
            val titleRegex = Regex("=== (.*?) ===")
            titleRegex.find(content)?.groupValues?.get(1)
        }
    }

    private fun extractPositionFocus(enhancedContent: String?): String? {
        return enhancedContent?.let { content ->
            val focusRegex = Regex("POSITION FOCUS:\\s*([^\\n]+)")
            focusRegex.find(content)?.groupValues?.get(1)
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
        
        // Calculate cost in dollars first
        val inputCostDollars = inputTokens * 0.000003 // $3/1M = $0.000003 per token
        val outputCostDollars = outputTokens * 0.000015 // $15/1M = $0.000015 per token
        val totalCostDollars = inputCostDollars + outputCostDollars
        
        // Convert to cents and round properly
        val totalCostCents = kotlin.math.round(totalCostDollars * 100).toInt()
        
        println("DEBUG: API Cost Calculation")
        println("  Input tokens: $inputTokens × \$0.000003 = \$${String.format("%.6f", inputCostDollars)}")
        println("  Output tokens: $outputTokens × \$0.000015 = \$${String.format("%.6f", outputCostDollars)}")
        println("  Total cost: \$${String.format("%.6f", totalCostDollars)} = ${totalCostCents} cents")
        
        return totalCostCents
    }

    fun getUserConversations(userId: Long): List<ClaudeConversation> {
        return claudeConversationRepository.findByUserId(userId)
    }
}