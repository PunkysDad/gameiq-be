interface CoachingScenarioRepository : JpaRepository<CoachingScenario, Long> {
    fun findBySport(sport: String): List<CoachingScenario>
    fun findByScenarioType(scenarioType: String): List<CoachingScenario>
}