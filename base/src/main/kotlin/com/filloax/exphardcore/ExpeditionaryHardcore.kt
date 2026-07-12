package com.filloax.exphardcore

import com.filloax.exphardcore.compat.ModCompatChecker
import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfigHandler
import com.filloax.exphardcore.network.ExpeditionaryHardcorePackets
import com.filloax.exphardcore.respawn.RespawnConfigResolver
import com.filloax.exphardcore.utils.loadPropertiesFile
import com.filloax.fxlib.api.platform.ServiceUtil
import org.apache.logging.log4j.LogManager

abstract class ExpeditionaryHardcore {
    companion object {
        const val MOD_ID = "exphardcore"
        const val MOD_NAME = "Expeditionary Hardcore"

        @JvmField
        val LOGGER = LogManager.getLogger(MOD_NAME)

        var isNeoforge = false

        val modCompat: ModCompatChecker = ServiceUtil.findService(ModCompatChecker::class.java)

        @JvmField
        val cydoniaMode: Boolean = loadPropertiesFile("cydonia.properties")["cydoniaMode"]!!.toBoolean()
    }

    fun initialize() {
        LOGGER.info("Initializing")

        ExpeditionaryHardcoreConfigHandler.initConfig()

        RespawnConfigResolver.init()

        initRegistries()

        ExpeditionaryHardcorePackets.registerPacketsC2S()
        ExpeditionaryHardcorePackets.registerPacketsS2C()

        ExpeditionaryHardcoreModEvents.get().initCallbacks()

        initItemGroups()
        registerResourceListeners()

        if (cydoniaMode) LOGGER.info("Cydonia mode enabled")

        LOGGER.info("Initialized!")
    }

    fun initClient() {
        LOGGER.info("Initialized client!")
    }

    abstract fun initItemGroups()
    abstract fun registerResourceListeners()
    // called at mod construction on client dist, not from initClient: on neoforge the
    // reload listener event can fire before client setup does
    abstract fun registerClientResourceListeners()
    abstract fun initRegistries()
}
