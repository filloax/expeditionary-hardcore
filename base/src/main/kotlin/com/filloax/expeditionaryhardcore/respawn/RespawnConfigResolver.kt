package com.filloax.expeditionaryhardcore.respawn

import com.filloax.expeditionaryhardcore.ExpeditionaryHardcore
import com.filloax.expeditionaryhardcore.config.ExpeditionaryHardcoreConfigHandler
import com.filloax.expeditionaryhardcore.config.RespawnConfig
import com.filloax.expeditionaryhardcore.utils.id
import com.ruslan.apibalego.http.ApiEntry
import com.ruslan.apibalego.http.ApiEntryHandler
import com.ruslan.apibalego.http.ApiEntryRegistry
import net.minecraft.server.MinecraftServer

object RespawnConfigResolver {
    private var remoteConfig: RespawnConfigDto? = null

    val currentRespawnConfig: RespawnConfigDef
        get() = getRespawnConfig()

    fun init() {
        ApiEntryRegistry.register(
            id("respawn_config"),
            RespawnConfigDto.serializer(),
            object : ApiEntryHandler<RespawnConfigDto> {
                override fun handleApiUpdate(
                    server: MinecraftServer,
                    entries: Collection<ApiEntry<RespawnConfigDto>>
                ) {
                    if (entries.isEmpty()) return
                    if (entries.size > 1) {
                        ExpeditionaryHardcore.LOGGER.warn("Received multiple respawn config entries!")
                    }
                    val lastEntry = entries.last()

                    remoteConfig = lastEntry.details
                }
            }
        )
    }

    fun getRespawnConfig(): RespawnConfigDef {
        val configConfig = loadFromConfig()
        val currentRemoteConfig = remoteConfig?.copy()

        return RespawnConfigDto(
            respawnCenter = currentRemoteConfig?.respawnCenter ?: configConfig.respawnCenter,
            respawnDistribution = currentRemoteConfig?.respawnDistribution ?: configConfig.respawnDistribution,
            respawnRadiusMin = currentRemoteConfig?.respawnRadiusMin ?: configConfig.respawnRadiusMin,
            respawnRadiusMax = currentRemoteConfig?.respawnRadiusMax ?: configConfig.respawnRadiusMax,
            minDistanceFromLastRespawn = currentRemoteConfig?.minDistanceFromLastRespawn ?: configConfig.minDistanceFromLastRespawn,
            respawnDistributionMidpoint = currentRemoteConfig?.respawnDistributionMidpoint ?: configConfig.respawnDistributionMidpoint,
            avoidOceans = currentRemoteConfig?.avoidOceans ?: configConfig.avoidOceans,
            forcedRespawnLocations = currentRemoteConfig?.forcedRespawnLocations,
        )
    }


    private fun loadFromConfig(): RespawnConfigDto {
        if (ExpeditionaryHardcoreConfigHandler.config == null) {
            throw IllegalStateException("Config not initialized!")
        }

        return RespawnConfigDto(
            respawnCenter = RespawnConfig.respawnCenter,
            respawnDistribution = RespawnConfig.respawnDistribution,
            respawnRadiusMin = RespawnConfig.respawnRadiusMin,
            respawnRadiusMax = RespawnConfig.respawnRadiusMax,
            minDistanceFromLastRespawn = RespawnConfig.minDistanceFromLastRespawn,
            respawnDistributionMidpoint = RespawnConfig.respawnDistributionMidpoint,
            avoidOceans = RespawnConfig.avoidOceans,
        )
    }
}