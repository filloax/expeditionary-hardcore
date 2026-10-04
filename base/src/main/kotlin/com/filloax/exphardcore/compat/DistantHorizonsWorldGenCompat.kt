package com.filloax.exphardcore.compat

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.seibel.distanthorizons.api.DhApi
import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiGeneratorPlan
import net.minecraft.server.MinecraftServer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

object DistantHorizonsWorldGenCompat {
    // DH checks the generator plan on its own thread every more or less 20ms
    private const val DH_TICK_MS = 20

    private var loggedError = false
    private var restartPending = false

    /**
     * Restarts DH world generation after a respawn, otherwise it wastes time finishing to
     * generate the previous area
     */
    fun restartWorldGen(server: MinecraftServer) {
        if (!server.isSingleplayer) {
            return
        }

        server.execute {
            if (restartPending) return@execute

            tryDh("stop world gen") {
                val time = DH_TICK_MS * 2L
                ExpeditionaryHardcore.LOGGER.info("Restart DH world gen (stop for {} seconds then restart)", time * .001)
                val planConfig = DhApi.Delayed.configs.worldGenerator().GeneratorPlan()
                if (!planConfig.value.generationEnabled) return@tryDh

                planConfig.setValue(EDhApiGeneratorPlan.DISABLED, ExpeditionaryHardcore.MOD_NAME)
                restartPending = true

                // wait 2 DH "ticks" so it picks up the change and stops tasks
                CompletableFuture.delayedExecutor(time, TimeUnit.MILLISECONDS).execute {
                    server.execute {
                        restartPending = false
                        tryDh("restart world gen") { planConfig.clearValue() }
                    }
                }
            }
        }
    }

    private inline fun tryDh(action: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            if (!loggedError) {
                loggedError = true
                ExpeditionaryHardcore.LOGGER.error("Failed to $action for Distant Horizons", e)
            }
        }
    }
}
