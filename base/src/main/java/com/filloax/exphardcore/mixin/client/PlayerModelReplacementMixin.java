package com.filloax.exphardcore.mixin.client;

import com.filloax.exphardcore.client.render.CharacterAvatarRenderer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.player.PlayerModelType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.EnumMap;
import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public class PlayerModelReplacementMixin {
    // ordinal 0 = not mannequin
    @ModifyExpressionValue(
        method = "onResourceManagerReload",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/EntityRenderers;createAvatarRenderers(Lnet/minecraft/client/renderer/entity/EntityRendererProvider$Context;)Ljava/util/Map;",
            ordinal = 0
        )
    )
    private Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> exphardcore$replacePlayerRenderers(
        Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> original,
        @Local EntityRendererProvider.Context context
    ) {
        Map<PlayerModelType, AvatarRenderer<AbstractClientPlayer>> replaced = new EnumMap<>(PlayerModelType.class);
        for (PlayerModelType type : original.keySet()) {
            replaced.put(type, new CharacterAvatarRenderer(context, type == PlayerModelType.SLIM));
        }
        return replaced;
    }
}
