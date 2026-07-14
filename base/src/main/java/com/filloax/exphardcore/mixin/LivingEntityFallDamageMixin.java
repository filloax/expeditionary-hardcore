package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.character.quirk.LifeQuirkHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFallDamageMixin {

    @ModifyExpressionValue(
            method = "causeFallDamage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;calculateFallDamage(DF)I")
    )
    private int expeditionaryhardcore$applyQuirkFallDamage(int original) {
        LivingEntity th1s = (LivingEntity) (Object) this;
        if (th1s instanceof ServerPlayer player && original > 0) {
            return LifeQuirkHandler.MixinHelpers.modifyFallDamage(player, original);
        }
        return original;
    }
}
