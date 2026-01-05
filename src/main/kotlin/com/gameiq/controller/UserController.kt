package com.gameiq.controller

import com.gameiq.entity.User
import com.gameiq.entity.Sport
import com.gameiq.entity.Position
import com.gameiq.entity.SubscriptionTier
import com.gameiq.service.UserService
import com.gameiq.service.ClaudeService
import com.gameiq.service.WorkoutService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

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

data class UserCreateRequest(
    val email: String,
    val firebaseUid: String,
    val displayName: String,
    val primarySport: String?,
    val primaryPosition: String?,
    val age: Int?
)

data class UserStatsResponse(
    val totalQuizzes: Long,
    val averageScore: Double,
    val totalConversations: Long,
    val totalWorkouts: Long,
    val daysSinceLastActivity: Int,
    val currentStreak: Int
)

@RestController
@RequestMapping("/users")
@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:19006"])
class UserController(
    private val userService: UserService,
    private val claudeService: ClaudeService,
    private val workoutService: WorkoutService
) {
    
    @PostMapping
    fun createUser(@RequestBody createRequest: UserCreateRequest): ResponseEntity<UserProfileResponse> {
        return try {
            // Convert string sport/position to enums
            val sport = createRequest.primarySport?.let { 
                try { Sport.valueOf(it.uppercase()) } catch (e: Exception) { null }
            }
            val position = createRequest.primaryPosition?.let {
                try { Position.valueOf(it.uppercase()) } catch (e: Exception) { null }
            }
            
            val user = userService.createUser(
                email = createRequest.email,
                firebaseUid = createRequest.firebaseUid,
                displayName = createRequest.displayName
            )
            
            // Update with additional profile info if provided
            val updatedUser = if (sport != null || position != null || createRequest.age != null) {
                userService.updateUserProfile(
                    userId = user.id,
                    displayName = user.displayName,
                    primarySport = sport,
                    primaryPosition = position,
                    age = createRequest.age
                )
            } else user
            
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
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: Exception) {
            ResponseEntity.badRequest().build()
        }
    }
    
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
            // Get actual conversation count from ClaudeService
            val conversationCount = claudeService.getUserConversations(userId).size.toLong()
            
            // Get actual workout count from WorkoutService
            val workoutCount = workoutService.getUserWorkoutPlans(userId).size.toLong()
            
            // Calculate days since last activity
            val conversations = claudeService.getUserConversations(userId)
            val workouts = workoutService.getUserWorkoutPlans(userId)
            
            val lastConversationDate = conversations.maxByOrNull { it.createdAt }?.createdAt
            val lastWorkoutDate = workouts.maxByOrNull { it.createdAt }?.createdAt
            
            val mostRecentActivity = listOfNotNull(lastConversationDate, lastWorkoutDate)
                .maxByOrNull { it } ?: LocalDateTime.now().minusDays(365)
            
            val daysSinceLastActivity = ChronoUnit.DAYS.between(mostRecentActivity, LocalDateTime.now()).toInt()
            
            val stats = UserStatsResponse(
                totalQuizzes = 0L, // Keep as 0 since we're not using quizzes for now
                averageScore = 0.0, // Keep as 0 since we're not using quizzes for now
                totalConversations = conversationCount,
                totalWorkouts = workoutCount,
                daysSinceLastActivity = daysSinceLastActivity,
                currentStreak = 0 // Can implement later
            )
            
            ResponseEntity.ok(stats)
        } catch (e: Exception) {
            println("Error getting user stats for userId $userId: ${e.message}")
            // Return empty stats on error instead of 404
            val emptyStats = UserStatsResponse(
                totalQuizzes = 0L,
                averageScore = 0.0,
                totalConversations = 0L,
                totalWorkouts = 0L,
                daysSinceLastActivity = 0,
                currentStreak = 0
            )
            ResponseEntity.ok(emptyStats)
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