package com.musicroom.server.common.exceptions

import io.ktor.http.HttpStatusCode

open class HttpException(
    val status: HttpStatusCode,
    val error: String,
    message: String? = null,
) : RuntimeException(message)

class BadRequestException(message: String? = null) :
    HttpException(HttpStatusCode.BadRequest, "bad_request", message)

class UnauthorizedException(message: String? = null) :
    HttpException(HttpStatusCode.Unauthorized, "unauthorized", message)

class ForbiddenException(message: String? = null) :
    HttpException(HttpStatusCode.Forbidden, "forbidden", message)

class NotFoundException(message: String? = null) :
    HttpException(HttpStatusCode.NotFound, "not_found", message)

class ConflictException(message: String? = null) :
    HttpException(HttpStatusCode.Conflict, "conflict", message)
