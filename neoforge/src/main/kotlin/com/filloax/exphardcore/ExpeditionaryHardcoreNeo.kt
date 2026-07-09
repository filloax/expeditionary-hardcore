package com.filloax.exphardcore

import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist

@Mod(ExpeditionaryHardcore.MOD_ID)
object ExpeditionaryHardcoreNeo {
    init {
        ExpeditionaryHardcore.isNeoforge = true
        ExpeditionaryHardcore.init()

        runForDist(
            clientTarget = {
                MOD_BUS.addListener<FMLClientSetupEvent> {
                    ExpeditionaryHardcore.initClient()
                }
            },
            serverTarget = {},
        )

        ExpeditionaryHardcore.LOGGER.info("Initialized NeoForge entry point")
    }
}
