package com.filloax.exphardcore.mixin.client;

import com.filloax.exphardcore.client.QuirkGfx;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EffectsInInventory.class)
public abstract class QuirkEffectsMixin {
    // getEffectName runs right before extractText for the same effect in the
    // extractEffects loop, so this tracks which effect the text belongs to
    @Unique
    private MobEffectInstance expeditionaryhardcore$currentEffect;

    @Inject(method = "getEffectName", at = @At("RETURN"), cancellable = true)
    private void expeditionaryhardcore$quirkName(MobEffectInstance effect, CallbackInfoReturnable<Component> cir) {
        expeditionaryhardcore$currentEffect = effect;
        Component quirkName = QuirkGfx.quirkName(effect);
        if (quirkName != null) {
            cir.setReturnValue(quirkName);
        }
    }

    // all quirks share one marker effect for their icon; swap the vanilla atlas
    // sprite for the current quirk's own icon, loaded as a plain (non-atlas) texture
    @WrapOperation(
            method = "extractEffects",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void expeditionaryhardcore$quirkIcon(
            GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height,
            Operation<Void> original,
            @Local(name = "effect") MobEffectInstance effect
    ) {
        QuirkGfx.QuirkIconInfo icon = QuirkGfx.quirkIcon(effect);
        if (icon.getCustomIcon() != null) {
            graphics.blit(pipeline, icon.getCustomIcon(), x, y, 0.0F, 0.0F, width, height, width, height);
        } else {
            if (icon.getVanillaEffectIcon() != null) {
                sprite = icon.getVanillaEffectIcon();
            }
            original.call(graphics, pipeline, sprite, x, y, width, height);
        }
    }

    // vanilla only shows the tooltip when the text doesn't fit; always show it for
    // the quirk marker so its description is visible on hover
    @Inject(method = "extractText", at = @At("RETURN"))
    private void expeditionaryhardcore$quirkTooltip(
            GuiGraphicsExtractor graphics, Component effectText, Component duration, Font font,
            int x0, int y0, int textureWidth, int yStep, int mouseX, int mouseY, CallbackInfo ci
    ) {
        QuirkGfx.extractTooltip(
                graphics, expeditionaryhardcore$currentEffect, effectText, duration, font,
                x0, y0, textureWidth, yStep, mouseX, mouseY
        );
    }
}
