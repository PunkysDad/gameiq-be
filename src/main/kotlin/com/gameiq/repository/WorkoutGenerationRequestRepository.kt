package com.gameiq.repository

import com.gameiq.entity.WorkoutGenerationRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface WorkoutGenerationRequestRepository : JpaRepository<WorkoutGenerationRequest, Long> {
    
    // Count workout generations by user in a time period
    @Query("SELECT COUNT(w) FROM WorkoutGenerationRequest w WHERE w.userId = :userId AND w.createdAt >= :since")
    fun countByUserIdSince(
        @Param("userId") userId: Long,
        @Param("since") since: LocalDateTime
    ): Long
    
    // Get total cost for user in a time period
    @Query("SELECT COALESCE(SUM(w.costCents), 0) FROM WorkoutGenerationRequest w WHERE w.userId = :userId AND w.createdAt >= :since")
    fun sumCostByUserIdSince(
        @Param("userId") userId: Long,
        @Param("since") since: LocalDateTime
    ): Long
    
    // Get total tokens used by user in a time period
    @Query("SELECT COALESCE(SUM(w.tokensUsed), 0) FROM WorkoutGenerationRequest w WHERE w.userId = :userId AND w.createdAt >= :since")
    fun sumTokensByUserIdSince(
        @Param("userId") userId: Long,
        @Param("since") since: LocalDateTime
    ): Long
    
    // Analytics queries
    @Query("SELECT AVG(w.generationTimeMs) FROM WorkoutGenerationRequest w WHERE w.createdAt >= :since")
    fun averageGenerationTime(@Param("since") since: LocalDateTime): Double?
    
    @Query("SELECT w.sport, COUNT(w) FROM WorkoutGenerationRequest w WHERE w.createdAt >= :since GROUP BY w.sport")
    fun getGenerationStatsBySport(@Param("since") since: LocalDateTime): List<Array<Any>>
    
    @Query("SELECT w.position, COUNT(w) FROM WorkoutGenerationRequest w WHERE w.createdAt >= :since GROUP BY w.position")
    fun getGenerationStatsByPosition(@Param("since") since: LocalDateTime): List<Array<Any>>
}