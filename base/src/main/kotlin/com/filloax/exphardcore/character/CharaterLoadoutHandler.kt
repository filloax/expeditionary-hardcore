package com.filloax.exphardcore.character

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.ExpeditionaryHardcoreTags
import com.filloax.exphardcore.character.team.ServerTeamsData
import com.filloax.exphardcore.character.team.TeamManager
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems.EXPEDITIONERS_LOGBOOK
import com.filloax.exphardcore.respawn.TeammateRespawnProvider
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.ItemTags
import net.minecraft.util.RandomSource
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random

object CharacterLoadoutHandler {
    const val LAST_HOTBAR_SLOT = 8
    const val FIRST_INVENTORY_SLOT = 9
    const val LAST_INVENTORY_SLOT = 36

    fun newLoadoutForPlayer(player: ServerPlayer, lifeData: PlayerLifeData) {
        // first, add logbook
        val logbookStack = EXPEDITIONERS_LOGBOOK.defaultInstance.copy()
        if (player.inventory.getItem(LAST_HOTBAR_SLOT).isEmpty) {
            player.inventory.setItem(LAST_HOTBAR_SLOT, logbookStack)
        } else {
            player.addItem(logbookStack)
        }

        var giveLoadout = true
        val server = player.level().server
        val hasMultiplayer = TeamManager.multiplayerEnabled()
        val isSecondary = hasMultiplayer && TeamManager.isSecondary(server, player)
        if (isSecondary && !TeamManager.isSecondaryLoadoutAvailable(server, player)) {
            giveLoadout = false
        }

        if (giveLoadout)
            giveRandomLoadout(player, lifeData.random())

        if (isSecondary) {
            TeamManager.setSecondaryLoadoutUsed(server, player)
        }
    }

    fun giveRandomLoadout(player: ServerPlayer, random: Random) {
        val level = player.level()
        val lootTables = level.server.reloadableRegistries().lookup().lookupOrThrow(Registries.LOOT_TABLE)
        val randomSource = RandomSource.create(random.nextLong())
        val tablesList = lootTables.get(ExpeditionaryHardcoreTags.LOOT_TABLE_CHARACTER_LOADOUT_OVERRIDE)
            .takeIf { (it.getOrNull()?.size() ?: 0) > 0 }
            ?: lootTables.get(ExpeditionaryHardcoreTags.LOOT_TABLE_CHARACTER_LOADOUT)
        val chosenTable = tablesList
            .flatMap { it.getRandomElement(randomSource) }
            .map { it.value() }
            .orElse(null)

        if (chosenTable == null) {
            ExpeditionaryHardcore.LOGGER.warn("No loadout loot table found in tag {}, skipping starting loadout", ExpeditionaryHardcoreTags.LOOT_TABLE_CHARACTER_LOADOUT.location())
            return
        }

        val params = LootParams.Builder(level)
            .withParameter(LootContextParams.ORIGIN, player.position())
            .withParameter(LootContextParams.THIS_ENTITY, player)
            .create(LootContextParamSets.GIFT)

        val itemList = chosenTable.getRandomItems(params)
        assignItemsToSlots(player, itemList, random)
    }

    private fun assignItemsToSlots(player: ServerPlayer, items: Collection<ItemStack>, random: Random) {
        // Weapons then tools: hotbar (except for last slot used by logbook)
        // Then armor: equip best one if available
        // Banners in offhand
        // Then remaining items in inventory

        val hotbarCandidates = items.filter { it[DataComponents.WEAPON] != null || it[DataComponents.TOOL] != null }
            // weapons first
            .sortedBy { if (it[DataComponents.WEAPON] != null) 0 else 1 }
        val freeHotbarSlots = (0 ..< LAST_HOTBAR_SLOT).filter { slotIdx ->
            player.inventory.getItem(slotIdx).isEmpty
        }
        val hotbarItems = hotbarCandidates.take(freeHotbarSlots.size)

        val bestArmorBySlot = items.filter{ it[DataComponents.EQUIPPABLE] != null }
            .groupBy { it[DataComponents.EQUIPPABLE]!!.slot }
            .mapValues { (slot, items) -> items.maxBy { getItemArmorValue(it, slot) } }

        val banner = items.find { it.`is`(ItemTags.BANNERS) }
            ?.takeIf { player.offhandItem.isEmpty }

        val remaining = (items
                - hotbarItems.toSet()
                - bestArmorBySlot.values.toSet()
                - (banner?.let { setOf(it) } ?: setOf())
                )
                .toMutableList()

        bestArmorBySlot.forEach { (slot, item) ->
            if (player.hasItemInSlot(slot)) {
                remaining.add(item)
            } else {
                player.setItemSlot(slot, item)
            }
        }

        freeHotbarSlots.zip(hotbarItems).forEach { (slotIdx, item) ->
            player.inventory.setItem(slotIdx, item)
        }

        banner?.let { player.setItemSlot(EquipmentSlot.OFFHAND, it) }

        val remainingSlots = randomFreeSlots(player, remaining.size, random)
        remaining.zip(remainingSlots).forEach { (item, slotIdx) ->
            player.inventory.setItem(slotIdx, item)
        }
    }

    private fun getItemArmorValue(item: ItemStack, slot: EquipmentSlot): Double {
        val mods = item[DataComponents.ATTRIBUTE_MODIFIERS] ?: ItemAttributeModifiers.EMPTY
        return mods.compute(Attributes.ARMOR, 0.0, slot)
    }

    private fun randomFreeSlots(player: ServerPlayer, amount: Int, random: Random): Collection<Int> {
        val inventoryBackpackSlots = FIRST_INVENTORY_SLOT .. LAST_INVENTORY_SLOT
        val freeSlots = inventoryBackpackSlots.filter { player.inventory.getItem(it).isEmpty }
        return freeSlots.shuffled(random).take(amount)
    }
}