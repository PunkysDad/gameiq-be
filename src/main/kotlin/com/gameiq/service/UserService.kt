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
            updatedAt = LocalDateTime.now()
        )
        
        return userRepository.save(updatedUser)
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
        
        return newIndex >= currentIndex
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