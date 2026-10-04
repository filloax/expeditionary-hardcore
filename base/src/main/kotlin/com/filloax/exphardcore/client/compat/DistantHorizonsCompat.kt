package com.filloax.exphardcore.client.compat

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig
import com.seibel.distanthorizons.api.DhApi
import com.seibel.distanthorizons.api.enums.rendering.EDhApiRendererMode
import com.seibel.distanthorizons.api.interfaces.config.IDhApiConfigValue
import com.seibel.distanthorizons.api.methods.events.DhApiEventRegister
import com.seibel.distanthorizons.api.methods.events.abstractEvents.DhApiBeforeRenderEvent
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiRenderParam
import net.minecraft.SharedConstants
import net.minecraft.client.Minecraft
import net.minecraft.util.Mth

/**
 * Temporarily disables DH rendering after respawning, to avoid spawning inside LODs,
 * then reenables it covered by opaque fog and fades the fog back to the user's settings.
 */
object DistantHorizonsCompat {
    private var loggedError = false

    private var elapsedTicks = -1
    private var disabledTicks = 0
    private var fadeTicks = 0

    private const val FADE_IN_PCT_TIME = .33f

    fun init() {
        ExpeditionaryHardcore.LOGGER.info("Distant Horizons detected, enabling compatibility")
        tryDh("init") {
            DhApiEventRegister.on(DhApiBeforeRenderEvent::class.java, FadeRenderHandler)
        }
    }

    fun disableDistantHorizonsTemporarily() {
        val seconds = ExpeditionaryHardcoreConfig.respawnDistantHorizonsDisableSeconds
        if (seconds <= 0) return

        tryDh("disable") {
            val graphics = DhApi.Delayed.configs.graphics()

            graphics.renderingMode().setValue(EDhApiRendererMode.DISABLED, ExpeditionaryHardcore.MOD_NAME)
            resetFog()
            disabledTicks = seconds * SharedConstants.TICKS_PER_SECOND
            fadeTicks = (disabledTicks * FADE_IN_PCT_TIME).toInt().coerceAtLeast(1)
            elapsedTicks = 0
            ExpeditionaryHardcore.LOGGER.info("Disabled Distant Horizons for {} seconds", seconds)
        }
    }

    fun onClientTick(client: Minecraft) {
        // count ticks except paused ticks, <0=disabled
        if (elapsedTicks < 0 || client.isPaused) return

        elapsedTicks++

        if (elapsedTicks == disabledTicks) {
            ExpeditionaryHardcore.LOGGER.info("Reenabling Distant Horizons")
            tryDh("reenable") {
                applyFog(0f)
                DhApi.Delayed.configs.graphics().renderingMode().clearValue()
            }
        } else if (elapsedTicks >= disabledTicks + fadeTicks) {
            ExpeditionaryHardcore.LOGGER.info("Finish fade")
            elapsedTicks = -1
            tryDh("finish fade") {
                resetFog()
            }
        }
    }

    // original idea was going on ticks, but it would be off sync with render frames
    private object FadeRenderHandler : DhApiBeforeRenderEvent() {
        val client = Minecraft.getInstance()

        override fun beforeRender(event: DhApiCancelableEventParam<DhApiRenderParam>) {
            if (elapsedTicks < disabledTicks) return

            val partialTick = client.deltaTracker.getGameTimeDeltaPartialTick(false)
            val t = (elapsedTicks - disabledTicks + partialTick) / fadeTicks
            tryDh("fade") { applyFog(t.coerceIn(0f, 1f)) }
        }
    }

    // 0 fully hidden - 1 base config
    private fun applyFog(t: Float) {
        // fog starts disappearing when at 50%
        val thickT = ((t - 0.5f) * 2f).coerceIn(0f, 1f)

        val fog = DhApi.Delayed.configs.graphics().fog()
        val farFog = fog.farFog()
        fog.enableDhFog().setValue(true, ExpeditionaryHardcore.MOD_NAME)
        farFog.farFogStartDistance().lerpToBase(0f, t)
        farFog.farFogMinThickness().lerpToBase(1f, thickT)
        farFog.farFogMaxThickness().lerpToBase(1f, thickT)
    }

    // this sets back to config value, doesn't change the actual config
    private fun resetFog() {
        val fog = DhApi.Delayed.configs.graphics().fog()
        val farFog = fog.farFog()
        fog.enableDhFog().clearValue()
        farFog.farFogStartDistance().clearValue()
        farFog.farFogMinThickness().clearValue()
        farFog.farFogMaxThickness().clearValue()
    }

    private fun IDhApiConfigValue<Float>.lerpToBase(from: Float, t: Float) {
        setValue(Mth.lerp(t, from, trueValue), ExpeditionaryHardcore.MOD_NAME)
    }

    private inline fun tryDh(action: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            if (!loggedError) {
                loggedError = true
                ExpeditionaryHardcore.LOGGER.error("Failed to $action Distant Horizons", e)
            }
        }
    }
}
