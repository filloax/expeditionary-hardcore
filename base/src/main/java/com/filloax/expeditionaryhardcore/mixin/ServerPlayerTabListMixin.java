package com.filloax.expeditionaryhardcore.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


// Use display name explicitly so our thing works here too
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTabListMixin {

    @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
    private void expeditionaryhardcore$onGetTabListDisplayName(CallbackInfoReturnable<Component> cir) {
        var player = (ServerPlayer) (Object) this;
        cir.setReturnValue(player.getDisplayName());
    }
}
