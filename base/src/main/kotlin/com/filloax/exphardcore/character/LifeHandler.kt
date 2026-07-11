package com.filloax.exphardcore.character

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.cydonia.ApibalegoInfoSender
import com.filloax.exphardcore.item.ExpeditionaryHardcoreDataComponents
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import com.filloax.exphardcore.item.LogbookOwner
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

        ApibalegoInfoSender.onUpdateLives(player)
    }

    fun ServerPlayer.createExpeditionCharacter(data: CharacterCreationData) {
        val currentLife = getExpeditionLife()

        if (currentLife.didCreation) {
            ExpeditionaryHardcore.LOGGER.warn("Player {} tried to create a new character but already has one", this)
            return
        }

        currentLife.name = data.name
        currentLife.didCreation = true
        currentLife.setDirty()

        refreshExpeditionData()

        setLogbookOwner(data.name)

        sendSystemMessage(Component.translatable("exphardcore.character.creation.success", data.name)
            .withStyle(ChatFormatting.DARK_PURPLE)
        )

        ApibalegoInfoSender.onSaveCharacter(this, currentLife)
    }

    private fun ServerPlayer.setLogbookOwner(name: String) {
        val checkItem = ExpeditionaryHardcoreItems.EXPEDITIONERS_LOGBOOK
        val logbook = if (inventory.selectedItem.`is`(checkItem))
            inventory.selectedItem
        else
            inventory.find { it.`is`(checkItem) } ?: return

        logbook.set(ExpeditionaryHardcoreDataComponents.LOGBOOK_OWNER, LogbookOwner(getExpeditionLife().id, name))
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