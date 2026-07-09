package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.ExpeditionaryHardcore;
import com.filloax.exphardcore.character.LifeHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import com.llamalad7.mixinextras.sugar.Local;


@Mixin(PlayerList.class)
public abstract class PlayerRespawnMixin {

    @Inject(method = "respawn", at = @At("RETURN"))
    private void expeditionaryhardcore$onRespawn(
            ServerPlayer oldPlayer,
            boolean keepAllPlayerData,
            Entity.RemovalReason removalReason,
            CallbackInfoReturnable<ServerPlayer> cir,
            @Local(name = "player") ServerPlayer player
    ) {
        ExpeditionaryHardcore.LOGGER.debug("Player respawned {} at {}", player, player.blockPosition());

        LifeHandler.newLife(player, player.blockPosition());
    }
}
