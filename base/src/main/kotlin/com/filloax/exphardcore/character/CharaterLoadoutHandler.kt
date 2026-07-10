package com.filloax.exphardcore.character

import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems.EXPEDITIONERS_LOGBOOK
import net.minecraft.server.level.ServerPlayer

object CharacterLoadoutHandler {
    const val LAST_HOTBAR_SLOT = 8

    fun newLoadoutForPlayer(player: ServerPlayer, lifeData: PlayerLifeData) {
        // first, add logbook
        val logbookStack = EXPEDITIONERS_LOGBOOK.defaultInstance.copy()
        if (player.inventory.getItem(LAST_HOTBAR_SLOT).isEmpty) {
            player.inventory.setItem(LAST_HOTBAR_SLOT, logbookStack)
        } else {
            player.addItem(logbookStack)
        }

        // TODO: add loot table
    }
}