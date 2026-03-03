package com.gameiq.controller

import com.gameiq.entity.Tag
import com.gameiq.service.TagService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/users/{userId}/tags")
class TagController @Autowired constructor(
    private val tagService: TagService
) {

    @GetMapping
    fun getUserTags(@PathVariable userId: Long): ResponseEntity<List<TagResponse>> {
        val tags = tagService.getUserTags(userId)
        return ResponseEntity.ok(tags.map { it.toResponse() })
    }

    @PostMapping
    fun createTag(
        @PathVariable userId: Long,
        @RequestBody request: CreateTagRequest
    ): ResponseEntity<TagResponse> {
        val tag = tagService.createTag(userId, request.name, request.color)
        return ResponseEntity.status(HttpStatus.CREATED).body(tag.toResponse())
    }

    @PutMapping("/{tagId}")
    fun updateTag(
        @PathVariable userId: Long,
        @PathVariable tagId: Long,
        @RequestBody request: UpdateTagRequest
    ): ResponseEntity<TagResponse> {
        val tag = tagService.updateTag(userId, tagId, request.name, request.color)
        return ResponseEntity.ok(tag.toResponse())
    }

    @DeleteMapping("/{tagId}")
    fun deleteTag(
        @PathVariable userId: Long,
        @PathVariable tagId: Long
    ): ResponseEntity<Map<String, String>> {
        tagService.deleteTag(userId, tagId)
        return ResponseEntity.ok(mapOf("message" to "Tag deleted successfully"))
    }

    @GetMapping("/search")
    fun searchTags(
        @PathVariable userId: Long,
        @RequestParam query: String
    ): ResponseEntity<List<TagResponse>> {
        val tags = tagService.searchTags(userId, query)
        return ResponseEntity.ok(tags.map { it.toResponse() })
    }

    // -------------------------------------------------------------------------
    // Tagged content endpoints — used by the Tags screen in the mobile app
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/users/{userId}/tags/{tagId}/conversations
     * Returns all user_conversations associated with a specific tag.
     */
    @GetMapping("/{tagId}/conversations")
    fun getConversationsByTag(
        @PathVariable userId: Long,
        @PathVariable tagId: Long
    ): ResponseEntity<List<TaggedConversationResponse>> {
        // Verify tag ownership before returning data
        tagService.getTagById(userId, tagId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val conversations = tagService.getConversationsByTag(userId, tagId)
        return ResponseEntity.ok(conversations)
    }

    /**
     * GET /api/v1/users/{userId}/tags/{tagId}/workouts
     * Returns all workout_plans associated with a specific tag.
     */
    @GetMapping("/{tagId}/workouts")
    fun getWorkoutsByTag(
        @PathVariable userId: Long,
        @PathVariable tagId: Long
    ): ResponseEntity<List<TaggedWorkoutResponse>> {
        tagService.getTagById(userId, tagId)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).build()

        val workouts = tagService.getWorkoutsByTag(userId, tagId)
        return ResponseEntity.ok(workouts)
    }
}

// -------------------------------------------------------------------------
// Data classes
// -------------------------------------------------------------------------

data class CreateTagRequest(
    val name: String,
    val color: String = "#007AFF"
)

data class UpdateTagRequest(
    val name: String? = null,
    val color: String? = null
)

data class TagResponse(
    val id: Long,
    val name: String,
    val color: String,
    val createdAt: String
)

data class TaggedConversationResponse(
    val id: Long,
    val title: String?,
    val conversationType: String?,
    val createdAt: String
)

data class TaggedWorkoutResponse(
    val id: Long,
    val title: String?,
    val sport: String?,
    val position: String?,
    val createdAt: String
)

// Extension to keep mapping DRY
private fun Tag.toResponse() = TagResponse(
    id = this.id,
    name = this.name,
    color = this.color,
    createdAt = this.createdAt.toString()
)