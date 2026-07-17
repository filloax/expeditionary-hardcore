package com.filloax.exphardcore

import com.filloax.exphardcore.client.CreateWorldScreenHook
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens

object ExpeditionaryHardcoreFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        ExpeditionaryHardcoreFabric.registerClientResourceListeners()
        ExpeditionaryHardcoreFabric.initClient()

        ScreenEvents.AFTER_INIT.register { _, screen, _, _ ->
            CreateWorldScreenHook.install(screen) { vanilla, replacement ->
                val widgets = Screens.getWidgets(screen)
                widgets.remove(vanilla)
                widgets.add(replacement)
            }
        }
    }
}
