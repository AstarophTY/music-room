package com.musicroom.server.modules.users

import com.musicroom.server.common.exceptions.BadRequestException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.common.exceptions.ValidationException
import com.musicroom.server.modules.users.entities.User
import com.musicroom.shared.ApiErrors
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.dto.UpdateProfileRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.OffsetDateTime
import java.util.UUID

class UsersService(private val repository: UsersRepository, private val avatars: AvatarsRepository) {

    suspend fun getById(id: UUID): User =
        repository.findById(id) ?: throw NotFoundException("User not found")

    suspend fun updateProfile(id: UUID, request: UpdateProfileRequest): User {
        val displayName = request.displayName?.trim()
        displayName?.let { AuthValidation.displayNameError(it)?.let { error -> throw ValidationException(error) } }
        return repository.updateProfile(id, displayName, request.avatarColor) ?: throw NotFoundException("User not found")
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
        fun imageTooLarge() = BadRequestException("The image is too large (5 MB max)", ApiErrors.IMAGE_TOO_LARGE)
    }
}
