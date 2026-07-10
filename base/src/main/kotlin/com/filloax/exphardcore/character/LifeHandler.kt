package com.filloax.exphardcore.character

import com.filloax.exphardcore.ExpeditionaryHardcore
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer

object LifeHandler {
    @JvmStatic
    fun newLife(player: ServerPlayer, position: BlockPos) {
        val newLifeData = player.newExpeditionLife()
        newLifeData.spawnPoint = position
        newLifeData.setDirty()

        CharacterLoadoutHandler.newLoadoutForPlayer(player, newLifeData)

        player.refreshExpeditionData()

        ExpeditionaryHardcore.LOGGER.info("New life for player {}: {}", player, newLifeData)
    }

    fun createCharacter(player: ServerPlayer, data: CharacterCreationData) {
        val currentLife = player.getExpeditionLife()

        if (currentLife.didCreation) {
            ExpeditionaryHardcore.LOGGER.warn("Player {} tried to create a new character but already has one", player)
            return
        }

        currentLife.name = data.name
        currentLife.didCreation = true
        currentLife.setDirty()

        player.refreshExpeditionData()

        player.sendSystemMessage(Component.translatable("exphardcore.character.creation.success", data.name)
            .withStyle(ChatFormatting.DARK_PURPLE)
        )
    }

    object Callbacks {
        fun onPlayerServerJoin(player: ServerPlayer) {
            if (player.getAllExpeditionLives().isEmpty()) {
                ExpeditionaryHardcore.LOGGER.info("Player {} joined with no lives, initialize", player)
                newLife(player, player.blockPosition())
            } else {
                player.refreshExpeditionData()
            }
        }
    }
}

interface CharacterCreationData {
    val name: String
}

fun ServerPlayer.refreshExpeditionData() {
    refreshExpeditionName()
    syncExpeditionLife()
}

private fun ServerPlayer.refreshExpeditionName() {
    val nickname = getExpeditionLifeOrNull()?.name
    customName = nickname?.let(Component::literal)
}