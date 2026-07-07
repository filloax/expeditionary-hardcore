package com.filloax.expeditionaryhardcore.mixin;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.commands.SetSpawnCommand;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;


@Mixin(SetSpawnCommand.class)
public abstract class SetSpawnCommandMixin {

    @Inject(method = "setSpawn", at = @At("HEAD"), cancellable = true)
    private static void expeditionaryhardcore$onSetSpawn(
            CommandSourceStack source,
            Collection<ServerPlayer> targets,
            BlockPos pos,
            Coordinates rotation,
            CallbackInfoReturnable<Integer> cir
    ) {
        source.sendFailure(Component.translatable("exphardcore.commands.spawnpoint.disabled"));
        cir.setReturnValue(0);
    }
}
