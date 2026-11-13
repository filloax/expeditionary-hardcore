package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.character.quirk.LifeQuirkHandler;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// Health regen draining food is handled in FoodData
@Mixin(FoodData.class)
public abstract class FoodDataExhaustionMixin {

    @ModifyArg(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;addExhaustion(F)V")
    )
    private float expeditionaryhardcore$applyQuirkRegenExhaustion(float amount, @Local(argsOnly = true) ServerPlayer player) {
        return LifeQuirkHandler.MixinHelpers.modifyFoodExhaustion(player, amount);
    }
}
