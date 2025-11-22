package com.gameiq.service

import com.gameiq.entity.User
import com.gameiq.entity.SubscriptionTier
import com.gameiq.entity.Sport
import com.gameiq.entity.Position
import com.gameiq.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class UserService(
    private val userRepository: UserRepository
) {
    
    // User creation and authentication
    fun createUser(
        email: String,
        firebaseUid: String,
        displayName: String,
        subscriptionTier: SubscriptionTier = SubscriptionTier.FREE
    ): User {
        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email $email already exists")
        }
        
        val user = User(
            email = email,
            firebaseUid = firebaseUid,
            displayName = displayName,
            subscriptionTier = subscriptionTier
        )
        
        return userRepository.save(user)
    }
    
    fun findByFirebaseUid(firebaseUid: String): User? {
        return userRepository.findByFirebaseUid(firebaseUid)
    }
    
    fun findByEmail(email: String): User? {
        return userRepository.findByEmail(email)
    }
    
    fun findById(userId: Long): User? {
        return userRepository.findById(userId).orElse(null)
    }
    
    // Profile updates
    fun updateUserProfile(
        userId: Long,
        displayName: String?,
        primarySport: Sport?,
        primaryPosition: Position?,
        age: Int?
    ): User {
        val user = findById(userId) ?: throw IllegalArgumentException("User not found")
        
        val updatedUser = user.copy(
            displayName = displayName ?: user.displayName,
            primarySport = primarySport ?: user.primarySport,
            primaryPosition = primaryPosition ?: user.primaryPosition,
            age = age ?: user.age,
            updatedAt = LocalDateTime.now()
        )
        
        return userRepository.save(updatedUser)
    }
    
    // Subscription management
    fun upgradeSubscription(userId: Long, newTier: SubscriptionTier): User {
        val user = findById(userId) ?: throw IllegalArgumentException("User not found")
        
        // Validate upgrade path
        if (!isValidUpgrade(user.subscriptionTier, newTier)) {
            throw IllegalArgumentException("Invalid subscription upgrade from ${user.subscriptionTier} to $newTier")
        }
        
        val updatedUser = user.copy(
            subscriptionTier = newTier,
            updatedAt = LocalDateTime.now()
        )
        
        return userRepository.save(updatedUser)
    }
    
    fun downgradeSubscription(userId: Long): User {
        val user = findById(userId) ?: throw IllegalArgumentException("User not found")
        
        val newTier = when (user.subscriptionTier) {
            SubscriptionTier.TEAM -> SubscriptionTier.FAMILY
            SubscriptionTier.FAMILY -> SubscriptionTier.INDIVIDUAL
            SubscriptionTier.INDIVIDUAL -> SubscriptionTier.FREE
            SubscriptionTier.FREE -> SubscriptionTier.FREE
        }
        
        val updatedUser = user.copy(
            subscriptionTier = newTier,
            updatedAt = LocalDateTime.now()
        )
        
        return userRepository.save(updatedUser)
    }
    
    // Activity tracking
    fun updateLastActive(userId: Long): User {
        val user = findById(userId) ?: throw IllegalArgumentException("User not found")
        
        val updatedUser = user.copy(
            lastActiveAt = LocalDateTime.now()
        )
        
        return userRepository.save(updatedUser)
    }
    
    fun deactivateUser(userId: Long): User {
        val user = findById(userId) ?: throw IllegalArgumentException("User not found")
        
        val updatedUser = user.copy(
            isActive = false,
            updatedAt = LocalDateTime.now()
        )
        
        return userRepository.save(updatedUser)
    }
    
    fun reactivateUser(userId: Long): User {
        val user = findById(userId) ?: throw IllegalArgumentException("User not found")
        
        val updatedUser = user.copy(
            isActive = true,
            lastActiveAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        return userRepository.save(updatedUser)
    }
    
    // User queries and analytics
    fun getActiveUsers(): List<User> {
        return userRepository.findByIsActiveTrue()
    }
    
    fun getRecentlyActiveUsers(hours: Long = 24): List<User> {
        val since = LocalDateTime.now().minusHours(hours)
        return userRepository.findActiveUsersSince(since)
    }
    
    fun getUsersBySubscriptionTier(tier: SubscriptionTier): List<User> {
        return userRepository.findBySubscriptionTier(tier)
    }
    
    fun getUsersBySport(sport: Sport): List<User> {
        return userRepository.findByPrimarySport(sport)
    }
    
    fun getUsersByPosition(position: Position): List<User> {
        return userRepository.findByPrimaryPosition(position)
    }
    
    fun getUsersBySportAndPosition(sport: Sport, position: Position): List<User> {
        return userRepository.findByPrimarySportAndPrimaryPosition(sport, position)
    }
    
    fun getNewUsersInPeriod(days: Long = 30): List<User> {
        val since = LocalDateTime.now().minusDays(days)
        return userRepository.findByCreatedAtAfter(since)
    }
    
    // Subscription analytics
    fun getSubscriptionTierDistribution(): Map<SubscriptionTier, Long> {
        val counts = userRepository.getSubscriptionTierCounts()
        return counts.associate { 
            (it[0] as SubscriptionTier) to (it[1] as Long) 
        }
    }
    
    fun getSportDistribution(): Map<Sport, Long> {
        val counts = userRepository.getSportDistribution()
        return counts.associate { 
            (it[0] as Sport) to (it[1] as Long) 
        }
    }
    
    // Rate limiting checks for API usage
    fun canUseFeature(userId: Long, feature: String): Boolean {
        val user = findById(userId) ?: return false
        
        return when (user.subscriptionTier) {
            SubscriptionTier.FREE -> {
                // Limited features for free users
                when (feature) {
                    "claude_chat" -> true // Will be rate limited in ClaudeService
                    "quiz_creation" -> true // Will be rate limited in QuizService
                    "workout_plans" -> true
                    else -> false
                }
            }
            SubscriptionTier.INDIVIDUAL, SubscriptionTier.FAMILY, SubscriptionTier.TEAM -> {
                // Full access for paid users
                true
            }
        }
    }
    
    fun getFeatureLimits(subscriptionTier: SubscriptionTier): Map<String, Int> {
        return when (subscriptionTier) {
            SubscriptionTier.FREE -> mapOf(
                "claude_questions_per_week" to 5,
                "quizzes_per_week" to 3,
                "workout_plans_saved" to 2
            )
            SubscriptionTier.INDIVIDUAL -> mapOf(
                "claude_questions_per_week" to -1, // unlimited
                "quizzes_per_week" to -1,
                "workout_plans_saved" to -1
            )
            SubscriptionTier.FAMILY -> mapOf(
                "claude_questions_per_week" to -1,
                "quizzes_per_week" to -1,
                "workout_plans_saved" to -1,
                "family_members" to 4
            )
            SubscriptionTier.TEAM -> mapOf(
                "claude_questions_per_week" to -1,
                "quizzes_per_week" to -1,
                "workout_plans_saved" to -1,
                "team_members" to 50
            )
        }
    }
    
    // Helper methods
    private fun isValidUpgrade(currentTier: SubscriptionTier, newTier: SubscriptionTier): Boolean {
        val tierOrder = listOf(
            SubscriptionTier.FREE,
            SubscriptionTier.INDIVIDUAL,
            SubscriptionTier.FAMILY,
            SubscriptionTier.TEAM
        )
        
        val currentIndex = tierOrder.indexOf(currentTier)
        val newIndex = tierOrder.indexOf(newTier)
        
        return newIndex > currentIndex
    }
}