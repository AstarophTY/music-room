package com.musicroom.server.modules.auth.entities

/** What an emailed code is for. [key] is stored in the `purpose` column. */
enum class EmailCodePurpose(val key: String) {
    VERIFY_EMAIL("verify_email"),
    RESET_PASSWORD("reset_password"),
}
