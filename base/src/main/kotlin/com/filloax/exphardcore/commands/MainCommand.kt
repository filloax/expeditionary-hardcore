package com.filloax.exphardcore.commands

import com.filloax.exphardcore.character.CharacterLoadoutHandler
import com.filloax.exphardcore.character.getAllExpeditionLives
import com.filloax.exphardcore.character.getExpeditionLife
import com.filloax.exphardcore.character.getExpeditionLifeOrNull
import com.filloax.exphardcore.character.quirk.LifeQuirkClientInfo
import com.filloax.exphardcore.character.quirk.LifeQuirkDefinitions
import com.filloax.exphardcore.character.quirk.LifeQuirkHandler
import com.filloax.exphardcore.character.refreshExpeditionData
import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.minecraft.ChatFormatting
import net.minecraft.commands.CommandBuildContext
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands.*
import net.minecraft.commands.SharedSuggestionProvider
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.IdentifierArgument
import net.minecraft.resources.Identifier
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions
import kotlin.random.Random


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
                .then(literal("history")
                    .then(literal("player")
                        .then(argument("player", EntityArgument.player())
                            .executes { ctx -> showHistory(ctx.source, EntityArgument.getPlayer(ctx, "player")) }
                        )
                    )
                    .executes { ctx -> showHistory(ctx.source, null) }
                )
                .then(literal("loadout")
                    .requires(hasPermission(LEVEL_GAMEMASTERS))
                    .then(literal("player")
                        .then(argument("player", EntityArgument.player())
                            .executes { ctx -> giveLoadout(ctx.source, EntityArgument.getPlayer(ctx, "player")) }
                        )
                    )
                    .executes { ctx -> giveLoadout(ctx.source, null) }
                )
                .then(literal("quirk")
                    .requires(hasPermission(LEVEL_GAMEMASTERS))
                    .then(literal("player")
                        .then(argument("player", EntityArgument.player())
                            .executes { ctx -> showQuirk(ctx.source, EntityArgument.getPlayer(ctx, "player")) }
                        )
                    )
                    .then(literal("set")
                        .then(argument("quirk", IdentifierArgument.id())
                            .suggests { _, builder -> SharedSuggestionProvider.suggestResource(LifeQuirkDefinitions.all.keys, builder) }
                            .then(literal("player")
                                .then(argument("player", EntityArgument.player())
                                    .executes { ctx -> setQuirk(ctx.source, EntityArgument.getPlayer(ctx, "player"), IdentifierArgument.getId(ctx, "quirk")) }
                                )
                            )
                            .executes { ctx -> setQuirk(ctx.source, null, IdentifierArgument.getId(ctx, "quirk")) }
                        )
                    )
                    .then(literal("clear")
                        .then(literal("player")
                            .then(argument("player", EntityArgument.player())
                                .executes { ctx -> clearQuirk(ctx.source, EntityArgument.getPlayer(ctx, "player")) }
                            )
                        )
                        .executes { ctx -> clearQuirk(ctx.source, null) }
                    )
                    .then(literal("reroll")
                        .then(literal("player")
                            .then(argument("player", EntityArgument.player())
                                .executes { ctx -> rerollQuirk(ctx.source, EntityArgument.getPlayer(ctx, "player")) }
                            )
                        )
                        .executes { ctx -> rerollQuirk(ctx.source, null) }
                    )
                    .executes { ctx -> showQuirk(ctx.source, null) }
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
        player.refreshExpeditionData()

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

    private fun showHistory(source: CommandSourceStack, player: ServerPlayer?): Int {
        val sourcePlayer = source.playerOrException
        val targetPlayer = player ?: sourcePlayer
        val isPersonal = targetPlayer == sourcePlayer

        if (!isPersonal && !source.permissions().hasPermission(Permissions.COMMANDS_MODERATOR)) {
            source.sendFailure(Component.translatable("exphardcore.commands.exphardcore.history_refuse"))
            return 0
        }

        val lives = targetPlayer.getAllExpeditionLives()

        if (lives.isEmpty()) {
            source.sendFailure(Component.translatable("exphardcore.commands.exphardcore.info_no_life"))
            return 0
        }

        lives.forEachIndexed { index, life ->
            val pos = life.spawnPoint
            val coordComponent = Component.translatable("chat.coordinates", pos.x, pos.y, pos.z)
                .withStyle { style ->
                    style
                        .withColor(ChatFormatting.GREEN)
                        .withClickEvent(ClickEvent.SuggestCommand("/tp @s ${pos.x} ${pos.y} ${pos.z}"))
                        .withHoverEvent(HoverEvent.ShowText(Component.translatable("chat.coordinates.tooltip")))
                }
            val message = Component.translatable(
                "exphardcore.commands.exphardcore.history_entry",
                index + 1, life.name ?: "?",
            ).append(coordComponent)
            source.sendSystemMessage(message)
        }

        return lives.size
    }

    private fun giveLoadout(source: CommandSourceStack, player: ServerPlayer?): Int {
        val sourcePlayer = source.playerOrException
        val targetPlayer = player ?: sourcePlayer

        val random = targetPlayer.random

        CharacterLoadoutHandler.giveRandomLoadout(targetPlayer, Random(random.nextLong()))

        source.sendSystemMessage(Component.translatable(
            "exphardcore.commands.exphardcore.loadout_ok", targetPlayer.name,
        ))

        return 1
    }

    private fun showQuirk(source: CommandSourceStack, player: ServerPlayer?): Int {
        val targetPlayer = player ?: source.playerOrException
        val quirk = targetPlayer.getExpeditionLifeOrNull()?.quirk

        if (quirk == null) {
            source.sendSystemMessage(Component.translatable(
                "exphardcore.commands.exphardcore.quirk_none", targetPlayer.name,
            ))
            return 0
        }

        source.sendSystemMessage(Component.translatable(
            "exphardcore.commands.exphardcore.quirk_current",
            targetPlayer.name, quirkDisplayName(quirk), quirk.quirkId.toString(),
        ))
        return 1
    }

    private fun setQuirk(source: CommandSourceStack, player: ServerPlayer?, quirkId: Identifier): Int {
        val targetPlayer = player ?: source.playerOrException

        if (!LifeQuirkHandler.setQuirk(targetPlayer, quirkId)) {
            source.sendFailure(Component.translatable(
                "exphardcore.commands.exphardcore.quirk_unknown", quirkId.toString(),
            ))
            return 0
        }

        source.sendSuccess({ Component.translatable(
            "exphardcore.commands.exphardcore.quirk_set", targetPlayer.name, quirkId.toString(),
        ) }, true)
        return 1
    }

    private fun clearQuirk(source: CommandSourceStack, player: ServerPlayer?): Int {
        val targetPlayer = player ?: source.playerOrException

        LifeQuirkHandler.setQuirk(targetPlayer, null)

        source.sendSuccess({ Component.translatable(
            "exphardcore.commands.exphardcore.quirk_cleared", targetPlayer.name,
        ) }, true)
        return 1
    }

    private fun rerollQuirk(source: CommandSourceStack, player: ServerPlayer?): Int {
        val targetPlayer = player ?: source.playerOrException

        val quirkId = LifeQuirkHandler.rerollQuirk(targetPlayer, Random(targetPlayer.random.nextLong()))

        source.sendSuccess({ Component.translatable(
            "exphardcore.commands.exphardcore.quirk_rerolled",
            targetPlayer.name, quirkId?.toString() ?: "none",
        ) }, true)
        return 1
    }

    private fun quirkDisplayName(quirk: LifeQuirkClientInfo): Component =
        quirk.name?.let { Component.translatableWithFallback(it, it) }
            ?: Component.literal(quirk.quirkId.toString())
}
