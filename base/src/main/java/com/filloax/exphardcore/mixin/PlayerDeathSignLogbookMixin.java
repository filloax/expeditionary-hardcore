package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.character.LifeHandler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerDeathSignLogbookMixin {

    @Inject(method = "dropEquipment", at = @At("HEAD"))
    private void exphardcore$signLogbookOnDeath(ServerLevel level, CallbackInfo ci) {
        LifeHandler.signLogbookOnDeath((net.minecraft.server.level.ServerPlayer) (Object) this);
    }
}
