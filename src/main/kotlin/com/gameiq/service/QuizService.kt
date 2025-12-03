package com.gameiq.service

import com.gameiq.entity.*
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
        subscriptionTier: SubscriptionTier = SubscriptionTier.NONE
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
            SubscriptionTier.PREMIUM -> SubscriptionTier.BASIC
            SubscriptionTier.BASIC -> SubscriptionTier.NONE
            SubscriptionTier.NONE -> SubscriptionTier.NONE
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
            updatedAt = LocalDateTime.now()
        )
        
        return userRepository.save(updatedUser)
    }
    
    // Helper methods
    private fun isValidUpgrade(currentTier: SubscriptionTier, newTier: SubscriptionTier): Boolean {
        val tierOrder = listOf(
            SubscriptionTier.NONE,
            SubscriptionTier.BASIC,
            SubscriptionTier.PREMIUM
        )
        
        val currentIndex = tierOrder.indexOf(currentTier)
        val newIndex = tierOrder.indexOf(newTier)
        
        return newIndex >= currentIndex
    }
    
    // Subscription benefits and pricing
    fun getSubscriptionBenefits(tier: SubscriptionTier): Map<String, Any> {
        return when (tier) {
            SubscriptionTier.NONE -> mapOf(
                "monthlyApiBudgetCents" to 0,
                "description" to "No active subscription",
                "features" to listOf("Limited access")
            )
            
            SubscriptionTier.BASIC -> mapOf(
                "monthlyApiBudgetCents" to 300, // $3.00 budget
                "description" to "Basic Plan - $9.99/month",
                "features" to listOf(
                    "Unlimited AI coaching for one sport/position",
                    "Basic progress tracking", 
                    "Standard response speed"
                )
            )
            
            SubscriptionTier.PREMIUM -> mapOf(
                "monthlyApiBudgetCents" to 1000, // $10.00 budget  
                "description" to "Premium Plan - $14.99/month",
                "features" to listOf(
                    "Unlimited AI coaching for all sports/positions",
                    "Advanced analytics and progress insights",
                    "Priority response speed",
                    "Social sharing features",
                    "Early access to new features"
                )
            )
        }
    }
    
    fun getMonthlyPrice(tier: SubscriptionTier): Double {
        return when (tier) {
            SubscriptionTier.NONE -> 0.00
            SubscriptionTier.BASIC -> 9.99
            SubscriptionTier.PREMIUM -> 14.99
        }
    }
    
    fun getAllUsers(): List<User> {
        return userRepository.findAll()
    }
    
    fun getActiveUsers(): List<User> {
        return userRepository.findByIsActiveTrue()
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
}