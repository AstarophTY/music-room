package com.musicroom.server.modules.users

import com.musicroom.server.common.exceptions.ValidationException
import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.modules.users.entities.User
import com.musicroom.shared.AuthValidation
import com.musicroom.shared.dto.UpdateProfileRequest
import java.util.UUID

class UsersService(private val repository: UsersRepository) {

    suspend fun getById(id: UUID): User =
        repository.findById(id) ?: throw NotFoundException("User not found")

    suspend fun updateProfile(id: UUID, request: UpdateProfileRequest): User {
        val displayName = request.displayName.trim()
        AuthValidation.displayNameError(displayName)?.let { throw ValidationException(it) }
        return repository.updateDisplayName(id, displayName) ?: throw NotFoundException("User not found")
    }
}
