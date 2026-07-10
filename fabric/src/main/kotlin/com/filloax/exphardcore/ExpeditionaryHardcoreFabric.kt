package com.filloax.exphardcore

import com.filloax.exphardcore.item.ExpeditionaryHardcoreDataComponents
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
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
}
