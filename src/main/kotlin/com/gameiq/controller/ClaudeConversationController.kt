package com.gameiq.controller

import com.gameiq.service.ClaudeService
import com.gameiq.entity.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus

data class ChatRequest(
    val message: String,
    val sessionId: String? = null,
    val sport: String? = null,
    val position: String? = null,
    val conversationType: String = "TRAINING_ADVICE" // Fixed: use actual enum value
)

data class ChatResponse(
    val id: Long,
    val sessionId: String,
    val userMessage: String,
    val claudeResponse: String,
    val sport: String?,
    val position: String?,
    val conversationType: String,
    val timestamp: String,
    val tokenUsage: Int?
)

@RestController
@RequestMapping("/api/v1/conversations")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:19006"])
class ClaudeConversationController(
    private val claudeService: ClaudeService
) {
    
    @PostMapping("/chat")
    fun chatWithClaude(
        @RequestParam userId: Long,
        @RequestBody request: ChatRequest
    ): ResponseEntity<ChatResponse> {
        return try {
            val conversation = claudeService.chatWithClaude(
                userId = userId,
                message = request.message,
                sessionId = request.sessionId,
                sport = request.sport?.let { Sport.valueOf(it.uppercase()) },
                position = request.position?.let { Position.valueOf(it.uppercase()) },
                conversationType = ConversationType.valueOf(request.conversationType.uppercase())
            )
            
            val response = ChatResponse(
                id = conversation.id,
                sessionId = conversation.sessionId,
                userMessage = conversation.userMessage,
                claudeResponse = conversation.claudeResponse,
                sport = conversation.sport?.name,
                position = conversation.position?.name,
                conversationType = conversation.conversationType.name,
                timestamp = conversation.createdAt.toString(),
                tokenUsage = (conversation.tokensUsedInput ?: 0) + (conversation.tokensUsedOutput ?: 0)
            )
            
            ResponseEntity.ok(response)
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(
                ChatResponse(
                    id = 0L,
                    sessionId = "",
                    userMessage = request.message,
                    claudeResponse = "Rate limit exceeded. ${e.message}",
                    sport = request.sport,
                    position = request.position,
                    conversationType = request.conversationType,
                    timestamp = java.time.LocalDateTime.now().toString(),
                    tokenUsage = null
                )
            )
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/available-options")
    fun getAvailableConversationOptions(): ResponseEntity<Map<String, List<String>>> {
        return ResponseEntity.ok(mapOf(
            "sports" to Sport.values().map { it.name },
            "positions" to Position.values().map { it.name },
            "conversationTypes" to ConversationType.values().map { it.name }
        ))
    }
}