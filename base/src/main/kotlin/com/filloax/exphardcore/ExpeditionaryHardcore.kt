package com.filloax.exphardcore

import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfigHandler
import com.filloax.exphardcore.respawn.RespawnConfigResolver
import com.filloax.exphardcore.utils.loadPropertiesFile
import org.apache.logging.log4j.LogManager

object ExpeditionaryHardcore {
    const val MOD_ID = "exphardcore"
    const val MOD_NAME = "Expeditionary Hardcore"

    @JvmField
    val LOGGER = LogManager.getLogger(MOD_NAME)

    var isNeoforge = false

    @JvmField
    val cydoniaMode: Boolean = loadPropertiesFile("cydonia.properties")["cydoniaMode"]!!.toBoolean()

    private var initialized = false
    private var clientInitialized = false

    fun init() {
        if (initialized) return
        initialized = true

        LOGGER.info("Initializing")

        ExpeditionaryHardcoreConfigHandler.initConfig()

        RespawnConfigResolver.init()

        ExpeditionaryHardcoreModEvents.get().initCallbacks()

        if (cydoniaMode) LOGGER.info("Cydonia mode enabled")

        LOGGER.info("Initialized!")
    }

    fun initClient() {
        if (clientInitialized) return
        clientInitialized = true

        LOGGER.info("Initialized client!")
    }
}
