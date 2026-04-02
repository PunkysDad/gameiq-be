package com.gameiq.service

import com.gameiq.entity.*
import com.gameiq.repository.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.*
import java.time.LocalDateTime
import java.util.*

class UserServiceTest {

    private val userRepository: UserRepository = mock()
    private val quizService: QuizService = mock()
    private val quizSessionRepository: QuizSessionRepository = mock()
    private val quizSessionAttemptRepository: QuizSessionAttemptRepository = mock()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private lateinit var service: UserService

    private fun makeUser(
        id: Long = 1L,
        tier: SubscriptionTier = SubscriptionTier.TRIAL,
        firebaseUid: String = "test-uid"
    ) = User(
        id = id,
        email = "test@test.com",
        firebaseUid = firebaseUid,
        displayName = "Test User",
        primarySport = Sport.FOOTBALL,
        primaryPosition = Position.QB,
        subscriptionTier = tier,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )

    @BeforeEach
    fun setUp() {
        service = UserService(
            userRepository = userRepository,
            quizService = quizService,
            quizSessionRepository = quizSessionRepository,
            quizSessionAttemptRepository = quizSessionAttemptRepository,
            objectMapper = objectMapper
        )
    }

    // =========================================================================
    // Soft Delete Tests
    // =========================================================================

    @Nested
    inner class SoftDeleteTests {

        @Test
        fun `deleteUser sets deletedAt timestamp instead of removing record`() {
            val user = makeUser(tier = SubscriptionTier.BASIC)
            whenever(userRepository.findById(1L)).thenReturn(Optional.of(user))
            val captor = argumentCaptor<User>()
            whenever(userRepository.save(captor.capture())).thenAnswer { captor.lastValue }

            service.deleteUser(1L)

            assertNotNull(captor.firstValue.deletedAt)
            verify(userRepository, never()).deleteById(any())
        }

        @Test
        fun `deleteUser throws when user not found`() {
            whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

            assertThrows<IllegalArgumentException> {
                service.deleteUser(99L)
            }
        }

        @Test
        fun `findByFirebaseUid reactivates soft-deleted account on sign-in`() {
            val deletedUser = makeUser(tier = SubscriptionTier.TRIAL).copy(
                deletedAt = LocalDateTime.now().minusDays(5)
            )
            whenever(userRepository.findByFirebaseUid("test-uid")).thenReturn(deletedUser)
            val captor = argumentCaptor<User>()
            whenever(userRepository.save(captor.capture())).thenAnswer { captor.lastValue }

            val result = service.findByFirebaseUid("test-uid")

            assertNull(result?.deletedAt)
            assertNull(captor.firstValue.deletedAt)
            assertTrue(captor.firstValue.isActive)
        }

        @Test
        fun `findByFirebaseUid returns active user without modification`() {
            val activeUser = makeUser(tier = SubscriptionTier.BASIC)
            whenever(userRepository.findByFirebaseUid("test-uid")).thenReturn(activeUser)

            val result = service.findByFirebaseUid("test-uid")

            assertNotNull(result)
            assertNull(result?.deletedAt)
            verify(userRepository, never()).save(anyOrNull())
        }

        @Test
        fun `findByFirebaseUid returns null when user does not exist`() {
            whenever(userRepository.findByFirebaseUid("unknown-uid")).thenReturn(null)

            val result = service.findByFirebaseUid("unknown-uid")

            assertNull(result)
            verify(userRepository, never()).save(anyOrNull())
        }

        @Test
        fun `createUser reactivates soft-deleted account instead of creating duplicate`() {
            val deletedUser = makeUser(tier = SubscriptionTier.TRIAL, firebaseUid = "test-firebase-uid").copy(
                deletedAt = LocalDateTime.now().minusDays(3),
                isActive = false
            )
            whenever(userRepository.findByFirebaseUid("test-firebase-uid")).thenReturn(deletedUser)
            val captor = argumentCaptor<User>()
            whenever(userRepository.save(captor.capture())).thenAnswer { captor.lastValue }

            val result = service.createUser(
                email = "test@example.com",
                firebaseUid = "test-firebase-uid",
                displayName = "Test User",
                primarySport = Sport.BASKETBALL,
                primaryPosition = Position.PG
            )

            assertNull(result.deletedAt)
            assertTrue(result.isActive)
            assertEquals(Sport.BASKETBALL, result.primarySport)
            assertEquals(Position.PG, result.primaryPosition)
            // Should update existing record, not save a new one with id=0
            assertNotEquals(0L, captor.firstValue.id)
        }

        @Test
        fun `createUser proceeds normally when no existing user found`() {
            whenever(userRepository.findByFirebaseUid("brand-new-uid")).thenReturn(null)
            val captor = argumentCaptor<User>()
            whenever(userRepository.save(captor.capture())).thenAnswer { captor.lastValue }

            val result = service.createUser(
                email = "new@example.com",
                firebaseUid = "brand-new-uid",
                displayName = "New User",
                primarySport = Sport.FOOTBALL,
                primaryPosition = Position.QB
            )

            assertNull(result.deletedAt)
            assertTrue(result.isActive)
            verify(userRepository).save(anyOrNull())
        }
    }
}
