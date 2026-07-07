package com.filloax.expeditionaryhardcore

import net.fabricmc.api.ModInitializer

object ExpeditionaryHardcoreFabric : ModInitializer {
    override fun onInitialize() {
        ExpeditionaryHardcore.init()

        ExpeditionaryHardcore.LOGGER.info("Initialized Fabric entry point")
    }
}
