package com.filloax.exphardcore.character.team

import com.filloax.exphardcore.config.CydoniaModeConfig
import net.minecraft.server.MinecraftServer
import java.util.UUID

/**
 * Abstract team belonging check so we can later add FTB Teams integration
 */
interface TeamProvider {
    // Main uuid if the given player is a secondary member; null if they are a main or teamless
    fun mainOf(server: MinecraftServer, playerUuid: UUID): UUID?

    fun isSecondaryLoadoutAvailable(server: MinecraftServer, playerUuid: UUID): Boolean
    fun setSecondaryLoadoutUsed(server: MinecraftServer, playerUuid: UUID)
    fun resetAllSecondaryLoadoutUsed(server: MinecraftServer, playerUuid: UUID)
}

object InternalTeamProvider : TeamProvider {
    override fun mainOf(server: MinecraftServer, playerUuid: UUID): UUID? =
        ServerTeamsData.get(server).mainOf(playerUuid)

    override fun isSecondaryLoadoutAvailable(
        server: MinecraftServer,
        playerUuid: UUID
    ): Boolean {
        return ServerTeamsData.get(server).teamOf(playerUuid)?.memberLoadoutUsed?.contains(playerUuid) != true
    }

    override fun setSecondaryLoadoutUsed(
        server: MinecraftServer,
        playerUuid: UUID
    ) {
        val serverData = ServerTeamsData.get(server)
        serverData.teamOf(playerUuid)?.let { teamData ->
            serverData.updateTeam(teamData) {
                copy(memberLoadoutUsed = memberLoadoutUsed + playerUuid)
            }
        }
        serverData.setDirty()
    }

    override fun resetAllSecondaryLoadoutUsed(
        server: MinecraftServer,
        playerUuid: UUID
    ) {
        val serverData = ServerTeamsData.get(server)
        serverData.teamOf(playerUuid)?.let { teamData ->
            serverData.updateTeam(teamData) {
                copy(memberLoadoutUsed = setOf())
            }
        }
        serverData.setDirty()
    }
}

object CydoniaTeamProvider : TeamProvider {
    override fun mainOf(server: MinecraftServer, playerUuid: UUID): UUID? {
        val mainUuid = resolveCydoniaMain(server) ?: return null
        return if (playerUuid == mainUuid) null else mainUuid
    }

    override fun isSecondaryLoadoutAvailable(
        server: MinecraftServer,
        playerUuid: UUID
    ): Boolean {
        return ServerTeamsData.get(server).teamOf(playerUuid)?.memberLoadoutUsed?.contains(playerUuid) != true
    }

    override fun setSecondaryLoadoutUsed(server: MinecraftServer, playerUuid: UUID) {
        val serverData = ServerTeamsData.get(server)
        val mainUuid = resolveCydoniaMain(server) ?: return
        val teamData = serverData.createOrGetTeamOf(mainUuid)
        serverData.updateTeam(teamData) {
            copy(
                memberLoadoutUsed = memberLoadoutUsed + playerUuid,
                memberUuids = memberUuids + playerUuid
            )
        }
        serverData.setDirty()
    }

    override fun resetAllSecondaryLoadoutUsed(
        server: MinecraftServer,
        playerUuid: UUID
    ) {
        val serverData = ServerTeamsData.get(server)
        val mainUuid = resolveCydoniaMain(server) ?: return
        val teamData = serverData.createOrGetTeamOf(mainUuid)
        serverData.updateTeam(teamData) {
            copy(memberLoadoutUsed = setOf(), memberUuids = memberUuids + playerUuid)
        }
        serverData.setDirty()
    }


    private fun resolveCydoniaMain(server: MinecraftServer): UUID? {
        val raw = CydoniaModeConfig.mainPlayer.trim()
        if (raw.isEmpty()) return null
        runCatching { return UUID.fromString(raw) }
        // treat as a player name: online player first, then the offline name cache
        server.playerList.getPlayerByName(raw)?.let { return it.uuid }
        return server.services().nameToIdCache().get(raw).map { it.id }.orElse(null)
    }
}

// TODO (owner = main, members = secondary)
object FtbTeamsProvider : TeamProvider {
    override fun mainOf(server: MinecraftServer, playerUuid: UUID): UUID? {
        TODO("Not yet implemented")
    }

    override fun isSecondaryLoadoutAvailable(
        server: MinecraftServer,
        playerUuid: UUID
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun setSecondaryLoadoutUsed(server: MinecraftServer, playerUuid: UUID) {
        TODO("Not yet implemented")
    }

    override fun resetAllSecondaryLoadoutUsed(
        server: MinecraftServer,
        playerUuid: UUID
    ) {
        TODO("Not yet implemented")
    }
}
