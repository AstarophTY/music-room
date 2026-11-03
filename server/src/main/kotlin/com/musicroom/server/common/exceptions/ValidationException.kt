package com.musicroom.server.common.exceptions

import com.musicroom.shared.ValidationError
import io.ktor.http.HttpStatusCode

/** 400 with the validation code as `error`, so the app can translate it. */
class ValidationException(validationError: ValidationError) :
    HttpException(HttpStatusCode.BadRequest, validationError.code, validationError.message)
