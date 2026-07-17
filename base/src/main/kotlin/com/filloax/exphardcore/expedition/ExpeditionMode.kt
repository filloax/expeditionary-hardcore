package com.filloax.exphardcore.expedition

import com.filloax.exphardcore.network.ClientboundExpeditionSyncPacket
import com.filloax.fxlib.api.EventUtil
import com.filloax.fxlib.api.networking.sendPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Difficulty

/**
 * Whether the mod's gameplay features (respawn override, lives/loadouts, quirks,
 * logbook rules, custom names) are active for the current world. 
 * Uses/sets the [ExpeditionGameRules.EXPEDITION] gamerule.
 */
class ExpeditionMode(
    val server: MinecraftServer
) {
    companion object {
        lateinit var instance: ExpeditionMode
            private set


        var enabled: Boolean
            get() = instance.enabled
            set(value) { instance.enabled = value }

        private fun initInstance(server: MinecraftServer): ExpeditionMode {
            return ExpeditionMode(server).also { instance = it }
        }

        @JvmStatic
        fun isEnabled(): Boolean = enabled
    }

    object Callbacks {
        fun onServerStarting(server: MinecraftServer) {
            initInstance(server)
            // Re-assert the Hard lock for expedition worlds on every load.
            EventUtil.runWhenServerStarted {
                instance.applyDifficultyLock()
            }
        }
    }

    var enabled: Boolean
        get() = server.gameRules.get(ExpeditionGameRules.EXPEDITION)
        set(value) {
            if (enabled == value) return
            server.gameRules.set(ExpeditionGameRules.EXPEDITION, value, server)
            if (value) applyDifficultyLock() else server.setDifficultyLocked(false)
            syncToClients()
        }

    /**
     * Force and lock Hard difficulty while expedition mode is on. Called on set and
     * on world load so the lock survives reloads.
     */
    fun applyDifficultyLock() {
        if (!isEnabled()) return
        server.setDifficulty(Difficulty.HARD, true)
        server.setDifficultyLocked(true)
    }

    fun syncToClients() {
        val enabled = isEnabled()
        server.playerList.players.forEach { it.sendPacket(ClientboundExpeditionSyncPacket(enabled)) }
    }

    fun syncToClient(player: ServerPlayer) {
        player.sendPacket(ClientboundExpeditionSyncPacket(isEnabled()))
    }
}
