package com.filloax.exphardcore

import com.filloax.exphardcore.client.model.PlayerModelOverrides
import com.filloax.exphardcore.item.ExpeditionaryHardcoreDataComponents
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import com.filloax.exphardcore.character.PlayerModelDefinitions
import com.filloax.exphardcore.character.quirk.LifeQuirkDefinitions
import com.filloax.exphardcore.effect.ExpeditionaryHardcoreMobEffects
import com.filloax.exphardcore.expedition.ExpeditionGameRules
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.fabricmc.fabric.api.resource.v1.ResourceLoader
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.PackType
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Items

object ExpeditionaryHardcoreFabric : ModInitializer, ExpeditionaryHardcore() {
    override fun onInitialize() {
        initialize()
    }

    override fun initRegistries() {
        ExpeditionaryHardcoreItems.registerItems { id, value -> Registry.register(BuiltInRegistries.ITEM, id, value) }
        ExpeditionaryHardcoreDataComponents.registerDataComponents { id, value ->
            Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id, value)
        }
        ExpeditionaryHardcoreMobEffects.registerEffects { id, value ->
            Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, id, value)
        }
        ExpeditionGameRules.register { id, rule -> Registry.register(BuiltInRegistries.GAME_RULE, id, rule) }
    }

    override fun initItemGroups() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register {
                it.insertAfter(Items.WRITABLE_BOOK, ExpeditionaryHardcoreItems.EXPEDITIONERS_LOGBOOK)
            }
    }

    override fun registerResourceListeners() {
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath(MOD_ID, "player_model_definitions"),
            PlayerModelDefinitions.ReloadListener(),
        )
        ResourceLoader.get(PackType.SERVER_DATA).registerReloadListener(
            Identifier.fromNamespaceAndPath(MOD_ID, "life_quirk_definitions"),
            LifeQuirkDefinitions.ReloadListener(),
        )
    }

    override fun registerClientResourceListeners() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            Identifier.fromNamespaceAndPath(MOD_ID, "player_models"),
            PlayerModelOverrides.reloadListener,
        )
    }
}
