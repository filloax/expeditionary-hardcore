package com.filloax.exphardcore

import com.filloax.exphardcore.client.model.PlayerModelOverrides
import com.filloax.exphardcore.item.ExpeditionaryHardcoreDataComponents
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
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
    }

    override fun initItemGroups() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register {
                it.insertAfter(Items.WRITABLE_BOOK, ExpeditionaryHardcoreItems.EXPEDITIONERS_LOGBOOK)
            }
    }

    override fun registerResourceListeners() {
    }

    override fun registerClientResourceListeners() {
        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(
            Identifier.fromNamespaceAndPath(MOD_ID, "player_models"),
            PlayerModelOverrides.reloadListener,
        )
    }
}
