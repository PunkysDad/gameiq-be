package com.gameiq.controller

import com.gameiq.service.ClaudeService
import com.gameiq.entity.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class CoachingSituationRequest(
    val sport: String,
    val situation: Map<String, String>
)

data class CoachingAnalysisResponse(
    val sport: String,
    val situation: String,
    val recommendation: String,
    val reasoning: List<String>,
    val timestamp: String,
    val conversationId: Long
)

@RestController
@RequestMapping("/coaching")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:19006"])
class CoachingController(
    private val claudeService: ClaudeService
) {

    @PostMapping("/analyze")
    fun analyzeCoachingSituation(
        @RequestBody request: CoachingSituationRequest,
        @RequestParam userId: Long
    ): ResponseEntity<Any> {
        return try {
            val sport = Sport.valueOf(request.sport.uppercase())
            val situationDescription = request.situation.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            val coachingMessage = "Analyze this ${sport.name} situation: $situationDescription. Provide strategic recommendation."

            val conversation = claudeService.chatWithClaude(
                userId = userId,
                message = coachingMessage,
                sport = sport,
                conversationType = ConversationType.GAME_STRATEGY
            )

            ResponseEntity.ok(
                CoachingAnalysisResponse(
                    sport = sport.name,
                    situation = situationDescription,
                    recommendation = conversation.claudeResponse,
                    reasoning = listOf("Analysis based on historical data and strategic principles"),
                    timestamp = conversation.createdAt.toString(),
                    conversationId = conversation.id
                )
            )
        } catch (e: IllegalStateException) {
            ResponseEntity.badRequest().body(
                mapOf("message" to (e.message ?: "Subscription limit reached."))
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(
                mapOf("message" to (e.message ?: "Failed to analyze coaching situation."))
            )
        }
    }

    @GetMapping("/test")
    fun testCoaching(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "message" to "Coaching controller is working",
            "availableSports" to Sport.values().joinToString(", ")
        ))
    }
}