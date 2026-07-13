package com.filloax.exphardcore.character

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.PERSISTENT_DATA_PLAYER_LIVES
import com.filloax.exphardcore.network.ClientboundLifeSyncPacket
import com.filloax.fxlib.api.codec.codec
import com.filloax.fxlib.api.json.BlockPosSerializer
import com.filloax.fxlib.api.json.UUIDSerializer
import com.filloax.fxlib.api.networking.sendPacket
import com.filloax.fxlib.api.savedata.FxSavedData
import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.json.IdentifierSerializer
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.random.Random
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*

fun ServerPlayer.getExpeditionLife() = PlayerLifeData.getCurrentForPlayer(this)
fun ServerPlayer.getExpeditionLifeOrNull() = PlayerLifeData.getCurrentForPlayerOrNull(this)
fun ServerPlayer.newExpeditionLife() = PlayerLifeData.createNewForPlayer(this)
fun ServerPlayer.getAllExpeditionLives() = PlayerLifeData.getAllLives(this)

// Call whenever PlayerLifeData changes server-side, so the client never has to ask on demand
fun ServerPlayer.syncExpeditionLife() {
    val life = getExpeditionLifeOrNull()
    sendPacket(ClientboundLifeSyncPacket(life))
}

/**
 * Per the mod's concept, every new "life" (respawn) is a
 * new character, with things like name, quirks, Figura/other model,
 * etc. configurable.
 */
// Do not use boolean for fields due to how serializer.codec() works
@Serializable
data class PlayerLifeData(
    @Serializable(with=UUIDSerializer::class)
    val id: UUID,
    var didCreationInt: Int = 0, // int as boolean is not supported by fxlib kotlin Codecs
    var name: String? = null,
    @Serializable(with=BlockPosSerializer::class)
    var spawnPoint: BlockPos,
    @Serializable(with= IdentifierSerializer::class)
    val modelId: Identifier,
) {
    @Transient
    lateinit var server: MinecraftServer
    var didCreation: Boolean
        get() = didCreationInt != 0
        set(value) { didCreationInt = if (value) 1 else 0 }

    val model: PlayerModelDefinition?
        get() {
            if (!PlayerModelResolver.playerModelsByKey.containsKey(modelId))
                ExpeditionaryHardcore.LOGGER.warn("Player model {} not found, using default", modelId)
            return PlayerModelResolver.playerModelsByKey[modelId]
        }

    fun setDirty() {
        val data = ServerAllPlayersLifeData.get(server)
        data.setDirty()
    }

    companion object {
        fun getCurrentForPlayer(player: ServerPlayer): PlayerLifeData {
            val data = ServerAllPlayersLifeData.get(player.level().server)
            val allLives = data.playerData.getOrPut(player.uuid) { mutableListOf() }
            if (allLives.isEmpty()) {
                allLives.add(createNewForPlayer(player))
                data.setDirty()
            }
            return allLives.last().also { finalize(it, player.level().server) }
        }

        fun getCurrentForPlayerOrNull(player: ServerPlayer): PlayerLifeData? {
            val data = ServerAllPlayersLifeData.get(player.level().server)
            val allLives = data.playerData.getOrPut(player.uuid) { mutableListOf() }
            return allLives.lastOrNull()?.also { finalize(it, player.level().server) }
        }

        fun createNewForPlayer(player: ServerPlayer): PlayerLifeData {
            val data = ServerAllPlayersLifeData.get(player.level().server)
            val allLives = data.playerData.getOrPut(player.uuid) { mutableListOf() }
            val newId = UUID.randomUUID()
            val random = Random(newId.mostSignificantBits xor newId.leastSignificantBits)

            val model = PlayerModelResolver.playerModelsByKey.keys.random(random)

            return PlayerLifeData(
                newId,
                0,
                null,
                player.blockPosition(),
                model
            ).also {
                finalize(it, player.level().server)
                allLives.add(it)
                data.setDirty()
            }
        }

        fun getAllLives(player: ServerPlayer) = ServerAllPlayersLifeData.get(player.level().server)
            .playerData[player.uuid]
            ?.map { finalize(it, player.level().server) }
            ?: emptyList()

        private fun finalize(data: PlayerLifeData, server: MinecraftServer): PlayerLifeData {
            data.server = server
            return data
        }

    }
}

class ServerAllPlayersLifeData private constructor(
    playerData: Map<UUID, List<PlayerLifeData>> = mapOf(),
) : FxSavedData<ServerAllPlayersLifeData>(CODEC) {
    val playerData = playerData.mapValues { (id, ls) -> ls.toMutableList() }.toMutableMap()

    companion object {
        val CODEC: Codec<ServerAllPlayersLifeData> = RecordCodecBuilder.create { builder -> builder.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerLifeData.serializer().codec().listOf())
                    .optionalFieldOf("playerData", mapOf()).forGetter(ServerAllPlayersLifeData::playerData),
        ).apply(builder, ::ServerAllPlayersLifeData) }

        private val DEF = define(id(PERSISTENT_DATA_PLAYER_LIVES), ::ServerAllPlayersLifeData, CODEC)

        internal fun get(server: MinecraftServer): ServerAllPlayersLifeData {
            return server.loadData(DEF)
        }
    }
}