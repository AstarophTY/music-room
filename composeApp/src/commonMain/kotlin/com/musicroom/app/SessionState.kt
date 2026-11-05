package com.musicroom.app

import com.musicroom.app.common.UiText
import com.musicroom.shared.dto.UserResponse

sealed interface SessionState {
    data object Loading : SessionState

    data class LoggedOut(val message: UiText? = null) : SessionState

    data class LoggedIn(val user: UserResponse) : SessionState
}
