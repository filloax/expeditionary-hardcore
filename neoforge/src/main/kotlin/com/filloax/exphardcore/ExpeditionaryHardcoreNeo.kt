package com.filloax.exphardcore

import com.filloax.exphardcore.item.ExpeditionaryHardcoreDataComponents
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import net.minecraft.core.registries.Registries
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.RegisterEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

@Mod(ExpeditionaryHardcore.MOD_ID)
object ExpeditionaryHardcoreNeo : ExpeditionaryHardcore() {
    init {
        isNeoforge = true
        initialize()

        runForDist(
            clientTarget = {
                MOD_BUS.addListener<FMLClientSetupEvent> {
                    initClient()
                }
            },
            serverTarget = {},
        )
    }

    override fun initItemGroups() {
        MOD_BUS.addListener<BuildCreativeModeTabContentsEvent> { ev ->
            val entries = ev.searchEntries.associateBy { it.item }
            val addAfter = { item: Item, new: Item ->
                ev.insertAfter(entries[item]!!, new.defaultInstance, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY)
            }

            when (ev.tabKey) {
                CreativeModeTabs.TOOLS_AND_UTILITIES -> {
                    addAfter(Items.WRITABLE_BOOK, ExpeditionaryHardcoreItems.EXPEDITIONERS_LOGBOOK)
                }
            }
        }
    }

    override fun registerResourceListeners() {
    }

    override fun initRegistries() {
        MOD_BUS.addListener<RegisterEvent> { ev ->
            ev.register(Registries.ITEM) { helper -> ExpeditionaryHardcoreItems.registerItems(helper::register) }
            ev.register(Registries.DATA_COMPONENT_TYPE) { helper ->
                ExpeditionaryHardcoreDataComponents.registerDataComponents(helper::register)
            }
        }
    }
}
