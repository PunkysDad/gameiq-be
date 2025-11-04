@Entity  
data class CoachingAnalysis(
    @Id @GeneratedValue val id: Long = 0,
    val userId: Long,
    val sport: String,
    val scenario: String,
    val recommendation: String,
    val successProbability: String,
    val reasoning: String, // JSON array
    val alternatives: String, // JSON array
    val createdAt: LocalDateTime = LocalDateTime.now()
)