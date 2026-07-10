package com.filloax.exphardcore.mixin.client;

import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig;
import com.filloax.exphardcore.item.LogbookOwner;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public class ClientPlayerDropItemMixin {

    @Inject(method = "drop", at = @At("HEAD"), cancellable = true)
    private void exphardcore$preventLogbookDrop(boolean all, CallbackInfoReturnable<Boolean> cir) {
        if (!ExpeditionaryHardcoreConfig.preventLogbookDrop) return;
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (LogbookOwner.isLogbookOwnedBy(player.getInventory().getSelectedItem(), player)) {
            cir.cancel();
        }
    }
}
