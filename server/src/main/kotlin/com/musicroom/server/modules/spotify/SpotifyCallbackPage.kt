package com.musicroom.server.modules.spotify

import com.musicroom.server.common.i18n.Language

/** Small page shown in the browser after the Spotify login: tells the user to go back to the app. */
object SpotifyCallbackPage {
    fun html(outcome: SpotifyCallbackOutcome, language: Language): String {
        val (title, body) = when (language) {
            Language.FR -> when (outcome) {
                SpotifyCallbackOutcome.Connected -> "Spotify est connecté" to "Tu peux fermer cette page et revenir dans Music Room."
                SpotifyCallbackOutcome.Denied -> "Connexion annulée" to "Spotify n'a pas été connecté. Reviens dans Music Room pour réessayer."
                SpotifyCallbackOutcome.Expired -> "Lien expiré" to "Recommence la connexion depuis Music Room."
                SpotifyCallbackOutcome.NotAllowed -> "Compte non autorisé" to
                    "L'app Spotify est en mode développement : ajoute ce compte dans « User Management » sur developer.spotify.com."
                SpotifyCallbackOutcome.Failed -> "La connexion a échoué" to "Reviens dans Music Room et réessaie."
            }
            Language.EN -> when (outcome) {
                SpotifyCallbackOutcome.Connected -> "Spotify is connected" to "You can close this page and go back to Music Room."
                SpotifyCallbackOutcome.Denied -> "Connection cancelled" to "Spotify was not connected. Go back to Music Room to try again."
                SpotifyCallbackOutcome.Expired -> "Link expired" to "Start again from Music Room."
                SpotifyCallbackOutcome.NotAllowed -> "Account not allowed" to
                    "The Spotify app is in development mode: add this account in \"User Management\" on developer.spotify.com."
                SpotifyCallbackOutcome.Failed -> "Connection failed" to "Go back to Music Room and try again."
            }
        }
        return """
            |<!doctype html>
            |<html lang="${language.name.lowercase()}">
            |<head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1"><title>Music Room</title>
            |<style>body{font-family:system-ui,sans-serif;max-width:32rem;margin:15vh auto;padding:0 1.5rem;line-height:1.5}</style></head>
            |<body><h1>$title</h1><p>$body</p></body>
            |</html>
        """.trimMargin()
    }
}
