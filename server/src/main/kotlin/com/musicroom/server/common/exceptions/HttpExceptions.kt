package com.musicroom.server.common.exceptions

import io.ktor.http.HttpStatusCode

open class HttpException(
    val status: HttpStatusCode,
    val error: String,
    message: String? = null,
) : RuntimeException(message)

class BadRequestException(message: String? = null, error: String = "bad_request") :
    HttpException(HttpStatusCode.BadRequest, error, message)

class UnauthorizedException(message: String? = null, error: String = "unauthorized") :
    HttpException(HttpStatusCode.Unauthorized, error, message)

class ForbiddenException(message: String? = null, error: String = "forbidden") :
    HttpException(HttpStatusCode.Forbidden, error, message)

class NotFoundException(message: String? = null, error: String = "not_found") :
    HttpException(HttpStatusCode.NotFound, error, message)

class ConflictException(message: String? = null, error: String = "conflict") :
    HttpException(HttpStatusCode.Conflict, error, message)
