package com.filloax.expeditionaryhardcore.character

import com.filloax.expeditionaryhardcore.PERSISTENT_DATA_PLAYER_LIVES
import com.filloax.fxlib.api.codec.codec
import com.filloax.fxlib.api.json.BlockPosSerializer
import com.filloax.fxlib.api.json.UUIDSerializer
import com.filloax.fxlib.api.savedata.FxSavedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import com.ruslan.apibalego.utils.id
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraft.core.BlockPos
import net.minecraft.core.UUIDUtil
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.*

fun ServerPlayer.getExpeditionLife() = PlayerLifeData.getCurrentForPlayer(this)
fun ServerPlayer.getExpeditionLifeOrNull() = PlayerLifeData.getCurrentForPlayerOrNull(this)
fun ServerPlayer.newExpeditionLife() = PlayerLifeData.createNewForPlayer(this)
fun ServerPlayer.getAllExpeditionLives() = PlayerLifeData.getAllLives(this)

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
    var name: String?,
    @Serializable(with=BlockPosSerializer::class)
    var spawnPoint: BlockPos,
) {
    @Transient
    lateinit var server: MinecraftServer

    fun setDirty() {
        val data = ServerPlayerLifeData.get(server)
        data.setDirty()
    }

    companion object {
        fun getCurrentForPlayer(player: ServerPlayer): PlayerLifeData {
            val data = ServerPlayerLifeData.get(player.level().server)
            val allLives = data.playerData.getOrPut(player.uuid) { mutableListOf() }
            if (allLives.isEmpty()) {
                allLives.add(createNewForPlayer(player))
                data.setDirty()
            }
            return allLives.last().also { finalize(it, player.level().server) }
        }

        fun getCurrentForPlayerOrNull(player: ServerPlayer): PlayerLifeData? {
            val data = ServerPlayerLifeData.get(player.level().server)
            val allLives = data.playerData.getOrPut(player.uuid) { mutableListOf() }
            return allLives.lastOrNull()?.also { finalize(it, player.level().server) }
        }

        fun createNewForPlayer(player: ServerPlayer): PlayerLifeData {
            val data = ServerPlayerLifeData.get(player.level().server)
            val allLives = data.playerData.getOrPut(player.uuid) { mutableListOf() }

            val newId = UUID.randomUUID()
            return PlayerLifeData(
                newId,
                null,
                player.blockPosition()
            ).also {
                finalize(it, player.level().server)
                allLives.add(it)
                data.setDirty()
            }
        }

        fun getAllLives(player: ServerPlayer) = ServerPlayerLifeData.get(player.level().server)
            .playerData[player.uuid]
            ?.map { finalize(it, player.level().server) }
            ?: emptyList()

        private fun finalize(data: PlayerLifeData, server: MinecraftServer) {
            data.server = server
        }

    }
}

class ServerPlayerLifeData private constructor(
    playerData: Map<UUID, List<PlayerLifeData>> = mapOf(),
) : FxSavedData<ServerPlayerLifeData>(CODEC) {
    val playerData = playerData.mapValues { (id, ls) -> ls.toMutableList() }.toMutableMap()

    companion object {
        val CODEC: Codec<ServerPlayerLifeData> = RecordCodecBuilder.create { builder -> builder.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, PlayerLifeData.serializer().codec().listOf())
                    .optionalFieldOf("playerData", mapOf()).forGetter(ServerPlayerLifeData::playerData),
        ).apply(builder, ::ServerPlayerLifeData) }

        private val DEF = define(id(PERSISTENT_DATA_PLAYER_LIVES), ::ServerPlayerLifeData, CODEC)

        internal fun get(server: MinecraftServer): ServerPlayerLifeData {
            return server.loadData(DEF)
        }
    }
}