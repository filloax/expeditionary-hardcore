package com.filloax.exphardcore.mixin.client;

import com.filloax.exphardcore.client.HeartGfx;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Hud.class)
public abstract class HudHeartsMixin {
    @WrapOperation(
            method = "extractHearts",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/LevelData;isHardcore()Z")
    )
    private boolean expeditionaryhardcore$hardcoreHearts(LevelData levelData, Operation<Boolean> original) {
        return HeartGfx.useHardcoreHearts(original.call(levelData));
    }

    // wrapping the blit instead of HeartType.getSprite, as HeartType is not accessible
    @WrapOperation(
            method = "extractHeart",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    )
    private void expeditionaryhardcore$heartSprite(
            GuiGraphicsExtractor graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int width, int height,
            Operation<Void> original
    ) {
        original.call(graphics, pipeline, HeartGfx.heartSprite(sprite), x, y, width, height);
    }
}
