package com.filloax.expeditionaryhardcore

import org.apache.logging.log4j.LogManager

object ExpeditionaryHardcore {
    const val MOD_ID = "expeditionaryhardcore"
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

        LOGGER.info("Initialized!")
    }

    fun initClient() {
        if (clientInitialized) return
        clientInitialized = true

        LOGGER.info("Initialized client!")
    }
}
