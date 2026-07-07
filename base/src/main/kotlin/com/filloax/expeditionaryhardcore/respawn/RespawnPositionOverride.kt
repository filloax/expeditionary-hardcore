package com.filloax.expeditionaryhardcore.respawn

import com.filloax.expeditionaryhardcore.ExpeditionaryHardcore
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import java.util.Optional
import java.util.Optional.*
import kotlin.random.Random

object RespawnPositionOverride {
    /**
     * Sets the respawn suggestion (if any), meaning the replacement for world spawn point to use
     * as origin for search (will then still use spawn_radius gamerule, etc.)
     */
    @JvmStatic
    fun pickRespawnSuggestion(level: ServerLevel, player: ServerPlayer, vanillaSuggestion: BlockPos): Optional<BlockPos> {
        val provider = level.providerFor(player)
        val result = provider.getRespawnPosition()

        ExpeditionaryHardcore.LOGGER.info("Respawn position override for player $player: $result")

        return ofNullable(result)
    }

    private fun ServerLevel.providerFor(player: ServerPlayer): IRespawnPositionProvider {
        // select providers and things like that here if needed later
        return SimpleRespawnPositionProvider(this, player)
    }
}

fun interface IRespawnPositionProvider {
    fun getRespawnPosition(): BlockPos?
}

open class SimpleRespawnPositionProvider(
    val level: ServerLevel,
    val player: ServerPlayer,
) : IRespawnPositionProvider {
    // TODO: seed-based and persistent memory of respawns for seed-baseness
    protected val random = Random

    override fun getRespawnPosition(): BlockPos? {
        val center = getSearchCenter()
        val dist = random.nextDouble(radius.toDouble())
        val angle = random.nextDouble(2.0 * Math.PI)
        val dx = (dist * Mth.cos(angle)).toInt()
        val dz = (dist * Mth.sin(angle)).toInt()

        return center.offset(dx, 0, dz)
    }

    protected open val radius = 10000 // todo config, website, etc.

    protected open fun getSearchCenter(): BlockPos {
        // for simple, just return world spawn
        return level.respawnData.globalPos.pos
    }
}