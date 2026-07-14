package com.filloax.exphardcore.client

import com.filloax.exphardcore.character.quirk.LifeQuirkClientInfo
import com.filloax.exphardcore.effect.ExpeditionaryHardcoreMobEffects
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffectInstance
import java.util.Optional

/**
 * All quirks share one marker mob effect (ExpeditionaryHardcoreMobEffects.LIFE_QUIRK),
 * so this renders each quirk's own name/description/icon in its place, used from
 * EffectsInInventoryMixin
 */
object QuirkEffectHover {
    private fun quirkInfoFor(effect: MobEffectInstance?): LifeQuirkClientInfo? {
        if (effect == null || effect.effect != ExpeditionaryHardcoreMobEffects.LIFE_QUIRK) return null
        return clientPlayerLifeData?.quirk
    }

    @JvmStatic
    fun quirkName(effect: MobEffectInstance?): Component? {
        val name = quirkInfoFor(effect)?.name ?: return null
        return Component.translatableWithFallback(name, name)
    }

    @JvmStatic
    fun quirkIcon(effect: MobEffectInstance?): Identifier? {
        val icon = quirkInfoFor(effect)?.icon ?: return null
        return icon.withPrefix("textures/quirk_icon/").withSuffix(".png")
    }

    @JvmStatic
    fun extractTooltip(
        graphics: GuiGraphicsExtractor, effect: MobEffectInstance?,
        effectText: Component, duration: Component, font: Font,
        x0: Int, y0: Int, textureWidth: Int, yStep: Int, mouseX: Int, mouseY: Int,
    ) {
        val description = quirkInfoFor(effect)?.description ?: return
        if (mouseX < x0 || mouseX > x0 + textureWidth || mouseY < y0 || mouseY > y0 + yStep) return
        val lines = listOf(
            effectText,
            duration,
            Component.translatableWithFallback(description, description).withStyle(ChatFormatting.GRAY),
        )
        graphics.setTooltipForNextFrame(font, lines, Optional.empty(), mouseX, mouseY)
    }
}
