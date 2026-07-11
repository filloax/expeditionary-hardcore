package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.item.LogbookItem;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityDespawnMixin {

    // ordinal 0 = empty-stack discard; ordinal 1 = age >= 6000 despawn timeout
    @Inject(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;discard()V", ordinal = 1),
        cancellable = true
    )
    private void exphardcore$preventLogbookDespawn(CallbackInfo ci) {
        if (((ItemEntity) (Object) this).getItem().getItem() instanceof LogbookItem) {
            ci.cancel();
        }
    }
}
