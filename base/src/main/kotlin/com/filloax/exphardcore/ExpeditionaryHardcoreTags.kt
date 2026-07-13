package com.filloax.exphardcore

import com.filloax.exphardcore.utils.id
import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.level.storage.loot.LootTable

object ExpeditionaryHardcoreTags {
        val LOOT_TABLE_CHARACTER_LOADOUT: TagKey<LootTable> = TagKey.create(Registries.LOOT_TABLE, id("character_loadout"))
        val LOOT_TABLE_CHARACTER_LOADOUT_OVERRIDE: TagKey<LootTable> = TagKey.create(Registries.LOOT_TABLE, id("character_loadout_override"))
}