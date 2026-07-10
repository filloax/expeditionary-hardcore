package com.filloax.exphardcore.item

import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.exphardcore.character.getExpeditionLifeOrNull
import com.filloax.exphardcore.client.CharacterCreationScreen
import com.filloax.exphardcore.client.clientPlayerLifeData
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.WritableBookItem
import net.minecraft.world.level.Level

class ExpeditionersLogbookItem(properties: Properties) : WritableBookItem(properties) {
    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        // if life data not filled yet: open name (+ more things later?) GUI
        // otherwise, go to super, which on the client side opens writable book GUI
        // (this will always skip server side

        if (player is ServerPlayer) {
            if (shouldOpen(player.getExpeditionLifeOrNull())) {
                player.awardStat(Stats.ITEM_USED.get(this))
                return InteractionResult.SUCCESS
            } else {
                return super.use(level, player, hand)
            }
        } else if (player is LocalPlayer) {
            if (shouldOpen(clientPlayerLifeData)) {
                player.awardStat(Stats.ITEM_USED.get(this))
                val minecraft = Minecraft.getInstance()
                val currentScreen = minecraft.gui.screen()

                minecraft.gui.setScreen(CharacterCreationScreen(currentScreen))
                return InteractionResult.SUCCESS
            } else {
                return super.use(level, player, hand)
            }
        }
        return InteractionResult.PASS
    }

    private fun shouldOpen(lifeData: PlayerLifeData?) =
        lifeData?.didCreation != true // null or false
}