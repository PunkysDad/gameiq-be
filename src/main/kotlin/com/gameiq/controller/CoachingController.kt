package com.gameiq.controller

import com.gameiq.service.ClaudeService
import com.gameiq.entity.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus

// Data classes for coaching functionality
data class CoachingSituationRequest(
    val sport: String,
    val situation: Map<String, String> // e.g. "downDistance" -> "4th and 2"
)

data class CoachingAnalysisResponse(
    val sport: String,
    val situation: String,
    val recommendation: String,
    val reasoning: List<String>,
    val timestamp: String
)

@RestController
@RequestMapping("/api/v1/coaching")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:19006"])
class CoachingController(
    private val claudeService: ClaudeService // Use existing ClaudeService instead
) {
    
    @PostMapping("/analyze")
    fun analyzeCoachingSituation(
        @RequestBody request: CoachingSituationRequest,
        @RequestParam userId: Long
    ): ResponseEntity<CoachingAnalysisResponse> {
        
        return try {
            val sport = Sport.valueOf(request.sport.uppercase())
            
            // Create a simple coaching prompt
            val situationDescription = request.situation.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            val coachingMessage = "Analyze this ${sport.name} situation: $situationDescription. Provide strategic recommendation."
            
            // Use existing ClaudeService for now (simplified approach)
            val conversation = claudeService.chatWithClaude(
                userId = userId,
                message = coachingMessage,
                sport = sport,
                conversationType = ConversationType.GAME_STRATEGY
            )
            
            val response = CoachingAnalysisResponse(
                sport = sport.name,
                situation = situationDescription,
                recommendation = conversation.claudeResponse,
                reasoning = listOf("Analysis based on historical data and strategic principles"),
                timestamp = conversation.createdAt.toString()
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
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