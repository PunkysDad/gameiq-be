package com.gameiq.service

import com.gameiq.entity.Tag
import com.gameiq.entity.User
import com.gameiq.repository.TagRepository
import com.gameiq.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
class TagService @Autowired constructor(
    private val tagRepository: TagRepository,
    private val userRepository: UserRepository
) {
    
    /**
     * Get all tags for a user
     */
    fun getUserTags(userId: Long): List<Tag> {
        return tagRepository.findByUserIdOrderByNameAsc(userId)
    }
    
    /**
     * Create a new tag for a user
     */
    fun createTag(userId: Long, name: String, color: String = "#007AFF"): Tag {
        // Validate user exists
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found with id: $userId") }
        
        // Check if tag name already exists for this user (case-insensitive)
        if (tagRepository.existsByUserIdAndNameIgnoreCase(userId, name.trim())) {
            throw IllegalArgumentException("Tag with name '${name.trim()}' already exists for this user")
        }
        
        // Validate input
        val trimmedName = name.trim()
        if (trimmedName.isEmpty() || trimmedName.length > 100) {
            throw IllegalArgumentException("Tag name must be between 1 and 100 characters")
        }
        
        if (!color.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
            throw IllegalArgumentException("Color must be a valid hex color code (e.g., #FF5733)")
        }
        
        // Create and save the tag
        val tag = Tag(
            user = user,
            name = trimmedName,
            color = color.uppercase(),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        
        return tagRepository.save(tag)
    }
    
    /**
     * Update an existing tag
     */
    fun updateTag(userId: Long, tagId: Long, newName: String?, newColor: String?): Tag {
        // Find existing tag and verify ownership
        val tag = tagRepository.findById(tagId)
            .orElseThrow { IllegalArgumentException("Tag not found with id: $tagId") }
        
        if (tag.user.id != userId) {
            throw IllegalArgumentException("Tag does not belong to user $userId")
        }
        
        // Validate and update name if provided
        val updatedName = if (newName != null) {
            val trimmedName = newName.trim()
            if (trimmedName.isEmpty() || trimmedName.length > 100) {
                throw IllegalArgumentException("Tag name must be between 1 and 100 characters")
            }
            
            // Check for name conflicts (excluding current tag)
            val existingTag = tagRepository.findByUserIdAndNameIgnoreCase(userId, trimmedName)
            if (existingTag != null && existingTag.id != tagId) {
                throw IllegalArgumentException("Tag with name '$trimmedName' already exists for this user")
            }
            
            trimmedName
        } else {
            tag.name
        }
        
        // Validate and update color if provided
        val updatedColor = if (newColor != null) {
            if (!newColor.matches(Regex("^#[0-9A-Fa-f]{6}$"))) {
                throw IllegalArgumentException("Color must be a valid hex color code (e.g., #FF5733)")
            }
            newColor.uppercase()
        } else {
            tag.color
        }
        
        // Create updated tag (since data class is immutable)
        val updatedTag = tag.copy(
            name = updatedName,
            color = updatedColor,
            updatedAt = LocalDateTime.now()
        )
        
        return tagRepository.save(updatedTag)
    }
    
    /**
     * Delete a tag
     */
    fun deleteTag(userId: Long, tagId: Long) {
        val tag = tagRepository.findById(tagId)
            .orElseThrow { IllegalArgumentException("Tag not found with id: $tagId") }
        
        if (tag.user.id != userId) {
            throw IllegalArgumentException("Tag does not belong to user $userId")
        }
        
        tagRepository.delete(tag)
    }
    
    /**
     * Search tags by name
     */
    fun searchTags(userId: Long, searchTerm: String): List<Tag> {
        return tagRepository.searchByUserIdAndName(userId, searchTerm.trim())
    }
    
    /**
     * Get tag by ID and verify ownership
     */
    fun getTagById(userId: Long, tagId: Long): Tag? {
        val tag = tagRepository.findById(tagId).orElse(null)
        return if (tag?.user?.id == userId) tag else null
    }
    
    /**
     * Get or create a tag by name
     */
    fun getOrCreateTag(userId: Long, name: String, color: String = "#007AFF"): Tag {
        return tagRepository.findByUserIdAndNameIgnoreCase(userId, name.trim())
            ?: createTag(userId, name, color)
    }
}