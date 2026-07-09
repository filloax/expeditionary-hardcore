package com.filloax.exphardcore.character

import com.filloax.exphardcore.ExpeditionaryHardcore
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object LifeHandler {
    @JvmStatic
    fun newLife(player: ServerPlayer, position: BlockPos) {
        val newLifeData = player.newExpeditionLife()
        newLifeData.spawnPoint = position
        newLifeData.setDirty()
        player.refreshExpeditionName()

        ExpeditionaryHardcore.LOGGER.info("New life for player {}: {}", player, newLifeData)
    }

    object Callbacks {
        fun onPlayerServerJoin(player: ServerPlayer) {
            player.refreshExpeditionName()
        }
    }
}

fun ServerPlayer.refreshExpeditionName() {
    val nickname = getExpeditionLifeOrNull()?.name
    customName = nickname?.let(Component::literal)
}