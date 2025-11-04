package com.gameiq.controller

import com.gameiq.service.ClaudeService
import com.gameiq.service.data.*
import com.gameiq.entity.ClaudeConversation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus

data class ChatRequest(
    val message: String,
    val sessionId: String? = null,
    val sport: String? = null,
    val position: String? = null,
    val conversationType: String = "GENERAL_SPORTS_QUESTION"
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

data class ConversationHistoryResponse(
    val sessionId: String,
    val sport: String?,
    val position: String?,
    val messageCount: Int,
    val startedAt: String,
    val lastMessageAt: String,
    val messages: List<ChatResponse>
)

data class UsageStatsResponse(
    val totalConversations: Long,
    val totalMessages: Long,
    val weeklyUsage: Long,
    val remainingQuestions: Int?, // For free users
    val subscriptionTier: String
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
                tokenUsage = conversation.tokenUsage
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
    
    @GetMapping("/user/{userId}/sessions")
    fun getUserConversationSessions(
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<List<ConversationHistoryResponse>> {
        return try {
            val sessions = claudeService.getUserConversationSessions(userId, page, size)
            
            val response = sessions.map { session ->
                ConversationHistoryResponse(
                    sessionId = session.sessionId,
                    sport = session.sport,
                    position = session.position,
                    messageCount = session.messageCount,
                    startedAt = session.startedAt.toString(),
                    lastMessageAt = session.lastMessageAt.toString(),
                    messages = session.conversations.map { conv ->
                        ChatResponse(
                            id = conv.id,
                            sessionId = conv.sessionId,
                            userMessage = conv.userMessage,
                            claudeResponse = conv.claudeResponse,
                            sport = conv.sport?.name,
                            position = conv.position?.name,
                            conversationType = conv.conversationType.name,
                            timestamp = conv.createdAt.toString(),
                            tokenUsage = conv.tokenUsage
                        )
                    }
                )
            }
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/session/{sessionId}")
    fun getConversationSession(@PathVariable sessionId: String): ResponseEntity<ConversationHistoryResponse> {
        return try {
            val session = claudeService.getConversationSession(sessionId)
            
            val response = ConversationHistoryResponse(
                sessionId = session.sessionId,
                sport = session.sport,
                position = session.position,
                messageCount = session.messageCount,
                startedAt = session.startedAt.toString(),
                lastMessageAt = session.lastMessageAt.toString(),
                messages = session.conversations.map { conv ->
                    ChatResponse(
                        id = conv.id,
                        sessionId = conv.sessionId,
                        userMessage = conv.userMessage,
                        claudeResponse = conv.claudeResponse,
                        sport = conv.sport?.name,
                        position = conv.position?.name,
                        conversationType = conv.conversationType.name,
                        timestamp = conv.createdAt.toString(),
                        tokenUsage = conv.tokenUsage
                    )
                }
            )
            
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/user/{userId}/usage")
    fun getUserUsageStats(@PathVariable userId: Long): ResponseEntity<UsageStatsResponse> {
        return try {
            val stats = claudeService.getUserUsageStats(userId)
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @DeleteMapping("/session/{sessionId}")
    fun deleteConversationSession(
        @PathVariable sessionId: String,
        @RequestParam userId: Long
    ): ResponseEntity<Map<String, Any>> {
        return try {
            claudeService.deleteConversationSession(sessionId, userId)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Conversation session deleted successfully"
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to e.message.orEmpty()
            ))
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
    
    @PostMapping("/session/{sessionId}/rate")
    fun rateConversation(
        @PathVariable sessionId: String,
        @RequestParam userId: Long,
        @RequestParam rating: Int,
        @RequestParam(required = false) feedback: String?
    ): ResponseEntity<Map<String, Any>> {
        return try {
            if (rating !in 1..5) {
                return ResponseEntity.badRequest().body(mapOf(
                    "success" to false,
                    "message" to "Rating must be between 1 and 5"
                ))
            }
            
            claudeService.rateConversation(sessionId, userId, rating, feedback)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Rating submitted successfully"
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to e.message.orEmpty()
            ))
        }
    }
}