package com.filloax.exphardcore.client.compat

import com.filloax.exphardcore.ExpeditionaryHardcore
import net.irisshaders.iris.api.v0.IrisApi
import kotlin.math.min
import kotlin.math.roundToInt

object IrisCompat {
    private var loggedError = false

    private var fogDistance: Float? = null

    val isUsingShaders: Boolean
        get() {
            if (!ExpeditionaryHardcore.modCompat.isIrisLoaded) return false
            return try {
                IrisApi.getInstance().isShaderPackInUse
            } catch (e: Throwable) {
                logError("check shaders", e)
                false
            }
        }

    /**
     * Limits the shader pack fog to [distance] blocks, null to reset.
     * Overrides Iris internal value dhRenderDistance, should work with DH-compatible packs
     */
    fun setShaderFogDistance(distance: Float?) {
        fogDistance = distance
    }

    @JvmStatic
    fun overrideDhRenderDistance(original: Int): Int {
        val distance = fogDistance ?: return original
        return min(original, distance.roundToInt().coerceAtLeast(1))
    }

    // avoid spam of errors in case iris compat breaks
    private fun logError(action: String, e: Throwable) {
        if (!loggedError) {
            loggedError = true
            ExpeditionaryHardcore.LOGGER.error("Failed to $action with Iris", e)
        }
    }
}
