package com.musicroom.server.modules.users

import com.musicroom.server.common.exceptions.NotFoundException
import com.musicroom.server.modules.users.entities.User
import java.util.UUID

class UsersService(private val repository: UsersRepository) {

    suspend fun getById(id: UUID): User =
        repository.findById(id) ?: throw NotFoundException("User not found")
}
