package com.filloax.exphardcore.cydonia

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.exphardcore.character.getAllExpeditionLives
import com.filloax.exphardcore.character.getExpeditionLifeOrNull
import com.filloax.exphardcore.config.CydoniaModeConfig
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import com.ruslan.apibalego.config.ApiBalegoConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.concurrent.thread

/**
 * Only for Cydonia mode, send assorted information to the apibalego-configured website
 * Not used in normal mode
 */
object ApibalegoInfoSender {
    private val http: HttpClient by lazy {
        // To be safe, error if it ever gets even just referenced
        if (!ExpeditionaryHardcore.cydoniaMode) throw IllegalStateException(
            "Cydonia mode is not enabled!"
        )

        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()
    }
    private val json = Json {
        explicitNulls = false
    }

    @JvmStatic
    fun onEditBook(stack: ItemStack, player: ServerPlayer, pages: List<String>) {
        if (stack.`is`(ExpeditionaryHardcoreItems.EXPEDITIONERS_LOGBOOK)) {
            sendLogbookAndCharacterContents(player, null, pages, null)
        }
    }

    @JvmStatic
    fun onSignBook(stack: ItemStack, player: ServerPlayer, pages: List<String>, title: String?) {
        if (stack.`is`(ExpeditionaryHardcoreItems.EXPEDITIONERS_LOGBOOK)) {
            sendLogbookAndCharacterContents(player, null, pages, title)
        }
    }

    fun onSaveCharacter(player: ServerPlayer, lifeData: PlayerLifeData) {
        sendLogbookAndCharacterContents(player, lifeData, listOf(), null)
        onUpdateLives(player)
    }

    fun onUpdateLives(player: ServerPlayer) {
        val allLives = player.getAllExpeditionLives()
        val request = ApibalegoPlayerLifeHistory(
            player.uuid,
            allLives.map(ApibalegoPlayerLifeData::fromLifeData),
            allLives.size - 1,
        )

        sendDataAsync("info/player_life_history", request, ApibalegoPlayerLifeHistory.serializer())
    }


    private fun sendLogbookAndCharacterContents(player: ServerPlayer, currentLife: PlayerLifeData?, pages: List<String>, title: String?) {
        val currentLife = player.getExpeditionLifeOrNull()
        val body = ApibalegoLogbookCharacterData(
            player.uuid,
            currentLife?.let(ApibalegoPlayerLifeData::fromLifeData),
            diaryPages = pages,
            diaryTitle = title,
        )
        sendDataAsync("info/logbook", body, ApibalegoLogbookCharacterData.serializer())
    }


    // other thread so this does not take up time
    private fun <T> sendDataAsync(endpoint: String, data: T, serializer: KSerializer<T>) = thread {
        if (!ExpeditionaryHardcore.cydoniaMode || !CydoniaModeConfig.sendInfoToApibalego) return@thread

        val url = getApibalegoUrl()
        val bodyStr = json.encodeToString(serializer, data)
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(bodyStr))
            .build()
        try {
            val resp = http.send(request, HttpResponse.BodyHandlers.ofString())
            ExpeditionaryHardcore.LOGGER.debug("Apibalego data info send $endpoint | Status ${resp.statusCode()}: ${resp.body()}")
        } catch (e: Exception) {
            ExpeditionaryHardcore.LOGGER.error("Failed to send data to apibalego $endpoint", e)
        }
    }

    private fun getApibalegoUrl(): String {
        return ApiBalegoConfig.dataSyncUrl
    }
}