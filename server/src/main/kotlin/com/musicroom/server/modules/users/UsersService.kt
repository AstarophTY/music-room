package com.musicroom.server.modules.users

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.ConflictException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.exceptions.ValidationException
import com.musicroom.server.modules.users.entities.User
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.SocialLinks
import com.musicroom.shared.dto.SocialLink
import com.musicroom.shared.dto.UpdateProfileRequest
import com.musicroom.shared.dto.UpdateSocialLinksRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.time.OffsetDateTime
import java.util.UUID

class UsersService(private val repository: UsersRepository, private val avatars: AvatarsRepository) {

    suspend fun getById(id: UUID): User =
        repository.findById(id) ?: throw NotFoundException("User not found")

    suspend fun updateProfile(id: UUID, request: UpdateProfileRequest): User {
        val displayName = request.displayName?.trim()
        displayName?.let { AuthValidation.displayNameError(it)?.let { error -> throw ValidationException(error) } }
        if (displayName != null && repository.isDisplayNameTaken(displayName, excluding = id)) throw displayNameTaken()
        return try {
            repository.updateProfile(id, displayName, request.avatarColor) ?: throw NotFoundException("User not found")
        } catch (e: ExposedSQLException) {
            // Someone took the name between the check and the update.
            if (e.sqlState == UNIQUE_VIOLATION) throw displayNameTaken() else throw e
        }
    }

    /** Cleans each username (accepts "@name" or a profile link), checks its format, keeps one per network. */
    suspend fun updateSocialLinks(id: UUID, request: UpdateSocialLinksRequest): User {
        val links = request.links
            .map { SocialLink(it.network, SocialLinks.normalize(it.network, it.handle)) }
            .filter { it.handle.isNotEmpty() }
            .distinctBy { it.network }
        links.forEach { link ->
            SocialLinks.handleError(link.network, link.handle)?.let {
                throw BadRequestException("${link.network.displayName}: ${it.message}", it.code)
            }
        }
        return repository.replaceSocialLinks(id, links) ?: throw NotFoundException("User not found")
    }

    suspend fun uploadAvatar(id: UUID, bytes: ByteArray): User {
        if (bytes.size > AvatarImages.MAX_UPLOAD_BYTES) throw imageTooLarge()
        val jpeg = withContext(Dispatchers.Default) { AvatarImages.toAvatarJpeg(bytes) }
            ?: throw BadRequestException("This file is not a supported image (JPEG or PNG)", ApiErrors.INVALID_IMAGE)
        avatars.save(id, jpeg)
        return repository.setAvatarUpdatedAt(id, OffsetDateTime.now()) ?: throw NotFoundException("User not found")
    }

    suspend fun deleteAvatar(id: UUID): User {
        avatars.delete(id)
        return repository.setAvatarUpdatedAt(id, null) ?: throw NotFoundException("User not found")
    }

    suspend fun avatar(id: UUID): ByteArray = avatars.find(id) ?: throw NotFoundException("No profile photo")

    companion object {
        private const val UNIQUE_VIOLATION = "23505"

        private fun displayNameTaken() = ConflictException("This name is already taken", ApiErrors.DISPLAY_NAME_TAKEN)

        fun imageTooLarge() = BadRequestException("The image is too large (5 MB max)", ApiErrors.IMAGE_TOO_LARGE)
    }
}
