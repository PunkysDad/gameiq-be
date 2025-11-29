package com.gameiq.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    
    @Column(unique = true, nullable = false)
    val email: String,
    
    @Column(name = "firebase_uid", unique = true, nullable = false)
    val firebaseUid: String,
    
    @Column(name = "display_name", nullable = false)
    val displayName: String,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "subscription_tier", nullable = false)
    val subscriptionTier: SubscriptionTier = SubscriptionTier.NONE,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_sport")
    val primarySport: Sport? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_position")
    val primaryPosition: Position? = null,
    
    @Column(name = "age")
    val age: Int? = null,
    
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "last_active_at")
    val lastActiveAt: LocalDateTime? = null,
    
    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true
)

enum class SubscriptionTier {
    NONE, BASIC, PREMIUM
}

enum class Sport {
    FOOTBALL, BASKETBALL, BASEBALL, SOCCER, HOCKEY
}

enum class Position {
    // Football positions
    QB, RB, WR, OL, TE, LB, DB, DL,
    
    // Basketball positions
    PG, SG, SF, PF, C,
    
    // Baseball positions
    PITCHER, CATCHER, INFIELD, OUTFIELD,
    
    // Soccer positions
    GOALKEEPER, DEFENDER, MIDFIELDER, FORWARD,
    
    // Hockey positions
    CENTER, WINGER, DEFENSEMAN, GOALIE
}