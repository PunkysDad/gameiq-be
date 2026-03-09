package com.gameiq.controller

import com.gameiq.entity.*
import com.gameiq.service.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.slf4j.LoggerFactory

@RestController
@RequestMapping("/workouts")
class WorkoutController @Autowired constructor(
    private val claudeService: ClaudeService,
    private val workoutService: WorkoutService
) {

    private val logger = LoggerFactory.getLogger(WorkoutController::class.java)
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    @GetMapping("/test")
    fun testWorkouts(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(mapOf(
            "message" to "Workout controller is working",
            "availableSports" to listOf("FOOTBALL", "BASKETBALL", "BASEBALL", "SOCCER"),
            "availablePositions" to listOf("QB", "PG", "PITCHER", "GOALKEEPER")
        ))
    }

    @PostMapping("/generate")
    fun generateWorkout(
        @RequestBody request: WorkoutGenerationRequest
    ): ResponseEntity<WorkoutPlanDTO> {
        return try {
            val workoutPlan = claudeService.generateWorkoutPlan(
                userId = request.userId,
                sport = request.sport,
                position = request.position,
                experienceLevel = request.experienceLevel,
                trainingPhase = request.trainingPhase,
                availableEquipment = request.availableEquipment.joinToString(", "),
                sessionDuration = request.sessionDuration,
                focusAreas = request.focusAreas.joinToString(", "),
                specialRequirements = request.specialRequirements
            )

            val exercises = parseExercisesFromGeneratedContent(workoutPlan.generatedContent)

            val workoutPlanDTO = WorkoutPlanDTO(
                id = workoutPlan.id.toString(),
                title = workoutPlan.workoutName ?: "${request.position} ${request.trainingPhase} Workout",
                description = workoutPlan.positionFocus ?: "",
                estimatedDuration = workoutPlan.durationMinutes ?: request.sessionDuration,
                exercises = exercises,
                focusAreas = request.focusAreas,
                createdAt = workoutPlan.createdAt.toString(),
                sport = request.sport,
                position = request.position,
                generatedContent = workoutPlan.generatedContent
            )

            ResponseEntity.ok(workoutPlanDTO)

        } catch (e: Exception) {
            logger.error("Failed to generate workout for userId=${request.userId}: ${e.message}", e)
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/user/{userId}")
    fun getUserWorkouts(@PathVariable userId: Long): ResponseEntity<List<WorkoutPlanDTO>> {
        return try {
            val workoutPlans = workoutService.getUserWorkoutPlans(userId)

            val workoutDTOs = workoutPlans.map { workoutPlan ->
                WorkoutPlanDTO(
                    id = workoutPlan.id.toString(),
                    title = workoutPlan.workoutName ?: "Workout Plan",
                    description = workoutPlan.positionFocus ?: "",
                    estimatedDuration = workoutPlan.durationMinutes ?: 45,
                    exercises = emptyList(),
                    focusAreas = listOf(workoutPlan.positionFocus ?: "General Training"),
                    createdAt = workoutPlan.createdAt.toString(),
                    sport = workoutPlan.sport.name,
                    position = workoutPlan.position.name,
                    generatedContent = workoutPlan.generatedContent
                )
            }

            ResponseEntity.ok(workoutDTOs)
        } catch (e: Exception) {
            logger.error("Failed to get workouts for userId=$userId: ${e.message}", e)
            ResponseEntity.badRequest().build()
        }
    }

    @GetMapping("/{workoutId}")
    fun getWorkoutById(@PathVariable workoutId: Long): ResponseEntity<WorkoutPlan?> {
        val workout = workoutService.getWorkoutPlanById(workoutId)
        return ResponseEntity.ok(workout)
    }

    private fun parseExercisesFromGeneratedContent(generatedContent: String?): List<ExerciseDTO> {
        if (generatedContent.isNullOrEmpty()) return emptyList()

        return try {
            val workoutData = objectMapper.readTree(generatedContent)
            val exercisesNode = workoutData.get("exercises") ?: workoutData.get("mainExercises")

            if (exercisesNode?.isArray == true) {
                exercisesNode.map { exerciseNode ->
                    ExerciseDTO(
                        name = exerciseNode.get("name")?.asText() ?: "Exercise",
                        description = exerciseNode.get("description")?.asText() ?: "",
                        sets = exerciseNode.get("sets")?.asInt() ?: 3,
                        reps = exerciseNode.get("reps")?.asText() ?: "8-12",
                        duration = exerciseNode.get("duration")?.asText(),
                        restPeriod = exerciseNode.get("restSeconds")?.asInt()?.let { "${it} seconds" },
                        instructions = exerciseNode.get("instructions")?.map { it.asText() },
                        videoUrl = exerciseNode.get("videoUrl")?.asText()
                    )
                }
            } else {
                parseExercisesFromMarkdown(generatedContent)
            }
        } catch (e: Exception) {
            parseExercisesFromMarkdown(generatedContent)
        }
    }

    private fun parseExercisesFromMarkdown(content: String): List<ExerciseDTO> {
        val exercises = mutableListOf<ExerciseDTO>()

        try {
            val exercisePattern = Regex("""\*\*(\d+\.\s+.*?)\*\*""")
            val matches = exercisePattern.findAll(content)

            matches.forEach { match ->
                val exerciseText = match.groupValues[1]
                val name = exerciseText.split("**").firstOrNull()?.trim() ?: "Exercise"

                val setsPattern = Regex("""Sets:\s*(\d+)""", RegexOption.IGNORE_CASE)
                val repsPattern = Regex("""Reps:\s*([^|]+)""", RegexOption.IGNORE_CASE)
                val restPattern = Regex("""Rest:\s*([^|]+)""", RegexOption.IGNORE_CASE)

                val sets = setsPattern.find(content)?.groupValues?.get(1)?.toIntOrNull() ?: 3
                val reps = repsPattern.find(content)?.groupValues?.get(1)?.trim() ?: "8-12"
                val rest = restPattern.find(content)?.groupValues?.get(1)?.trim()

                exercises.add(ExerciseDTO(
                    name = name,
                    description = "Position-specific exercise",
                    sets = sets,
                    reps = reps,
                    restPeriod = rest,
                    instructions = null,
                    videoUrl = null
                ))
            }
        } catch (e: Exception) {
            exercises.add(ExerciseDTO(
                name = "AI-Generated Workout",
                description = "Complete workout available in enhanced content",
                sets = 3,
                reps = "As prescribed",
                instructions = listOf("Follow the detailed workout instructions provided by Claude AI")
            ))
        }

        return exercises
    }
}