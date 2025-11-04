@Entity
data class CoachingScenario(
    @Id @GeneratedValue val id: Long = 0,
    val sport: String,
    val scenarioType: String, // "4th_down", "end_game", "matchup_analysis"  
    val description: String,
    val contextFactors: String, // JSON of situation details
    val createdAt: LocalDateTime = LocalDateTime.now()
)