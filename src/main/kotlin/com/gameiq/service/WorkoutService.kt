// WorkoutService.kt
package com.gameiq.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.gameiq.entity.*
import com.gameiq.repository.*
import com.gameiq.exception.WorkoutNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class WorkoutService(
    private val workoutPlanRepository: WorkoutPlanRepository,
    private val workoutGenerationRequestRepository: WorkoutGenerationRequestRepository,
    private val userWorkoutPreferencesRepository: UserWorkoutPreferencesRepository,
    private val claudeService: ClaudeService,
    private val workoutValidationService: WorkoutValidationService,
    private val objectMapper: ObjectMapper
) {

    @Transactional
    fun generateWorkout(userId: Long, request: GenerateWorkoutRequest): WorkoutPlanResponse {
        // Validate the request first
        workoutValidationService.validateWorkoutRequest(request)
        
        val startTime = System.currentTimeMillis()
        
        // Get user preferences for context
        val userPrefs = getUserWorkoutPreferences(userId)
        
        // Use your existing ClaudeService with workout-specific conversation type
        val conversation = claudeService.chatWithClaude(
            userId = userId,
            message = createWorkoutUserMessage(request),
            sport = Sport.fromString(request.sport),
            position = Position.fromString(request.position),
            conversationType = ConversationType.WORKOUT_CUSTOMIZATION
        )
        
        val endTime = System.currentTimeMillis()
        val generationTimeMs = (endTime - startTime).toInt()
        
        // Parse the Claude response to get structured workout
        val generatedWorkout = try {
            objectMapper.readValue(conversation.claudeResponse, GeneratedWorkout::class.java)
        } catch (e: Exception) {
            // If JSON parsing fails, create a fallback workout from the text response
            createFallbackWorkout(request, conversation.claudeResponse)
        }
        
        // Save the workout plan
        val workoutPlan = WorkoutPlan(
            userId = userId,
            sport = request.sport,
            position = request.position,
            workoutName = generatedWorkout.workoutName,
            durationMinutes = generatedWorkout.duration,
            difficultyLevel = generatedWorkout.difficultyLevel,
            equipmentNeeded = generatedWorkout.equipmentUsed.toTypedArray(),
            positionFocus = generatedWorkout.positionFocus,
            generatedContent = objectMapper.writeValueAsString(generatedWorkout),
            isSaved = false // User needs to explicitly save it
        )
        
        val savedWorkoutPlan = workoutPlanRepository.save(workoutPlan)
        
        // Track the generation request for analytics (reuse conversation data)
        val generationRequest = WorkoutGenerationRequest(
            userId = userId,
            sport = request.sport,
            position = request.position,
            inputParameters = objectMapper.writeValueAsString(request),
            tokensUsed = (conversation.tokensUsedInput ?: 0) + (conversation.tokensUsedOutput ?: 0),
            costCents = conversation.apiCostCents ?: 0,
            generationTimeMs = generationTimeMs
        )
        workoutGenerationRequestRepository.save(generationRequest)
        
        return convertToResponse(savedWorkoutPlan, generatedWorkout)
    }
    
    private fun createFallbackWorkout(request: GenerateWorkoutRequest, textResponse: String): GeneratedWorkout {
        // Create a basic workout structure when JSON parsing fails
        return GeneratedWorkout(
            workoutName = "${request.position.replaceFirstChar { it.uppercase() }} ${request.sport.replaceFirstChar { it.uppercase() }} Workout",
            duration = request.timeAvailable,
            difficultyLevel = request.experience,
            positionFocus = "Position-specific training for ${request.position} in ${request.sport}",
            warmUp = listOf(
                WorkoutExercise(
                    name = "Dynamic Warm-up",
                    sets = 1,
                    reps = "5-10 minutes",
                    rest = "None",
                    description = "Light movement to prepare for training",
                    positionBenefit = "Injury prevention and movement preparation"
                )
            ),
            mainExercises = listOf(
                WorkoutExercise(
                    name = "Position-Specific Training",
                    sets = 3,
                    reps = "Based on workout plan",
                    rest = "60-90 seconds",
                    description = textResponse.take(300), // Use Claude's response as description
                    positionBenefit = "Improves ${request.position} specific skills and fitness"
                )
            ),
            coolDown = listOf(
                WorkoutExercise(
                    name = "Cool Down",
                    sets = 1,
                    reps = "5-10 minutes",
                    rest = "None",
                    description = "Light stretching and recovery",
                    positionBenefit = "Aids recovery and flexibility"
                )
            ),
            equipmentUsed = request.equipmentAvailable,
            notes = "AI-generated workout plan for position-specific development"
        )
    }
    
    private fun createWorkoutUserMessage(request: GenerateWorkoutRequest): String {
        return """
        Create a ${request.timeAvailable}-minute ${request.position} workout for a ${request.experience} level athlete.
        
        Available equipment: ${request.equipmentAvailable.joinToString(", ")}
        Training goals: ${request.goals.joinToString(", ")}
        Sport: ${request.sport}
        Position: ${request.position}
        
        Generate a complete position-specific workout plan in JSON format.
        """.trimIndent()
    }
    
    @Transactional
    fun saveWorkout(userId: Long, workoutPlanId: Long): WorkoutPlanResponse {
        val workoutPlan = workoutPlanRepository.findById(workoutPlanId)
            .orElseThrow { WorkoutNotFoundException("Workout plan not found") }
        
        if (workoutPlan.userId != userId) {
            throw SecurityException("Cannot save workout plan belonging to another user")
        }
        
        val updatedPlan = workoutPlan.copy(isSaved = true, updatedAt = LocalDateTime.now())
        val savedPlan = workoutPlanRepository.save(updatedPlan)
        
        val generatedWorkout = objectMapper.readValue(savedPlan.generatedContent, GeneratedWorkout::class.java)
        return convertToResponse(savedPlan, generatedWorkout)
    }
    
    @Transactional
    fun unsaveWorkout(userId: Long, workoutPlanId: Long): WorkoutPlanResponse {
        val workoutPlan = workoutPlanRepository.findById(workoutPlanId)
            .orElseThrow { WorkoutNotFoundException("Workout plan not found") }
        
        if (workoutPlan.userId != userId) {
            throw SecurityException("Cannot modify workout plan belonging to another user")
        }
        
        val updatedPlan = workoutPlan.copy(isSaved = false, updatedAt = LocalDateTime.now())
        val savedPlan = workoutPlanRepository.save(updatedPlan)
        
        val generatedWorkout = objectMapper.readValue(savedPlan.generatedContent, GeneratedWorkout::class.java)
        return convertToResponse(savedPlan, generatedWorkout)
    }
    
    fun getUserWorkouts(userId: Long, savedOnly: Boolean = false): List<WorkoutPlanResponse> {
        val workoutPlans = if (savedOnly) {
            workoutPlanRepository.findByUserIdAndIsSavedTrueOrderByCreatedAtDesc(userId)
        } else {
            workoutPlanRepository.findByUserIdOrderByCreatedAtDesc(userId)
        }
        
        return workoutPlans.map { plan ->
            val generatedWorkout = objectMapper.readValue(plan.generatedContent, GeneratedWorkout::class.java)
            convertToResponse(plan, generatedWorkout)
        }
    }
    
    fun getWorkoutById(userId: Long, workoutPlanId: Long): WorkoutPlanResponse {
        val workoutPlan = workoutPlanRepository.findById(workoutPlanId)
            .orElseThrow { WorkoutNotFoundException("Workout plan not found") }
        
        if (workoutPlan.userId != userId) {
            throw SecurityException("Cannot access workout plan belonging to another user")
        }
        
        val generatedWorkout = objectMapper.readValue(workoutPlan.generatedContent, GeneratedWorkout::class.java)
        return convertToResponse(workoutPlan, generatedWorkout)
    }
    
    @Transactional
    fun saveUserWorkoutPreferences(userId: Long, request: UserWorkoutPreferencesRequest): UserWorkoutPreferences {
        val existing = userWorkoutPreferencesRepository.findByUserId(userId)
        
        return if (existing.isPresent) {
            val updated = existing.get().copy(
                availableEquipment = request.availableEquipment?.toTypedArray(),
                preferredDuration = request.preferredDuration ?: existing.get().preferredDuration,
                experienceLevel = request.experienceLevel ?: existing.get().experienceLevel,
                trainingGoals = request.trainingGoals?.toTypedArray(),
                updatedAt = LocalDateTime.now()
            )
            userWorkoutPreferencesRepository.save(updated)
        } else {
            val newPrefs = UserWorkoutPreferences(
                userId = userId,
                availableEquipment = request.availableEquipment?.toTypedArray(),
                preferredDuration = request.preferredDuration ?: 45,
                experienceLevel = request.experienceLevel ?: "intermediate",
                trainingGoals = request.trainingGoals?.toTypedArray()
            )
            userWorkoutPreferencesRepository.save(newPrefs)
        }
    }
    
    fun getUserWorkoutPreferences(userId: Long): UserWorkoutPreferences? {
        return userWorkoutPreferencesRepository.findByUserId(userId).orElse(null)
    }
    
    private fun convertToResponse(plan: WorkoutPlan, workout: GeneratedWorkout): WorkoutPlanResponse {
        return WorkoutPlanResponse(
            id = plan.id!!,
            sport = plan.sport,
            position = plan.position,
            workoutName = plan.workoutName,
            durationMinutes = plan.durationMinutes,
            difficultyLevel = plan.difficultyLevel,
            equipmentNeeded = plan.equipmentNeeded.toList(),
            positionFocus = plan.positionFocus,
            workout = workout,
            isSaved = plan.isSaved,
            createdAt = plan.createdAt
        )
    }
}