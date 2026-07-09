package com.filloax.exphardcore.config

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.teamresourceful.resourcefulconfig.api.loader.Configurator
import com.teamresourceful.resourcefulconfig.api.types.ResourcefulConfig

/**
 * Wrap/contain config initialization to avoid changing the main mod file
 * if the library changes initialization means between versions.
 */
object ExpeditionaryHardcoreConfigHandler {
    private val CONFIGURATOR = Configurator(ExpeditionaryHardcore.MOD_ID)
    var config: ResourcefulConfig? = null
        private set

    fun initConfig() {
        CONFIGURATOR.register(ExpeditionaryHardcoreConfig::class.java)
        config = CONFIGURATOR.getConfig(ExpeditionaryHardcoreConfig::class.java).also { c ->
            c.load { }
        }
    }

    fun saveConfig() {
        config?.save() ?: throw IllegalStateException("No config loaded!")
    }
}
