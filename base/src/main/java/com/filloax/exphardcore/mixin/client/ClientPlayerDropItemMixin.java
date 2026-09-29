package com.filloax.exphardcore.mixin.client;

import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig;
import com.filloax.exphardcore.item.LogbookOwner;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MultiPlayerGameMode.class)
public class ClientPlayerDropItemMixin {

    @Inject(method = "dropItem", at = @At("HEAD"), cancellable = true)
    private void exphardcore$preventLogbookDrop(LocalPlayer player, boolean all, CallbackInfo ci) {
        if (!ExpeditionaryHardcoreConfig.preventLogbookDrop) return;
        if (LogbookOwner.isLogbookOwnedBy(player.getInventory().getSelectedItem(), player)) {
            ci.cancel();
        }
    }
}
