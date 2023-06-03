package de.fiereu

import de.fiereu.pokemmo.headless.game.ChatType
import de.fiereu.pokemmo.headless.game.Language

data class AccountConfig(
    val username: String,
    val password: String,
    val hwid: String,
    private val mac: String,
) {
    fun getMacAddress(): ByteArray {
        return mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
    }
}

data class WebhookConfig(
    val url: String,
    val languages: List<Language>,
    val chats: List<ChatType>,
    val keywords: List<String>,
)

data class ItemScraperConfig(
    val account: AccountConfig,
    val webhooks: List<WebhookConfig>,
)