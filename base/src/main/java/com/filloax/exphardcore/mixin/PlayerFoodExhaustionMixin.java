package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.character.quirk.LifeQuirkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// doing things that drain food is handled in player
@Mixin(Player.class)
public abstract class PlayerFoodExhaustionMixin {

    @ModifyVariable(method = "causeFoodExhaustion", at = @At("HEAD"), argsOnly = true)
    private float expeditionaryhardcore$applyQuirkFoodExhaustion(float amount) {
        if ((Object) this instanceof ServerPlayer player && amount > 0) {
            return LifeQuirkHandler.MixinHelpers.modifyFoodExhaustion(player, amount);
        }
        return amount;
    }
}
