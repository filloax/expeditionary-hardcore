package com.filloax.expeditionaryhardcore

import com.filloax.expeditionaryhardcore.config.ExpeditionaryHardcoreConfigHandler
import org.apache.logging.log4j.LogManager

object ExpeditionaryHardcore {
    const val MOD_ID = "exphardcore"
    const val MOD_NAME = "Expeditionary Hardcore"

    @JvmField
    val LOGGER = LogManager.getLogger(MOD_NAME)

    var isNeoforge = false

    private var initialized = false
    private var clientInitialized = false

    fun init() {
        if (initialized) return
        initialized = true

        LOGGER.info("Initializing")

        ExpeditionaryHardcoreConfigHandler.initConfig()

        ExpeditionaryHardcoreModEvents.get().initCallbacks()

        LOGGER.info("Initialized!")
    }

    fun initClient() {
        if (clientInitialized) return
        clientInitialized = true

        LOGGER.info("Initialized client!")
    }
}
