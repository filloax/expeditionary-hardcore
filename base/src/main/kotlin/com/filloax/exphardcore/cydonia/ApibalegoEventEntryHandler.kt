package com.filloax.exphardcore.cydonia

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.PERSISTENT_DATA_SENT_API_EVENTS
import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.savedata.FxSavedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.apibalego.http.ApiEntry
import com.ruslan.apibalego.http.ApiEntryHandler
import com.ruslan.apibalego.http.ApiEntryRegistry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import net.minecraft.server.MinecraftServer

/**
 * [Cydonia mode] when active, send data to site. Mainly intended to use with Apibalego triggers
 * <br>
 * Can lead to complex behavior when paired with custom advancement triggers + server logic
 */
object ApibalegoEventEntryHandler {
    val ID = id("send_event")

    @Serializable
    data class EventDetails(
        val event: String,
        val data: JsonElement? = null,
    )

    fun init() {
        if (!ExpeditionaryHardcore.cydoniaMode || !ExpeditionaryHardcore.modCompat.isApibalegoLoaded) return

        ApiEntryRegistry.register(ID, EventDetails.serializer(), Handler)
    }

    // Separate class so Apibalego classes are only loaded when registering
    private object Handler : ApiEntryHandler<EventDetails> {
        override fun handleApiUpdate(server: MinecraftServer, entries: Collection<ApiEntry<EventDetails>>) {
            val toSend = entries.filter { it.active && it.details != null }
            if (toSend.isEmpty()) return

            // Active entries get re-dispatched on every api update, track what was already sent
            server.execute {
                val sentData = SentApiEventsData.get(server)
                toSend.forEach { entry ->
                    if (!sentData.sent.add(entry.id)) return@forEach
                    sentData.setDirty()
                    ApibalegoInfoSender.onApiEvent(entry.id, entry.details!!)
                }
            }
        }
    }
}

class SentApiEventsData private constructor(
    sent: Collection<String> = listOf(),
) : FxSavedData<SentApiEventsData>(CODEC) {
    val sent = sent.toMutableSet()

    companion object {
        val CODEC: Codec<SentApiEventsData> = RecordCodecBuilder.create { builder -> builder.group(
            Codec.STRING.listOf().optionalFieldOf("sent", listOf()).forGetter { it.sent.toList() },
        ).apply(builder, ::SentApiEventsData) }

        private val DEF = define(id(PERSISTENT_DATA_SENT_API_EVENTS), ::SentApiEventsData, CODEC)

        internal fun get(server: MinecraftServer): SentApiEventsData {
            return server.loadData(DEF)
        }
    }
}

@Serializable
data class ApibalegoApiEventData(
    val entryId: String,
    val event: String,
    val data: JsonElement?,
)
