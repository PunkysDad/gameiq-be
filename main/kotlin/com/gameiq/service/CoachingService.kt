@Service
class CoachingService(
    private val claudeService: ClaudeService,
    private val coachingAnalysisRepository: CoachingAnalysisRepository,
    private val coachingScenarioRepository: CoachingScenarioRepository
) {
    
    suspend fun analyzeCoachingSituation(
        userId: Long,
        sport: String,
        situation: CoachingSituationRequest
    ): CoachingAnalysisResponse {
        
        // Generate coaching-specific prompt using historical data
        val prompt = generateCoachingPrompt(sport, situation)
        
        // Get analysis from Claude
        val claudeResponse = claudeService.getCoachingAnalysis(prompt)
        
        // Save analysis for user
        val analysis = CoachingAnalysis(
            userId = userId,
            sport = sport,
            scenario = situation.toString(),
            recommendation = claudeResponse.recommendation,
            successProbability = claudeResponse.successProbability,
            reasoning = claudeResponse.reasoning,
            alternatives = claudeResponse.alternatives
        )
        
        coachingAnalysisRepository.save(analysis)
        
        return CoachingAnalysisResponse.from(claudeResponse)
    }
    
    private fun generateCoachingPrompt(sport: String, situation: CoachingSituationRequest): String {
        return when (sport.uppercase()) {
            "FOOTBALL" -> generateFootballCoachingPrompt(situation)
            "BASKETBALL" -> generateBasketballCoachingPrompt(situation) 
            "BASEBALL" -> generateBaseballCoachingPrompt(situation)
            else -> generateGenericCoachingPrompt(sport, situation)
        }
    }
    
    private fun generateFootballCoachingPrompt(situation: CoachingSituationRequest): String {
        return """
        You are an elite NFL/college football coach with 20+ years of experience.
        
        SITUATION ANALYSIS:
        - Down & Distance: ${situation.context["downDistance"]}
        - Field Position: ${situation.context["fieldPosition"]}
        - Score: ${situation.context["score"]}
        - Time Remaining: ${situation.context["timeRemaining"]}
        - Quarter: ${situation.context["quarter"]}
        
        Using historical NFL data and coaching principles, analyze this situation and provide:
        
        1. Your primary strategic recommendation
        2. Success probability based on league historical data
        3. 2-3 alternative strategies with their success rates
        4. Key factors that influence this decision
        5. What successful coaches typically do in this situation
        
        Return response as JSON with fields: recommendation, successProbability, alternatives, reasoning, historicalContext
        """.trimIndent()
    }
    
    // Similar methods for basketball, baseball, etc.
}