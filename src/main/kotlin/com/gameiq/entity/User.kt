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
    FOOTBALL,
    BASKETBALL, 
    BASEBALL,
    SOCCER;
    
    companion object {
        fun fromString(sport: String): Sport {
            return valueOf(sport.uppercase())
        }
    }
}

enum class Position {
    // Football positions
    QUARTERBACK,
    RUNNING_BACK,
    WIDE_RECEIVER,
    TIGHT_END,
    OFFENSIVE_LINE,
    LINEBACKER,
    DEFENSIVE_BACK,
    DEFENSIVE_LINE,
    
    // Basketball positions  
    POINT_GUARD,
    SHOOTING_GUARD,
    SMALL_FORWARD,
    POWER_FORWARD,
    CENTER,
    
    // Baseball positions
    PITCHER,
    CATCHER,
    INFIELD,
    OUTFIELD,
    
    // Soccer positions
    GOALKEEPER,
    DEFENDER,
    MIDFIELDER,
    FORWARD;
    
    companion object {
        fun fromString(position: String): Position {
            // Handle common variations
            val normalized = when (position.lowercase().replace("-", "_").replace(" ", "_")) {
                "qb" -> "quarterback"
                "rb" -> "running_back"
                "wr" -> "wide_receiver"
                "te" -> "tight_end"
                "ol" -> "offensive_line"
                "lb" -> "linebacker"
                "db" -> "defensive_back"
                "dl" -> "defensive_line"
                "pg" -> "point_guard"
                "sg" -> "shooting_guard"
                "sf" -> "small_forward"
                "pf" -> "power_forward"
                "c" -> "center"
                "goalie" -> "goalkeeper"
                else -> position.lowercase().replace("-", "_").replace(" ", "_")
            }
            
            return valueOf(normalized.uppercase())
        }
    }
}

enum class DifficultyLevel {
    BEGINNER,
    INTERMEDIATE, 
    ADVANCED;
    
    companion object {
        fun fromString(level: String): DifficultyLevel {
            return valueOf(level.uppercase())
        }
    }
}

enum class TrainingPhase {
    OFF_SEASON,
    PRE_SEASON,
    IN_SEASON,
    POST_SEASON;
    
    companion object {
        fun fromString(phase: String): TrainingPhase {
            return valueOf(phase.uppercase().replace("-", "_"))
        }
    }
}