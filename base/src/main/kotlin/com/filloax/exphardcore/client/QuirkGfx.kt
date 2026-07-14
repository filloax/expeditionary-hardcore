package com.filloax.exphardcore.client

import com.filloax.exphardcore.character.quirk.LifeQuirkClientInfo
import com.filloax.exphardcore.effect.ExpeditionaryHardcoreMobEffects
import com.mojang.authlib.minecraft.client.MinecraftClient
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.Hud
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import java.util.Optional

/**
 * All quirks share one mob effect (ExpeditionaryHardcoreMobEffects.LIFE_QUIRK),
 * so this renders each quirk's own name/description/icon in its place
 * <br>
 * (Allows dynamic icons via resource/datapack)
 */
object QuirkGfx {
    var vanillaEffectToHudIcon = mapOf<Identifier, Identifier>()

    val QUIRK_ICON_INFO_EMPTY = QuirkIconInfo(null, null)

    fun initVanillaIcons() {
        val registry = BuiltInRegistries.MOB_EFFECT
        vanillaEffectToHudIcon = registry.keySet().associateWith { key ->
            val effect = registry.get(key).orElseThrow()
            Hud.getMobEffectSprite(effect)
        }
    }

    @JvmStatic
    fun quirkName(effect: MobEffectInstance?): Component? {
        val name = quirkInfoFor(effect)?.name ?: return null
        return Component.translatableWithFallback(name, name)
    }

    @JvmStatic
    fun quirkDuration(effect: MobEffectInstance?): Component? {
        quirkInfoFor(effect) ?: return null
        return Component.translatable("effect.exphardcore.life_quirk.duration")
    }

    @JvmStatic
    fun quirkIcon(effect: MobEffectInstance?): QuirkIconInfo {
        val quirkInfo = quirkInfoFor(effect) ?: return QUIRK_ICON_INFO_EMPTY
        val icon = quirkInfo.icon

        if (quirkInfo.iconIsVanillaEffect) {
            val effectIcon = vanillaEffectToHudIcon[icon] ?: return QUIRK_ICON_INFO_EMPTY
            return QuirkIconInfo(vanillaEffectIcon=effectIcon)
        } else {
            val textureId = icon.withPath { "textures/quirk_icon/$it.png" }
            return QuirkIconInfo(customIcon=textureId)
        }
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

    private fun quirkInfoFor(effect: MobEffectInstance?): LifeQuirkClientInfo? {
        if (effect == null || effect.effect != ExpeditionaryHardcoreMobEffects.LIFE_QUIRK) return null
        return clientPlayerLifeData?.quirk
    }


    data class QuirkIconInfo(val customIcon: Identifier? = null, val vanillaEffectIcon: Identifier? = null)
}
