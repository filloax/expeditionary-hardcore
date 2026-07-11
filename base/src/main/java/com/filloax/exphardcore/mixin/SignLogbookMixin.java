package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems;
import com.filloax.exphardcore.item.ExpeditionersLogbookItem;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// Signing an ExpeditionersLogbookItem should produce our own sealed variant, not vanilla's
// written_book (which would drop out of this mod's item type entirely).
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class SignLogbookMixin {

    @Redirect(
        method = "signBook",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;transmuteCopy(Lnet/minecraft/world/level/ItemLike;)Lnet/minecraft/world/item/ItemStack;")
    )
    private ItemStack exphardcore$signToCustomLogbook(ItemStack carried, ItemLike newItem) {
        if (carried.getItem() instanceof ExpeditionersLogbookItem) {
            return carried.transmuteCopy(ExpeditionaryHardcoreItems.INSTANCE.getSIGNED_EXPEDITIONERS_LOGBOOK());
        }
        return carried.transmuteCopy(newItem);
    }
}
