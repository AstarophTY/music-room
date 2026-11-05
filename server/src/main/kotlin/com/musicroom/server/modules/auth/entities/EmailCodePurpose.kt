package com.musicroom.server.modules.auth.entities

/** What an emailed code is for. [key] is stored in the `purpose` column. */
enum class EmailCodePurpose(val key: String) {
    VERIFY_EMAIL("verify_email"),
    RESET_PASSWORD("reset_password"),

    /** Email 2FA: login codes, and the codes that turn it on or off. */
    TWO_FACTOR("two_factor"),
}
