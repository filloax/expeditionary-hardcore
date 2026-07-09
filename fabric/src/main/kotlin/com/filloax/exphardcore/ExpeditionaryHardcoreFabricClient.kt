package com.filloax.exphardcore

import net.fabricmc.api.ClientModInitializer

object ExpeditionaryHardcoreFabricClient : ClientModInitializer {
    override fun onInitializeClient() {
        ExpeditionaryHardcore.initClient()
    }
}
