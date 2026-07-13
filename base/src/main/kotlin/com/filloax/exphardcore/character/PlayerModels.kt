package com.filloax.exphardcore.character

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.json.IdentifierSerializer
import com.filloax.fxlib.api.json.KotlinJsonResourceReloadListener
import com.ruslan.apibalego.api.Apibalego
import com.ruslan.apibalego.http.ApiEntry
import com.ruslan.apibalego.http.ApiEntryHandler
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller


object PlayerModelResolver {
    val playerModels
        get() = playerModelsByKey.values

    val playerModelsByKey: Map<Identifier, PlayerModelDefinition>
        get() {
            val models = mutableMapOf<Identifier, PlayerModelDefinition>()
            val replaceBuiltins = shouldDisableBuiltins()
            if (!replaceBuiltins) {
                models.putAll(PlayerModelDefinitions.all)
            } else {
                models.putAll(PlayerModelDefinitions.all.filterValues { !it.builtin })
            }
            if (ExpeditionaryHardcore.modCompat.isApibalegoLoaded) {
                models.putAll(PlayerModelsApibalegoHandler.extraModels)
            }
            return models.toMap()
        }

    fun init() {
        if (ExpeditionaryHardcore.modCompat.isApibalegoLoaded) {
            PlayerModelsApibalegoHandler.init()
        }
    }

    private fun shouldDisableBuiltins(): Boolean {
        if (ExpeditionaryHardcore.modCompat.isApibalegoLoaded) {
            return PlayerModelsApibalegoHandler.replaceBuiltins
        }
        return false
    }
}

/**
 * Server-side pointer to a player model, to sync it for multiplayer
 */
@Serializable
data class PlayerModelDefinition(
    @Serializable(with = IdentifierSerializer::class)
    val model: Identifier,
    val builtin: Boolean = false,
)

object PlayerModelDefinitions {
    const val DIRECTORY = "exphard_playermodels"

    private var definitions: Map<Identifier, PlayerModelDefinition> = mapOf()

    val all: Map<Identifier, PlayerModelDefinition> get() = definitions

    operator fun get(id: Identifier): PlayerModelDefinition? = definitions[id]

    class ReloadListener : KotlinJsonResourceReloadListener(Json, DIRECTORY) {
        override fun apply(
            elements: Map<Identifier, JsonElement>,
            resourceManager: ResourceManager,
            profiler: ProfilerFiller,
        ) {
            definitions = elements.mapNotNull { (id, element) ->
                try {
                    id to Json.decodeFromJsonElement(PlayerModelDefinition.serializer(), element)
                } catch (e: Exception) {
                    ExpeditionaryHardcore.LOGGER.error("Failed to load player model definition {}", id, e)
                    null
                }
            }.toMap()
        }
    }
}

//#region Apibalego

/**
 * Mainly exists to allow replacing builtin models outright
 * (datapacks otherwise could already add new ones).
 *
 * Use resource packs for actual models
 */
@Serializable
data class PlayerModelApibalegoDetails(
    val models: List<@Serializable(with = IdentifierSerializer::class) Identifier> = listOf(),
    val replaceBuiltins: Boolean = false,
)

object PlayerModelsApibalegoHandler : ApiEntryHandler<PlayerModelApibalegoDetails> {
    var replaceBuiltins = false
        private set

    var extraModels = mapOf<Identifier, PlayerModelDefinition>()
        private set

    fun init() {
        Apibalego.registerApiHandler(id("player_models"), PlayerModelApibalegoDetails.serializer(), this)
    }

    override fun handleApiUpdate(
        server: MinecraftServer,
        entries: Collection<ApiEntry<PlayerModelApibalegoDetails>>
    ) {
        val replaceBuiltinsNew = entries.any { it.details!!.replaceBuiltins }
        val models = entries.flatMap { it.details!!.models }.toSet()
        replaceBuiltins = replaceBuiltinsNew
        extraModels = models.associateWith { PlayerModelDefinition(it, false) }
    }
}

// #endregion