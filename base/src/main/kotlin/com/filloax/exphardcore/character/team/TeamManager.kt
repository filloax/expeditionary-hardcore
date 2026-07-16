package com.filloax.exphardcore.character.team

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.character.CharacterLoadoutHandler
import com.filloax.exphardcore.config.CydoniaModeConfig
import com.filloax.exphardcore.config.MultiplayerConfig
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

/**
 * Normally: teams managed by players
 *
 * Cydonia mode: one team for whole server with main configured
 */
object TeamManager {
    fun multiplayerEnabled(): Boolean =
        ExpeditionaryHardcore.cydoniaMode || MultiplayerConfig.enableTeams

    // Main uuid if the given player is a secondary member; null if they are a main or teamless
    fun mainUuidFor(server: MinecraftServer, playerUuid: UUID): UUID? {
        return activeProvider(server).mainOf(server, playerUuid)
    }

    fun isSecondary(server: MinecraftServer, player: ServerPlayer): Boolean =
        mainUuidFor(server, player.uuid)?.let { it != player.uuid } ?: false

    fun isSecondaryLoadoutAvailable(server: MinecraftServer, player: ServerPlayer) =
        activeProvider(server).isSecondaryLoadoutAvailable(server, player.uuid)
    fun setSecondaryLoadoutUsed(server: MinecraftServer, player: ServerPlayer) =
        activeProvider(server).setSecondaryLoadoutUsed(server, player.uuid)
    fun resetAllSecondaryLoadoutUsed(server: MinecraftServer, player: ServerPlayer) =
        activeProvider(server).resetAllSecondaryLoadoutUsed(server, player.uuid)

    private fun activeProvider(server: MinecraftServer): TeamProvider =
        if (ExpeditionaryHardcore.cydoniaMode) CydoniaTeamProvider
//        else if (ExpeditionaryHardcore.modCompat.isFtbTeamsLoaded) FtbTeamsProvider
        else InternalTeamProvider
}
