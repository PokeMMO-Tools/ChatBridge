package de.fiereu.packets

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import de.fiereu.WebHook
import de.fiereu.WebhookConfig
import de.fiereu.pokemmo.headless.network.packets.chat.deserializable.ReceiveMessagePacket
import de.fiereu.pokemmo.headless.network.server.AbstractServer

class CustomReceiveMessagePacket(id: Byte) : ReceiveMessagePacket(id) {

  companion object {
    var webHooks: List<WebhookConfig>? = null
    val monsterNames: HashMap<Int, String> = HashMap()
    val itemNames: HashMap<Int, String> = HashMap()
    init {
      val monsterJson: JsonArray =
          JsonParser.parseReader(
                  {}.javaClass.getResourceAsStream("/json/monster.json")?.reader()
                      ?: throw Exception("Couldn't read /json/monster.json"))
              .asJsonArray
      val itemJson: JsonArray =
          JsonParser.parseReader(
                  {}.javaClass.getResourceAsStream("/json/items.json")?.reader()
                      ?: throw Exception("Couldn't read /json/items.json"))
              .asJsonArray

      for (jsonElement in monsterJson) {
        val monster = jsonElement.asJsonObject
        val id = monster.get("id").asInt
        val name = monster.get("name").asString
        monsterNames[id] = name
      }

      for (jsonElement in itemJson) {
        val item = jsonElement.asJsonObject
        val id = item.get("id").asInt
        val name = item.get("name").asString
        itemNames[id] = name
      }
    }
  }

  override fun handle(server: AbstractServer) {
    super.handle(server)
    if (webHooks == null) return
    val message = formatMessage()
    for (webhook in webHooks!!) {
      if (webhook.chats.isNotEmpty() && !webhook.chats.contains(super.chatType)) continue
      if (webhook.languages.isNotEmpty() && !webhook.languages.contains(super.language)) continue
      if (webhook.keywords.isNotEmpty() && !webhook.keywords.any { message.contains(it, true) }) continue
      WebHook(webhook.url)
          .sendMessage(
              WebHook.WebHookMessage()
                  .setUsername("[$chatType-$language] $sender")
                  .addEmbed(WebHook.WebHookEmbed().setDescription(message)))
    }
  }

  private fun formatMessage(): String {
    var message = super.message
    // regex {I:5008;C:0;O:587374916} or {M:52;G:1;F:0;S:0;A:0;O:357221007}
    val regex = Regex("\\{[0-9a-zA-Z:;-]*}")
    val matches = regex.findAll(message)
    for (match in matches) {
      val json = JsonParser.parseString(match.value).asJsonObject
      if (json.has("I")) {
        val itemId = json.get("I").asInt
        val item = "[${itemNames[itemId]?: "UNKNOWN-ITEM"}]"
        message = message.replace(match.value, item)
      } else if (json.has("M")) {
        val moveId = json.get("M").asInt
        val shiny = if (json.has("S")) json.get("S").asInt == 1 else false
        val alpha = if (json.has("A")) json.get("A").asInt == 1 else false
        val genderID = if (json.has("G")) json.get("G").asInt else -1
        val gender = if (genderID == 1) "male" else if (genderID == 0) "female" else "genderless"
        val move = "[${monsterNames[moveId]}, Shiny: ${shiny}, Alpha: ${alpha}, $gender]"
        message = message.replace(match.value, move)
      } else {
        message = message.replace(match.value, "[UNKNOWN, RAW: $match.value]")
      }
    }
    return " $message "
  }
}
