package com.filloax.exphardcore.character.quirk

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.utils.id
import com.ruslan.apibalego.api.Apibalego
import com.ruslan.apibalego.http.ApiEntry
import com.ruslan.apibalego.http.ApiEntryHandler
import kotlinx.serialization.Serializable
import net.minecraft.server.MinecraftServer

/**
Do not reference if Apibalego is not loaded
 */
object LifeQuirksApibalego : ApiEntryHandler<LifeQuirksApibalego.ApibalegoQuirkConfig> {
    var apiEnabledQuirks: Boolean = true
        private set

    var apiDisabledBuiltins: Boolean = false
        private set

    fun init() {
        if (ExpeditionaryHardcore.modCompat.isApibalegoLoaded) {
            Apibalego.registerApiHandler(id("life_quirks_config"), ApibalegoQuirkConfig.serializer(), LifeQuirksApibalego)
        }
    }

    override fun handleApiUpdate(
        server: MinecraftServer,
        entries: Collection<ApiEntry<ApibalegoQuirkConfig>>
    ) {
        if (entries.isEmpty()) return
        if (entries.size > 1) {
            ExpeditionaryHardcore.LOGGER.warn("Received multiple life quirks config entries! Using last")
        }
        val lastEntry = entries.last()

        apiEnabledQuirks = lastEntry.details!!.enabled
        apiDisabledBuiltins = lastEntry.details!!.disableBuiltins
    }

    @Serializable
    data class ApibalegoQuirkConfig(
        val enabled: Boolean = true,
        val disableBuiltins: Boolean = false,
    )
}