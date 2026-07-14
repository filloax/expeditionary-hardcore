package com.filloax.exphardcore

import com.filloax.exphardcore.character.LifeHandler
import com.filloax.exphardcore.character.quirk.LifeQuirkHandler
import com.filloax.exphardcore.commands.MainCommand
import com.filloax.fxlib.api.platform.ServiceUtil
import com.filloax.fxlib.platform.ServerEvent
import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.CommandSelection
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/**
 * Loader-agnostic event hooks for the mod, resolved per-platform via ServiceLoader.
 * Mirrors ApiBalegoModEvents. Wire actual reactions in initCallbacks().
 */
abstract class ExpeditionaryHardcoreModEvents {
    companion object {
        fun get(): ExpeditionaryHardcoreModEvents = ServiceUtil.findService(ExpeditionaryHardcoreModEvents::class.java)
    }

    fun initCallbacks() {
        onRegisterCommands { dispatcher, ctx, selection ->
            MainCommand.register(dispatcher, ctx, selection)
        }
        onPlayerServerJoin { player ->
            LifeHandler.Callbacks.onPlayerServerJoin(player)
            LifeQuirkHandler.Callbacks.onPlayerServerJoin(player)
        }
        onStartServerTick { server ->
            LifeQuirkHandler.Callbacks.onStartServerTick(server)
        }
    }

    abstract fun onServerStarting(event: ServerEvent)
    abstract fun onServerStopping(event: ServerEvent)
    abstract fun onServerLevelLoad(event: (MinecraftServer, ServerLevel) -> Unit)
    abstract fun onStartServerTick(event: ServerEvent)
    abstract fun onPlayerServerJoin(event: (player: ServerPlayer) -> Unit)
    abstract fun onRegisterCommands(event: (dispatcher: CommandDispatcher<CommandSourceStack>, ctx: CommandBuildContext, selection: CommandSelection) -> Unit)
}
