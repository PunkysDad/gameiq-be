package com.gameiq.controller

import com.gameiq.service.ClaudeService
import com.gameiq.service.data.*
import com.gameiq.entity.ClaudeConversation
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/api/v1/coaching")
@CrossOrigin(origins = ["http://localhost:3000"])
class CoachingController(
    private val coachingService: CoachingService
) {
    
    @PostMapping("/analyze")
    suspend fun analyzeCoachingSituation(
        @RequestBody request: CoachingSituationRequest,
        @RequestHeader("User-ID") userId: Long
    ): ResponseEntity<CoachingAnalysisResponse> {
        
        return try {
            val analysis = coachingService.analyzeCoachingSituation(
                userId = userId,
                sport = request.sport,
                situation = request
            )
            ResponseEntity.ok(analysis)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/history")
    suspend fun getCoachingHistory(
        @RequestHeader("User-ID") userId: Long,
        @RequestParam(required = false) sport: String?
    ): ResponseEntity<List<CoachingAnalysisResponse>> {
        
        val history = if (sport != null) {
            coachingService.getCoachingHistory(userId, sport)
        } else {
            coachingService.getCoachingHistory(userId)
        }
        
        return ResponseEntity.ok(history)
    }
}