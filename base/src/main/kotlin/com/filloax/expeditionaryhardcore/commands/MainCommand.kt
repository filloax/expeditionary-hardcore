package com.filloax.expeditionaryhardcore.commands

import com.filloax.expeditionaryhardcore.character.getExpeditionLife
import com.filloax.expeditionaryhardcore.character.getExpeditionLifeOrNull
import com.filloax.expeditionaryhardcore.character.refreshExpeditionName
import com.filloax.expeditionaryhardcore.config.ExpeditionaryHardcoreConfig
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions


/**
 * Utility commands for the mod.
 */
object MainCommand {
    fun register(dispatcher: CommandDispatcher<CommandSourceStack>, registryAccess: CommandBuildContext, environment: CommandSelection) {
        dispatcher.register(
            literal("exphardcore")
                .then(literal("name")
                    .then(literal("player")
                        .then(argument("player", EntityArgument.player())
                            .then(argument("name", StringArgumentType.string())
                                .executes { ctx -> setName(ctx.source, EntityArgument.getPlayer(ctx, "player"), StringArgumentType.getString(ctx, "name")) }
                            )
                        )
                    )
                    .then(argument("name", StringArgumentType.string())
                        .executes { ctx -> setName(ctx.source, null, StringArgumentType.getString(ctx, "name")) }
                    )
                )
                .then(literal("info")
                    .executes { ctx -> showInfo(ctx.source) }
                )
        )
    }

    private fun setName(source: CommandSourceStack, player: ServerPlayer?, name: String): Int {
        val sourcePlayer = source.playerOrException
        val player = player ?: sourcePlayer
        val isPersonal = player == sourcePlayer

        if (!source.permissions().hasPermission((Permissions.COMMANDS_MODERATOR))
            && !isPersonal
        ) {
            source.sendFailure(Component.translatable("exphardcore.commands.exphardcore.name_set_refuse"))
            return 0
        }

        val playerLifeData = player.getExpeditionLife()

        if (!source.permissions().hasPermission((Permissions.COMMANDS_MODERATOR)) && ExpeditionaryHardcoreConfig.allowChangingName) {
            source.sendFailure(Component.translatable("exphardcore.commands.exphardcore.name_change_refuse"))
            return 0
        }

        playerLifeData.name = name
        player.refreshExpeditionName()

        source.sendSuccess(
            {
                Component.translatable(if (isPersonal)
                    "exphardcore.commands.exphardcore.name_set_personal"
                else "exphardcore.commands.exphardcore.name_set", name)
                .withStyle(ChatFormatting.ITALIC)
            },
            true,
        )
        return 1
    }

    private fun showInfo(source: CommandSourceStack): Int {
        val player = source.playerOrException
        val playerLifeData = player.getExpeditionLifeOrNull()

        if (playerLifeData == null) {
            source.sendFailure(Component.translatable("exphardcore.commands.exphardcore.info_no_life"))
            return 0
        }


        source.sendSystemMessage(Component.translatable(
            "exphardcore.commands.exphardcore.info_name", playerLifeData.name ?: "null",
        ))

        return 1
    }
}
