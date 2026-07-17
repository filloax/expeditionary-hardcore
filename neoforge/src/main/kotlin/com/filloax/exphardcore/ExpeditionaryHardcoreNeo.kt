package com.filloax.exphardcore

import com.filloax.exphardcore.client.CreateWorldScreenHook
import com.filloax.exphardcore.client.model.PlayerModelOverrides
import com.filloax.exphardcore.expedition.ExpeditionGameRules
import com.filloax.exphardcore.item.ExpeditionaryHardcoreDataComponents
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import com.filloax.exphardcore.character.PlayerModelDefinitions
import com.filloax.exphardcore.character.quirk.LifeQuirkDefinitions
import com.filloax.exphardcore.effect.ExpeditionaryHardcoreMobEffects
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent
import net.neoforged.neoforge.client.event.ScreenEvent
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import net.neoforged.neoforge.event.AddServerReloadListenersEvent
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent
import net.neoforged.neoforge.registries.RegisterEvent
import thedarkcolour.kotlinforforge.neoforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

@Mod(ExpeditionaryHardcore.MOD_ID)
object ExpeditionaryHardcoreNeo : ExpeditionaryHardcore() {
    init {
        isNeoforge = true
        initialize()

        runForDist(
            clientTarget = {
                registerClientResourceListeners()
                MOD_BUS.addListener<FMLClientSetupEvent> {
                    initClient()
                }
                FORGE_BUS.addListener<ScreenEvent.Init.Post> { ev ->
                    CreateWorldScreenHook.install(ev.screen) { vanilla, replacement ->
                        ev.removeListener(vanilla)
                        ev.addListener(replacement)
                    }
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
        FORGE_BUS.addListener<AddServerReloadListenersEvent> { ev ->
            ev.addListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "player_model_definitions"),
                PlayerModelDefinitions.ReloadListener(),
            )
            ev.addListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "life_quirk_definitions"),
                LifeQuirkDefinitions.ReloadListener(),
            )
        }
    }

    override fun registerClientResourceListeners() {
        MOD_BUS.addListener<AddClientReloadListenersEvent> { ev ->
            ev.addListener(
                Identifier.fromNamespaceAndPath(MOD_ID, "player_models"),
                PlayerModelOverrides.reloadListener,
            )
        }
    }

    override fun initRegistries() {
        MOD_BUS.addListener<RegisterEvent> { ev ->
            ev.register(Registries.ITEM) { helper -> ExpeditionaryHardcoreItems.registerItems(helper::register) }
            ev.register(Registries.DATA_COMPONENT_TYPE) { helper ->
                ExpeditionaryHardcoreDataComponents.registerDataComponents(helper::register)
            }
            ev.register(Registries.MOB_EFFECT) { _ ->
                ExpeditionaryHardcoreMobEffects.registerEffects { id, value ->
                    Registry.registerForHolder(ev.getRegistry(Registries.MOB_EFFECT)!!, id, value)
                }
            }
            ev.register(Registries.GAME_RULE) { helper -> ExpeditionGameRules.register(helper::register) }
        }
    }
}
