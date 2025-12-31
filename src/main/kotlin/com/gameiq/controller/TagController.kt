package com.gameiq.controller

import com.gameiq.entity.Tag
import com.gameiq.service.TagService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/users/{userId}/tags")
class TagController @Autowired constructor(
    private val tagService: TagService
) {
    
    @GetMapping
    fun getUserTags(@PathVariable userId: Long): ResponseEntity<List<TagResponse>> {
        val tags = tagService.getUserTags(userId)
        val tagResponses = tags.map { tag ->
            TagResponse(
                id = tag.id,
                name = tag.name,
                color = tag.color,
                createdAt = tag.createdAt.toString()
            )
        }
        return ResponseEntity.ok(tagResponses)
    }
    
    @PostMapping
    fun createTag(
        @PathVariable userId: Long,
        @RequestBody request: CreateTagRequest
    ): ResponseEntity<TagResponse> {
        val tag = tagService.createTag(userId, request.name, request.color)
        val response = TagResponse(
            id = tag.id,
            name = tag.name,
            color = tag.color,
            createdAt = tag.createdAt.toString()
        )
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
    
    @PutMapping("/{tagId}")
    fun updateTag(
        @PathVariable userId: Long,
        @PathVariable tagId: Long,
        @RequestBody request: UpdateTagRequest
    ): ResponseEntity<TagResponse> {
        val tag = tagService.updateTag(userId, tagId, request.name, request.color)
        val response = TagResponse(
            id = tag.id,
            name = tag.name,
            color = tag.color,
            createdAt = tag.createdAt.toString()
        )
        return ResponseEntity.ok(response)
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
        val tagResponses = tags.map { tag ->
            TagResponse(
                id = tag.id,
                name = tag.name,
                color = tag.color,
                createdAt = tag.createdAt.toString()
            )
        }
        return ResponseEntity.ok(tagResponses)
    }
}

// Data classes for tag requests and responses
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