package com.athleteperformance.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

// Data classes for API responses
data class Video(
    val id: String,
    val youtubeUrl: String,
    val youtubeId: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val sport: String,
    val category: String,
    val isFeatured: Boolean,
    val displayOrder: Int,
    val tags: List<String>?,
    val isPublic: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class CreateVideoRequest(
    val youtubeUrl: String,
    val title: String?,
    val description: String?,
    val sport: String,
    val category: String,
    val datePerformed: String?,
    val tags: List<String>?,
    val isPublic: Boolean = true
)

data class HealthStatus(
    val status: String,
    val database: String = "connected"
)

@RestController
@RequestMapping
class VideoController {

    @GetMapping("/actuator/health")
    fun healthCheck(): ResponseEntity<HealthStatus> {
        return ResponseEntity.ok(HealthStatus("UP"))
    }

    @GetMapping("/api/videos/sports")
    fun getSports(): ResponseEntity<List<String>> {
        val sports = listOf(
            "FOOTBALL", "BASKETBALL", "BASEBALL", "SOCCER", "TENNIS",
            "TRACK_FIELD", "SWIMMING", "WRESTLING", "VOLLEYBALL", "SOFTBALL", 
            "LACROSSE", "HOCKEY", "GOLF", "CROSS_COUNTRY", "GYMNASTICS", "OTHER"
        )
        return ResponseEntity.ok(sports)
    }

    @GetMapping("/api/videos/categories")
    fun getCategories(): ResponseEntity<List<String>> {
        val categories = listOf(
            "GAME_HIGHLIGHTS", "TRAINING", "SKILLS_DEMO", "DRILLS",
            "STRENGTH_TRAINING", "SPEED_AGILITY", "TECHNIQUE", "SCRIMMAGE"
        )
        return ResponseEntity.ok(categories)
    }

    @GetMapping("/api/videos/my-videos")
    fun getMyVideos(): ResponseEntity<List<Video>> {
        // Mock data for now - in production this would come from database
        val videos = listOf(
            Video(
                id = "123e4567-e89b-12d3-a456-426614174000",
                youtubeUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
                youtubeId = "dQw4w9WgXcQ",
                title = "Football Highlights - Championship Game",
                description = "Amazing touchdown pass in the 4th quarter that secured our victory",
                thumbnailUrl = "https://img.youtube.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                sport = "FOOTBALL",
                category = "GAME_HIGHLIGHTS",
                isFeatured = true,
                displayOrder = 0,
                tags = listOf("touchdown", "quarterback", "championship"),
                isPublic = true,
                createdAt = LocalDateTime.of(2024, 10, 3, 20, 30, 0),
                updatedAt = LocalDateTime.of(2024, 10, 3, 20, 30, 0)
            ),
            Video(
                id = "223e4567-e89b-12d3-a456-426614174001", 
                youtubeUrl = "https://www.youtube.com/watch?v=9bZkp7q19f0",
                youtubeId = "9bZkp7q19f0",
                title = "Speed and Agility Training Session",
                description = "40-yard dash practice and cone drills to improve my footwork",
                thumbnailUrl = "https://img.youtube.com/vi/9bZkp7q19f0/maxresdefault.jpg",
                sport = "FOOTBALL",
                category = "TRAINING",
                isFeatured = false,
                displayOrder = 1,
                tags = listOf("speed", "agility", "training"),
                isPublic = true,
                createdAt = LocalDateTime.of(2024, 10, 2, 15, 20, 0),
                updatedAt = LocalDateTime.of(2024, 10, 2, 15, 20, 0)
            ),
            Video(
                id = "323e4567-e89b-12d3-a456-426614174002",
                youtubeUrl = "https://www.youtube.com/watch?v=jNQXAC9IVRw",
                youtubeId = "jNQXAC9IVRw", 
                title = "Route Running Drills and Technique",
                description = "Working on precision routes and catching techniques",
                thumbnailUrl = "https://img.youtube.com/vi/jNQXAC9IVRw/maxresdefault.jpg",
                sport = "FOOTBALL",
                category = "SKILLS_DEMO",
                isFeatured = false,
                displayOrder = 2,
                tags = listOf("routes", "catching", "technique"),
                isPublic = true,
                createdAt = LocalDateTime.of(2024, 10, 1, 10, 15, 0),
                updatedAt = LocalDateTime.of(2024, 10, 1, 10, 15, 0)
            )
        )
        return ResponseEntity.ok(videos)
    }

    @PostMapping("/api/videos")
    fun addVideo(@RequestBody request: CreateVideoRequest): ResponseEntity<Map<String, Any>> {
        // Extract YouTube ID from URL (simple regex)
        val youtubeId = extractYouTubeId(request.youtubeUrl)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to "Invalid YouTube URL"))

        // In production, this would:
        // 1. Call YouTube Data API to get metadata
        // 2. Save to database
        // 3. Return saved video object
        
        val response = mapOf(
            "id" to "424e4567-e89b-12d3-a456-426614174003",
            "message" to "Video added successfully (mock implementation)",
            "youtubeUrl" to request.youtubeUrl,
            "youtubeId" to youtubeId,
            "title" to (request.title ?: "New Video"),
            "sport" to request.sport,
            "category" to request.category,
            "isPublic" to request.isPublic,
            "createdAt" to LocalDateTime.now()
        )
        
        return ResponseEntity.status(201).body(response)
    }

    @PutMapping("/api/videos/{videoId}")
    fun updateVideo(
        @PathVariable videoId: String,
        @RequestBody request: Map<String, Any>
    ): ResponseEntity<Map<String, Any>> {
        // Mock implementation
        val response = mapOf(
            "id" to videoId,
            "message" to "Video updated successfully (mock implementation)",
            "updatedAt" to LocalDateTime.now()
        )
        return ResponseEntity.ok(response)
    }

    @DeleteMapping("/api/videos/{videoId}")
    fun deleteVideo(@PathVariable videoId: String): ResponseEntity<Map<String, Any>> {
        // Mock implementation
        val response = mapOf(
            "message" to "Video deleted successfully (mock implementation)",
            "videoId" to videoId
        )
        return ResponseEntity.ok(response)
    }

    @PutMapping("/api/videos/{videoId}/featured")
    fun setFeaturedVideo(@PathVariable videoId: String): ResponseEntity<Map<String, Any>> {
        // Mock implementation
        val response = mapOf(
            "message" to "Video set as featured (mock implementation)",
            "videoId" to videoId
        )
        return ResponseEntity.ok(response)
    }

    @GetMapping("/api/videos/discover")
    fun discoverVideos(
        @RequestParam sport: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<List<Video>> {
        // Mock implementation - return empty list for now
        return ResponseEntity.ok(emptyList())
    }

    private fun extractYouTubeId(url: String): String? {
        val patterns = listOf(
            Regex("(?:https?://)?(?:www\\.)?youtube\\.com/watch\\?v=([a-zA-Z0-9_-]{11})"),
            Regex("(?:https?://)?(?:www\\.)?youtu\\.be/([a-zA-Z0-9_-]{11})"),
            Regex("(?:https?://)?(?:www\\.)?youtube\\.com/embed/([a-zA-Z0-9_-]{11})")
        )
        
        return patterns.firstNotNullOfOrNull { pattern ->
            pattern.find(url)?.groups?.get(1)?.value
        }
    }
}