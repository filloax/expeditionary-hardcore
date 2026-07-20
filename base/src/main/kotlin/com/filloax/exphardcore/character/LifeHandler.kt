package com.filloax.exphardcore.character

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.character.quirk.LifeQuirkHandler
import com.filloax.exphardcore.character.team.TeamManager
import com.filloax.exphardcore.cydonia.ApibalegoInfoSender
import com.filloax.exphardcore.expedition.ExpeditionMode
import com.filloax.exphardcore.item.*
import com.filloax.exphardcore.network.ClientboundLifeSyncPacket
import com.filloax.exphardcore.network.DATA_PLAYER_MODEL
import com.filloax.fxlib.api.networking.sendPacket
import com.filloax.fxlib.api.networking.setTrackedData
import net.minecraft.resources.Identifier
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.Filterable
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.WrittenBookContent
import java.util.*

object LifeHandler {
    @JvmStatic
    fun newLife(player: ServerPlayer, position: BlockPos) {
        if (!ExpeditionMode.enabled) return

        val newLifeData = player.newExpeditionLife()
        newLifeData.spawnPoint = position
        newLifeData.setDirty()

        CharacterLoadoutHandler.newLoadoutForPlayer(player, newLifeData)
        LifeQuirkHandler.newQuirkForPlayer(player, newLifeData)

        if (TeamManager.multiplayerEnabled()) {
            val server = player.level().server
            if (!TeamManager.isSecondary(server, player)) {
                TeamManager.resetAllSecondaryLoadoutUsed(server, player)
            }
        }

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

    fun playerInit(player: ServerPlayer) {
        ExpeditionMode.instance.syncToClient(player)
        if (!ExpeditionMode.enabled) return

        if (player.getAllExpeditionLives().isEmpty()) {
            if (tryMigrateSingleplayerLives(player)) {
                player.refreshExpeditionData()
            } else {
                ExpeditionaryHardcore.LOGGER.info("Player {} joined with no lives, initialize", player)
                newLife(player, player.blockPosition())
            }
        } else {
            player.refreshExpeditionData()
        }
    }

    /**
     * In singleplayer, UUID for main player might change (offline mode, passing
     * save, etc.)
     */
    private fun tryMigrateSingleplayerLives(player: ServerPlayer): Boolean {
        val server = player.level().server
        if (!server.isSingleplayer) return false
        if (!server.isSingleplayerOwner(player.nameAndId())) return false

        val data = ServerAllPlayersLifeData.get(server)

        if (!data.playerData[player.uuid].isNullOrEmpty()) {
            recordOwner(data, player.uuid)
            return false
        }

        val recorded = data.lastSingleplayerOwner
        val oldUuid = when {
            recorded != null && recorded != player.uuid && !data.playerData[recorded].isNullOrEmpty() -> recorded
            recorded == null -> data.playerData
                .filter { (uuid, lives) -> uuid != player.uuid && lives.isNotEmpty() }
                .keys.singleOrNull()
            else -> null
        }

        if (oldUuid == null) {
            recordOwner(data, player.uuid)
            return false
        }

        data.playerData[player.uuid] = data.playerData.remove(oldUuid)?.toMutableList() ?: mutableListOf()
        recordOwner(data, player.uuid)

        ExpeditionaryHardcore.LOGGER.info(
            "Migrated singleplayer lives from old uuid {} to current uuid {}", oldUuid, player.uuid
        )
        return true
    }

    private fun recordOwner(data: ServerAllPlayersLifeData, uuid: UUID) {
        if (data.lastSingleplayerOwner != uuid) {
            data.lastSingleplayerOwner = uuid
            data.setDirty()
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
            playerInit(player)
        }
    }
}

interface CharacterCreationData {
    val name: String
}

fun ServerPlayer.refreshExpeditionData() {
    refreshExpeditionName()
    syncExpeditionLife()
    syncExpeditionModel()
}

fun ServerPlayer.syncExpeditionModel() {
    val modelResourceId = Optional.ofNullable(getExpeditionLifeOrNull()?.model?.model)
    setTrackedData(DATA_PLAYER_MODEL, modelResourceId, includeSelf = true)
}

private fun ServerPlayer.refreshExpeditionName() {
    val nickname = getExpeditionLifeOrNull()?.name
    customName = nickname?.let(Component::literal)
}

fun ServerPlayer.applyExpeditionState() {
    if (ExpeditionMode.enabled) {
        LifeHandler.playerInit(this)
    } else {
        clearExpeditionData()
    }
}

fun ServerPlayer.clearExpeditionData() {
    customName = null
    sendPacket(ClientboundLifeSyncPacket(null))
    setTrackedData(DATA_PLAYER_MODEL, Optional.empty<Identifier>(), includeSelf = true)
}