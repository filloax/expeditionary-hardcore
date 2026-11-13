package com.filloax.exphardcore.client

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig
import net.minecraft.client.Minecraft
import net.minecraft.data.AtlasIds
import net.minecraft.resources.Identifier

// Helper for hud hearts mixin
// TODO: maybe custom heart gfx
object HeartGfx {
    private const val HEART_PREFIX = "hud/heart/"

    private val overrideIds = HashMap<Identifier, Identifier>()

    @JvmStatic
    fun useHardcoreHearts(isHardcore: Boolean): Boolean = isHardcore || clientExpeditionMode

    @JvmStatic
    fun heartSprite(vanillaSprite: Identifier): Identifier {
        if (!clientExpeditionMode) return vanillaSprite
        if (vanillaSprite.namespace != Identifier.DEFAULT_NAMESPACE || !vanillaSprite.path.startsWith(HEART_PREFIX)) {
            return vanillaSprite
        }

        if (ExpeditionaryHardcoreConfig.enableCustomHeartsIcon) {
            val overrideId = overrideIds.getOrPut(vanillaSprite) {
                Identifier.fromNamespaceAndPath(ExpeditionaryHardcore.MOD_ID, vanillaSprite.path)
            }

            // lookup without caching to handle resource reload
            val atlas = Minecraft.getInstance().atlasManager.getAtlasOrThrow(AtlasIds.GUI)
            if (atlas.getSprite(overrideId) !== atlas.missingSprite())
                return overrideId
        }

        return vanillaSprite
    }
}
