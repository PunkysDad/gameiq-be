interface CoachingAnalysisRepository : JpaRepository<CoachingAnalysis, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<CoachingAnalysis>
    fun findByUserIdAndSport(userId: Long, sport: String): List<CoachingAnalysis>
}