package com.filloax.exphardcore.client.compat

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig
import com.filloax.exphardcore.config.RespawnConfig
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
 * Temporarily disables DH rendering after respawning, to avoid spawning inside LODs
 * - without shaders: turns off dh, then attempts lerping fog back to original distance (may not work depending on dh issue fixes)
 * - with shaders: alters iris internal dhRenderDistance var to reduce dist for duration, then lerps back
 */
object DistantHorizonsCompat {
    private var loggedError = false

    private var elapsedTicks = -1
    private var disabledTicks = 0
    private var fadeTicks = 0
    private var usingShaders = false

    private const val FADE_IN_PCT_TIME = .33f
    // how much more than vanilla render distance fog dist is set
    // (as DH normally makes you use a lower vanilla distance
    // than normal, to compensate)
    private const val HIDDEN_FOG_DIST_MULT = 1.35f

    fun init() {
        ExpeditionaryHardcore.LOGGER.info("Distant Horizons detected, enabling compatibility")
        tryDh("init") {
            DhApiEventRegister.on(DhApiBeforeRenderEvent::class.java, FadeRenderHandler)
        }
    }

    fun disableDistantHorizonsTemporarily() {
        val seconds = RespawnConfig.respawnDistantHorizonsDisableSeconds
        if (seconds <= 0) return

        tryDh("disable") {
            usingShaders = IrisCompat.isUsingShaders
            if (usingShaders) {
                IrisCompat.setShaderFogDistance(hiddenFogDistanceBlocks())
            } else {
                DhApi.Delayed.configs.graphics().renderingMode().setValue(EDhApiRendererMode.DISABLED, ExpeditionaryHardcore.MOD_NAME)
                applyFog(0f)
            }
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
            if (!usingShaders) tryDh("reenable") {
                applyFog(0f)
                DhApi.Delayed.configs.graphics().renderingMode().clearValue()
            }
        } else if (elapsedTicks >= disabledTicks + fadeTicks) {
            ExpeditionaryHardcore.LOGGER.info("Finish fade")
            elapsedTicks = -1
            if (usingShaders) {
                IrisCompat.setShaderFogDistance(null)
            } else tryDh("finish fade") {
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
            val t = ((elapsedTicks - disabledTicks + partialTick) / fadeTicks).coerceIn(0f, 1f)
            tryDh("fade") {
                if (usingShaders) {
                    IrisCompat.setShaderFogDistance(Mth.lerp(t, hiddenFogDistanceBlocks(), dhRenderDistanceBlocks()))
                } else {
                    applyFog(t)
                }
            }
        }
    }

    // 0 fully hidden - 1 base config
    private fun applyFog(t: Float) {
        // fog starts disappearing when at 50%
        val thickT = ((t - 0.5f) * 2f).coerceIn(0f, 1f)

        val fog = DhApi.Delayed.configs.graphics().fog()
        val farFog = fog.farFog()
        fog.enableDhFog().setValue(true, ExpeditionaryHardcore.MOD_NAME)
        // start distance is relative to DH render distance
        farFog.farFogStartDistance().lerpToBase(hiddenFogDistanceBlocks() / dhRenderDistanceBlocks(), t)
        farFog.farFogMinThickness().lerpToBase(1f, thickT)
        farFog.farFogMaxThickness().lerpToBase(1f, thickT)
    }

    private fun hiddenFogDistanceBlocks() = Minecraft.getInstance().options.effectiveRenderDistance * 16f * HIDDEN_FOG_DIST_MULT

    private fun dhRenderDistanceBlocks() = DhApi.Delayed.configs.graphics().chunkRenderDistance().value * 16f

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
