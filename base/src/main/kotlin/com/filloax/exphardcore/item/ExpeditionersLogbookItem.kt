package com.filloax.exphardcore.item

import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.exphardcore.character.getExpeditionLifeOrNull
import com.filloax.exphardcore.client.CharacterCreationScreen
import com.filloax.exphardcore.client.clientPlayerLifeData
import net.minecraft.client.Minecraft
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.stats.Stats
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.WritableBookItem
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.level.Level
import java.util.function.Consumer

class ExpeditionersLogbookItem(properties: Properties) : WritableBookItem(properties) {
    override fun isFoil(stack: ItemStack): Boolean {
        val owner = stack.get(ExpeditionaryHardcoreDataComponents.LOGBOOK_OWNER)
            ?: return clientPlayerLifeData?.didCreation != true
        return clientPlayerLifeData?.id == owner.lifeId
    }

    // Likely to be removed when MC changes how tooltips are done again
    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipDisplay: TooltipDisplay,
        tooltipAdder: Consumer<Component>,
        tooltipFlag: TooltipFlag,
    ) {
        super.appendHoverText(stack, context, tooltipDisplay, tooltipAdder, tooltipFlag)
        stack.addToTooltip(ExpeditionaryHardcoreDataComponents.LOGBOOK_OWNER, context, tooltipDisplay, tooltipAdder, tooltipFlag)
    }

    override fun use(level: Level, player: Player, hand: InteractionHand): InteractionResult {
        // if life data not filled yet: open name (+ more things later?) GUI
        // otherwise, go to super, which on the client side opens writable book GUI

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