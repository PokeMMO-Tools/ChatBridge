package de.fiereu

import com.sksamuel.hoplite.ConfigLoaderBuilder
import de.fiereu.packets.CustomReceiveMessagePacket
import de.fiereu.pokemmo.headless.Client
import de.fiereu.pokemmo.headless.config.ClientConfig
import de.fiereu.pokemmo.headless.game.Account
import de.fiereu.pokemmo.headless.network.packets.ChatPacketFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory

fun getPokeMMOVersion(): Int {
  val url = "https://dl.pokemmo.com/feeds/main_feed.txt"
  val request = Request.Builder().url(url).build()
  val response = OkHttpClient().newCall(request).execute()
  if (!response.isSuccessful) return -1
  val body = response.body?.string() ?: return -1
  val regex = Regex("<min_revision>(\\d+)</min_revision>")
  val match = regex.find(body) ?: return -1
  return match.groupValues[1].toInt()
}

fun main() {
  val logger = LoggerFactory.getLogger("ChatBridge")

  val config =
      ConfigLoaderBuilder.default().build().loadConfigOrThrow<ItemScraperConfig>("/config.json")

  val version = getPokeMMOVersion()

  if (version == -1) {
    logger.error("Failed to get PokeMMO version")
    return
  }

  val client =
      Client(
          ClientConfig(
              Account(
                  config.account.username,
                  config.account.password,
                  config.account.hwid.toByteArray(charset = Charsets.UTF_8),
              ),
              version = version,
              macAddress = config.account.getMacAddress()))
  CustomReceiveMessagePacket.webHooks = config.webhooks
  ChatPacketFactory.registerPacket(0x02, CustomReceiveMessagePacket::class)
  client.login()

  logger.info("ChatBridge started")
}
