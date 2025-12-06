package com.gameiq.repository

import com.gameiq.entity.UserWorkoutPreferences
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface UserWorkoutPreferencesRepository : JpaRepository<UserWorkoutPreferences, Long> {
    
    fun findByUserId(userId: Long): Optional<UserWorkoutPreferences>
    
    fun existsByUserId(userId: Long): Boolean
    
    fun deleteByUserId(userId: Long)
}