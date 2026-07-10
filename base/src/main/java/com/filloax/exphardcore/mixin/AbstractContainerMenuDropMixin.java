package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig;
import com.filloax.exphardcore.item.LogbookOwner;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuDropMixin {

    @Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
    private void exphardcore$preventLogbookActions(int slot, int button, ContainerInput input, Player player, CallbackInfo ci) {
        if (!ExpeditionaryHardcoreConfig.preventLogbookDrop) return;

        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;

        // Drag out of inventory (release cursor item outside the window)
        if (slot == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE
                && input == ContainerInput.PICKUP
                && LogbookOwner.isLogbookOwnedBy(menu.getCarried(), player)) {
            ci.cancel();
            return;
        }

        if (slot < 0) return;

        // Q pressed while hovering over a slot, or shift-click out of player inventory
        if ((input == ContainerInput.THROW || input == ContainerInput.QUICK_MOVE)
                && menu.getSlot(slot).container instanceof Inventory
                && LogbookOwner.isLogbookOwnedBy(menu.getSlot(slot).getItem(), player)) {
            ci.cancel();
            return;
        }

        // Placing cursor item (logbook) into a non-player-inventory slot
        if (input == ContainerInput.PICKUP
                && !(menu.getSlot(slot).container instanceof Inventory)
                && LogbookOwner.isLogbookOwnedBy(menu.getCarried(), player)) {
            ci.cancel();
        }
    }
}
