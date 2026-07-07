package com.filloax.expeditionaryhardcore.mixin;

import com.filloax.expeditionaryhardcore.respawn.RespawnPositionOverride;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ServerPlayer.class)
public abstract class RespawnPointMixin {

    // Runs before vanilla's PlayerSpawnFinder.findSpawn, to pass a replacement starting position
    // for the search (still uses the spawn_radius gamerule)
    @ModifyVariable(method = "adjustSpawnLocation", at = @At("HEAD"), argsOnly = true)
    private BlockPos expeditionaryhardcore$onAdjustSpawnLocation(BlockPos spawnSuggestion, ServerLevel level) {
        var player = (ServerPlayer) (Object) this;

        return RespawnPositionOverride.pickRespawnSuggestion(level, player, spawnSuggestion).orElse(spawnSuggestion);
    }

    // Prevents beds/respawn anchors and other mod features from setting respawn point
    @Inject(method = "setRespawnPosition", at = @At("HEAD"), cancellable = true)
    private void expeditionaryhardcore$onSetRespawnPosition(
            ServerPlayer.RespawnConfig respawnConfig,
            boolean showMessage,
            CallbackInfo ci
    ) {
        ci.cancel();
    }
}
