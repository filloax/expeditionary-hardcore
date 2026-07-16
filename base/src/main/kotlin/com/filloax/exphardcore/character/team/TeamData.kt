package com.filloax.exphardcore.character.team

import com.filloax.exphardcore.PERSISTENT_DATA_TEAMS
import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.codec.codec
import com.filloax.fxlib.api.json.UUIDSerializer
import com.filloax.fxlib.api.savedata.FxSavedData
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import kotlinx.serialization.Serializable
import net.minecraft.core.UUIDUtil
import net.minecraft.server.MinecraftServer
import java.util.*


@Serializable
data class TeamData(
    @Serializable(with = UUIDSerializer::class)
    val mainUuid: UUID,
    val memberUuids: List<@Serializable(with = UUIDSerializer::class) UUID> = listOf(),
    val memberLoadoutUsed: Set<@Serializable(with = UUIDSerializer::class) UUID> = setOf(),
)

class ServerTeamsData private constructor(
    teams: List<TeamData> = listOf(),
    invites: Map<UUID, UUID> = mapOf(),
) : FxSavedData<ServerTeamsData>(CODEC) {
    val teams = teams.toMutableList()
    // invitee uuid -> inviting main uuid
    val invites = invites.toMutableMap()

    fun teamOf(uuid: UUID): TeamData? = teams.firstOrNull { it.mainUuid == uuid || uuid in it.memberUuids }

    // Main uuid if the given player is a *secondary* member; null if they are a main or teamless
    fun mainOf(uuid: UUID): UUID? = teamOf(uuid)?.takeIf { uuid in it.memberUuids }?.mainUuid

    fun pendingInviteFor(uuid: UUID): UUID? = invites[uuid]

    fun createInvite(main: UUID, invitee: UUID) {
        invites[invitee] = main
        setDirty()
    }

    fun declineInvite(invitee: UUID) {
        if (invites.remove(invitee) != null) setDirty()
    }

    fun acceptInvite(invitee: UUID): UUID? {
        val main = invites.remove(invitee) ?: return null
        val existing = teams.firstOrNull { it.mainUuid == main }
        if (existing != null) {
            if (invitee !in existing.memberUuids) {
                teams[teams.indexOf(existing)] = existing.copy(memberUuids = existing.memberUuids + invitee)
            }
        } else {
            teams.add(TeamData(main, listOf(invitee)))
        }
        setDirty()
        return main
    }

    fun createOrGetTeamOf(main: UUID): TeamData =
        teams.firstOrNull { it.mainUuid == main } ?: TeamData(main).also(teams::add)

    fun updateTeam(team: TeamData, updater: TeamData.() -> TeamData) = teams.set(
        teams.indexOf(team),
        team.updater()
    )

    // Remove the player from whatever team they belong to (as member or main).
    // Removing a main disbands the whole team.
    fun removeMember(uuid: UUID) {
        val team = teamOf(uuid) ?: return
        if (team.mainUuid == uuid) {
            disband(uuid)
        } else {
            teams[teams.indexOf(team)] = team.copy(memberUuids = team.memberUuids - uuid)
            setDirty()
        }
    }

    fun disband(main: UUID) {
        val removed = teams.removeIf { it.mainUuid == main }
        val invitesCleared = invites.values.removeIf { it == main }
        if (removed || invitesCleared) setDirty()
    }

    companion object {
        val CODEC: Codec<ServerTeamsData> = RecordCodecBuilder.create { builder -> builder.group(
            TeamData.serializer().codec().listOf()
                .optionalFieldOf("teams", listOf()).forGetter(ServerTeamsData::teams),
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, UUIDUtil.STRING_CODEC)
                .optionalFieldOf("invites", mapOf()).forGetter(ServerTeamsData::invites),
        ).apply(builder, ::ServerTeamsData) }

        private val DEF = define(id(PERSISTENT_DATA_TEAMS), ::ServerTeamsData, CODEC)

        internal fun get(server: MinecraftServer): ServerTeamsData {
            return server.loadData(DEF)
        }
    }
}
