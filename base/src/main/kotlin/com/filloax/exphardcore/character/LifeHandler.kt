package com.filloax.exphardcore.character

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.cydonia.ApibalegoInfoSender
import com.filloax.exphardcore.item.ExpeditionaryHardcoreDataComponents
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import com.filloax.exphardcore.item.ExpeditionersLogbookItem
import com.filloax.exphardcore.item.LogbookOwner
import com.filloax.exphardcore.item.SignedExpeditionersLogbookItem
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.Filterable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.WrittenBookContent

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

    @JvmStatic
    fun signLogbookOnDeath(player: ServerPlayer) {
        val inventory = player.inventory
        for (slot in 0 until inventory.containerSize) {
            val stack = inventory.getItem(slot)
            if (stack.item is ExpeditionersLogbookItem) {
                inventory.setItem(slot, stack.signAsLostLogbook(player))
            }
        }
    }

    private fun ItemStack.signAsLostLogbook(player: ServerPlayer): ItemStack {
        val pageStrings = get(DataComponents.WRITABLE_BOOK_CONTENT)?.pages()?.map { it.raw } ?: emptyList()
        val pages: List<Filterable<Component>> = get(DataComponents.WRITABLE_BOOK_CONTENT)?.pages()?.map { it.map { text -> Component.literal(text) } } ?: emptyList()
        val authorName = player.getExpeditionLifeOrNull()?.name ?: player.gameProfile.name

        val signed = transmuteCopy(ExpeditionaryHardcoreItems.SIGNED_EXPEDITIONERS_LOGBOOK)
        signed.remove(DataComponents.WRITABLE_BOOK_CONTENT)
        signed.set(
            DataComponents.WRITTEN_BOOK_CONTENT,
            WrittenBookContent(Filterable.passThrough(SignedExpeditionersLogbookItem.LOST_LOGBOOK_TITLE), authorName, 0, pages, true)
        )

        // TODO: make language server-side
        ApibalegoInfoSender.onSignBook(this, player, pageStrings, SignedExpeditionersLogbookItem.LOST_LOGBOOK_TITLE)

        return signed
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