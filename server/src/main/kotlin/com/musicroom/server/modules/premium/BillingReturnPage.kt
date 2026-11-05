package com.musicroom.server.modules.premium

import com.musicroom.server.common.i18n.Language

/** Small page shown in the browser after the Stripe pages: tells the user to go back to the app. */
object BillingReturnPage {
    fun html(outcome: BillingReturnOutcome, language: Language): String {
        val (title, body) = when (language) {
            Language.FR -> when (outcome) {
                BillingReturnOutcome.Paid -> "Bienvenue dans Premium" to "Tu peux fermer cette page et revenir dans Music Room pour ajouter tes sons."
                BillingReturnOutcome.Pending -> "Paiement en cours" to "Stripe confirme ton paiement. Reviens dans Music Room : Premium s'active dans un instant."
                BillingReturnOutcome.Cancelled -> "Retour à Music Room" to "Tu peux fermer cette page et revenir dans Music Room."
            }
            Language.EN -> when (outcome) {
                BillingReturnOutcome.Paid -> "Welcome to Premium" to "You can close this page and go back to Music Room to add your songs."
                BillingReturnOutcome.Pending -> "Payment in progress" to "Stripe is confirming your payment. Go back to Music Room: Premium turns on in a moment."
                BillingReturnOutcome.Cancelled -> "Back to Music Room" to "You can close this page and go back to Music Room."
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
