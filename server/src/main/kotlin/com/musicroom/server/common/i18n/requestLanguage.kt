package com.musicroom.server.common.i18n

import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

/** Language of the client that made this request (from its Accept-Language header). */
fun ApplicationCall.requestLanguage(): Language = Language.fromAcceptLanguage(request.headers[HttpHeaders.AcceptLanguage])
