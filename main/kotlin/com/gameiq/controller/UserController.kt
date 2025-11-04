package com.gameiq.controller

import com.gameiq.entity.User
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
    val primaryPosition: String?
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
            val user = userService.getUserById(userId)
            val response = UserProfileResponse(
                id = user.id,
                firebaseUid = user.firebaseUid,
                email = user.email,
                displayName = user.displayName,
                subscriptionTier = user.subscriptionTier.name,
                primarySport = user.primarySport,
                primaryPosition = user.primaryPosition,
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
            val user = userService.getUserByFirebaseUid(firebaseUid)
            val response = UserProfileResponse(
                id = user.id,
                firebaseUid = user.firebaseUid,
                email = user.email,
                displayName = user.displayName,
                subscriptionTier = user.subscriptionTier.name,
                primarySport = user.primarySport,
                primaryPosition = user.primaryPosition,
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
            val updatedUser = userService.updateUserProfile(
                userId = userId,
                displayName = updateRequest.displayName,
                primarySport = updateRequest.primarySport,
                primaryPosition = updateRequest.primaryPosition
            )
            
            val response = UserProfileResponse(
                id = updatedUser.id,
                firebaseUid = updatedUser.firebaseUid,
                email = updatedUser.email,
                displayName = updatedUser.displayName,
                subscriptionTier = updatedUser.subscriptionTier.name,
                primarySport = updatedUser.primarySport,
                primaryPosition = updatedUser.primaryPosition,
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
            val stats = userService.getUserStats(userId)
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
            userService.updateSubscriptionTier(userId, tier)
            ResponseEntity.ok(mapOf(
                "success" to true,
                "message" to "Subscription updated to $tier",
                "newTier" to tier
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