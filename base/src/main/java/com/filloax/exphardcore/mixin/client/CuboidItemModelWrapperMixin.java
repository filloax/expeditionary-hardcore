package com.filloax.exphardcore.mixin.client;

import com.filloax.exphardcore.item.ExpeditionaryHardcoreDataComponents;
import com.filloax.exphardcore.character.PlayerLifeData;
import com.filloax.exphardcore.client.ClientPlayerLifeDataKt;
import com.filloax.exphardcore.item.ExpeditionersLogbookItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Special shine animation for unused logbook
 */
@Mixin(CuboidItemModelWrapper.class)
public class CuboidItemModelWrapperMixin {
    @WrapOperation(
        method = "update",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/CuboidItemModelWrapper;hasSpecialAnimatedTexture(Lnet/minecraft/world/item/ItemStack;)Z")
    )
    private boolean exphardcore$wrapHasSpecialAnimatedTexture(ItemStack item, Operation<Boolean> original) {
        if (item.getItem() instanceof ExpeditionersLogbookItem
                && item.get(ExpeditionaryHardcoreDataComponents.LOGBOOK_OWNER) == null) {
            PlayerLifeData clientLife = ClientPlayerLifeDataKt.getClientPlayerLifeData();
            if (clientLife == null || !clientLife.getDidCreation()) {
                return true;
            }
        }
        return original.call(item);
    }
}
