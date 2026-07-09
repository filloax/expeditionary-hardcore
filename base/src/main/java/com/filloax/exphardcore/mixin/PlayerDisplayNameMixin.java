package com.filloax.exphardcore.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


// applies to chat and death messages
@Mixin(Player.class)
public abstract class PlayerDisplayNameMixin {

    @Inject(method = "getName", at = @At("RETURN"), cancellable = true)
    private void expeditionaryhardcore$onGetName(CallbackInfoReturnable<Component> cir) {
        var player = (Player) (Object) this;
        Component nickname = player.getCustomName();

        if (nickname != null) {
            cir.setReturnValue(nickname.copy()
                    .append(" [")
                    .append(player.getGameProfile().name())
                    .append("]"));
        }
    }
}
