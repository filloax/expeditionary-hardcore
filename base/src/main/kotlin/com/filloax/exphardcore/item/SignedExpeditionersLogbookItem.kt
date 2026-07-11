package com.filloax.exphardcore.item

import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.WrittenBookItem
import net.minecraft.world.item.component.TooltipDisplay
import java.util.function.Consumer

class SignedExpeditionersLogbookItem(properties: Properties) : WrittenBookItem(properties), LogbookItem {
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

    companion object {
        // TODO: make language server-side
        // Title stamped on a logbook auto-signed on death, so it can be identified if found by another life
        const val LOST_LOGBOOK_TITLE = "Lost Logbook"
    }
}
