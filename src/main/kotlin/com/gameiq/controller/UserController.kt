package com.gameiq.controller

import com.gameiq.entity.User
import com.gameiq.entity.Sport
import com.gameiq.entity.Position
import com.gameiq.entity.SubscriptionTier
import com.gameiq.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus

data class UserProfileResponse(
    val id: Long,
    val firebaseUid: String,
    val email: String?,
    val displayName: String?,
    val subscriptionTier: String,
    val primarySport: String?,
    val primaryPosition: String?,
    val createdAt: String,
    val isActive: Boolean
)

data class UserProfileUpdateRequest(
    val displayName: String?,
    val primarySport: String?,
    val primaryPosition: String?,
    val age: Int?
)

data class UserStatsResponse(
    val totalQuizzes: Long,
    val averageScore: Double,
    val totalConversations: Long,
    val currentStreak: Int
)

@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:19006"])
class UserController(
    private val userService: UserService
) {
    
    @GetMapping("/{userId}")
    fun getUserProfile(@PathVariable userId: Long): ResponseEntity<UserProfileResponse> {
        return try {
            val user = userService.findById(userId) ?: return ResponseEntity.notFound().build()
            val response = UserProfileResponse(
                id = user.id,
                firebaseUid = user.firebaseUid,
                email = user.email,
                displayName = user.displayName,
                subscriptionTier = user.subscriptionTier.name,
                primarySport = user.primarySport?.name,
                primaryPosition = user.primaryPosition?.name,
                createdAt = user.createdAt.toString(),
                isActive = user.isActive
            )
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @GetMapping("/firebase/{firebaseUid}")
    fun getUserByFirebaseUid(@PathVariable firebaseUid: String): ResponseEntity<UserProfileResponse> {
        return try {
            val user = userService.findByFirebaseUid(firebaseUid) ?: return ResponseEntity.notFound().build()
            val response = UserProfileResponse(
                id = user.id,
                firebaseUid = user.firebaseUid,
                email = user.email,
                displayName = user.displayName,
                subscriptionTier = user.subscriptionTier.name,
                primarySport = user.primarySport?.name,
                primaryPosition = user.primaryPosition?.name,
                createdAt = user.createdAt.toString(),
                isActive = user.isActive
            )
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @PutMapping("/{userId}")
    fun updateUserProfile(
        @PathVariable userId: Long,
        @RequestBody updateRequest: UserProfileUpdateRequest
    ): ResponseEntity<UserProfileResponse> {
        return try {
            // Convert string sport/position to enums
            val sport = updateRequest.primarySport?.let { 
                try { Sport.valueOf(it.uppercase()) } catch (e: Exception) { null }
            }
            val position = updateRequest.primaryPosition?.let {
                try { Position.valueOf(it.uppercase()) } catch (e: Exception) { null }
            }
            
            val updatedUser = userService.updateUserProfile(
                userId = userId,
                displayName = updateRequest.displayName,
                primarySport = sport,
                primaryPosition = position,
                age = updateRequest.age
            )
            
            val response = UserProfileResponse(
                id = updatedUser.id,
                firebaseUid = updatedUser.firebaseUid,
                email = updatedUser.email,
                displayName = updatedUser.displayName,
                subscriptionTier = updatedUser.subscriptionTier.name,
                primarySport = updatedUser.primarySport?.name,
                primaryPosition = updatedUser.primaryPosition?.name,
                createdAt = updatedUser.createdAt.toString(),
                isActive = updatedUser.isActive
            )
            ResponseEntity.ok(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
    @GetMapping("/{userId}/stats")
    fun getUserStats(@PathVariable userId: Long): ResponseEntity<UserStatsResponse> {
        return try {
            // For now, return mock stats until we implement the actual stats methods
            val stats = UserStatsResponse(
                totalQuizzes = 0L,
                averageScore = 0.0,
                totalConversations = 0L,
                currentStreak = 0
            )
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            ResponseEntity.notFound().build()
        }
    }
    
    @PostMapping("/{userId}/subscription")
    fun updateSubscription(
        @PathVariable userId: Long,
        @RequestParam tier: String
    ): ResponseEntity<Map<String, Any>> {
        return try {
            // Convert string to SubscriptionTier enum
            val subscriptionTier = try {
                SubscriptionTier.valueOf(tier.uppercase())
            } catch (e: Exception) {
                return ResponseEntity.badRequest().body(mapOf(
                    "success" to false,
                    "message" to "Invalid subscription tier: $tier"
                ))
            }
            
            val updatedUser = userService.upgradeSubscription(userId, subscriptionTier)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Subscription updated to $tier",
                "newTier" to updatedUser.subscriptionTier.name
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to e.message.orEmpty()
            ))
        }
    }
    
    @DeleteMapping("/{userId}")
    fun deactivateUser(@PathVariable userId: Long): ResponseEntity<Map<String, Any>> {
        return try {
            userService.deactivateUser(userId)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "User deactivated successfully"
            ))
        } catch (e: Exception) {
            ResponseEntity.badRequest().body(mapOf(
                "success" to false,
                "message" to e.message.orEmpty()
            ))
        }
    }
}