package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig;
import com.filloax.exphardcore.item.LogbookOwner;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class PlayerDropItemMixin {

    @Inject(method = "drop(Z)V", at = @At("HEAD"), cancellable = true)
    private void exphardcore$preventLogbookDrop(boolean dropAll, CallbackInfo ci) {
        if (!ExpeditionaryHardcoreConfig.preventLogbookDrop) return;
        ServerPlayer player = (ServerPlayer) (Object) this;
        if (LogbookOwner.isLogbookOwnedBy(player.getInventory().getSelectedItem(), player)) {
            ci.cancel();
        }
    }
}
