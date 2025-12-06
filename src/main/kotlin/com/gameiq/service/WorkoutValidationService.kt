package com.gameiq.service

import com.gameiq.entity.Sport
import com.gameiq.entity.Position
import com.gameiq.entity.DifficultyLevel
import org.springframework.stereotype.Service

@Service 
class WorkoutValidationService {
    
    // Valid sport-position combinations
    private val validPositions = mapOf(
        Sport.FOOTBALL to listOf(
            Position.QUARTERBACK,
            Position.RUNNING_BACK,
            Position.WIDE_RECEIVER,
            Position.TIGHT_END,
            Position.OFFENSIVE_LINE,
            Position.LINEBACKER,
            Position.DEFENSIVE_BACK,
            Position.DEFENSIVE_LINE
        ),
        Sport.BASKETBALL to listOf(
            Position.POINT_GUARD,
            Position.SHOOTING_GUARD,
            Position.SMALL_FORWARD,
            Position.POWER_FORWARD,
            Position.CENTER
        ),
        Sport.BASEBALL to listOf(
            Position.PITCHER,
            Position.CATCHER,
            Position.INFIELD,
            Position.OUTFIELD
        ),
        Sport.SOCCER to listOf(
            Position.GOALKEEPER,
            Position.DEFENDER,
            Position.MIDFIELDER,
            Position.FORWARD
        )
    )
    
    fun validateWorkoutRequest(request: GenerateWorkoutRequest) {
        // Basic validation for now - we'll implement this properly later
        if (request.sport.isBlank()) {
            throw RuntimeException("Sport cannot be blank")
        }
        if (request.position.isBlank()) {
            throw RuntimeException("Position cannot be blank")
        }
    }
    
    fun getSupportedSports(): List<String> {
        return Sport.values().map { it.name.lowercase() }
    }
    
    fun getSupportedPositions(sport: String): List<String> {
        return try {
            val sportEnum = Sport.fromString(sport)
            validPositions[sportEnum]?.map { it.name.lowercase().replace("_", "-") } ?: emptyList()
        } catch (e: IllegalArgumentException) {
            emptyList()
        }
    }
    
    fun getSupportedEquipment(): List<String> {
        return listOf("bodyweight", "dumbbells", "barbell", "resistance_bands", "medicine_ball")
    }
    
    fun getValidGoals(): List<String> {
        return listOf("strength", "power", "speed", "agility", "endurance", "mobility")
    }
}