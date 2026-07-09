package com.filloax.expeditionaryhardcore.respawn

import com.filloax.fxlib.api.json.BlockPosSerializer
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos

// Separate interface because initial idea was having like a proxy that merged remote and config,
// didn't do that in the end but oh well
interface RespawnConfigDef {
    val respawnRadiusMin: Int
    val respawnRadiusMax: Int
    val minDistanceFromLastRespawn: Int
    val respawnCenter: RespawnCenter
    val respawnDistribution: RespawnDistribution
    val respawnDistributionMidpoint: Int
    val avoidOceans: Boolean
    val forcedRespawnLocations: List<BlockPos>?
}

// Used for both config and remote
object RespawnConfigDefaults: RespawnConfigDef {
    override val respawnRadiusMin = 500
    override val respawnRadiusMax = 5000
    override val minDistanceFromLastRespawn = 1000
    override val respawnCenter = RespawnCenter.WORLD_ORIGIN
    override val respawnDistribution = RespawnDistribution.UNIFORM
    override val respawnDistributionMidpoint = -1
    override val avoidOceans = true
    override val forcedRespawnLocations = null
}

@Serializable
data class RespawnConfigDto(
    override val respawnRadiusMin: Int = RespawnConfigDefaults.respawnRadiusMin,
    override val respawnRadiusMax: Int = RespawnConfigDefaults.respawnRadiusMax,
    override val minDistanceFromLastRespawn: Int = RespawnConfigDefaults.minDistanceFromLastRespawn,
    override val respawnCenter: RespawnCenter = RespawnConfigDefaults.respawnCenter,
    override val respawnDistribution: RespawnDistribution = RespawnConfigDefaults.respawnDistribution,
    override val respawnDistributionMidpoint: Int = RespawnConfigDefaults.respawnDistributionMidpoint,
    override val avoidOceans: Boolean = RespawnConfigDefaults.avoidOceans,
    override val forcedRespawnLocations: List<@Serializable(with = BlockPosSerializer::class)BlockPos>? = null,
) : RespawnConfigDef {
    init {
        require(respawnRadiusMin >= 0)
        require(respawnRadiusMax >= respawnRadiusMin)
        require(respawnDistributionMidpoint in respawnRadiusMin..respawnRadiusMax || respawnDistributionMidpoint == -1)
    }
}

enum class RespawnCenter {
    WORLD_ORIGIN,
    STRONGHOLD,
    LAST_RESPAWN_POINT,
}

enum class RespawnDistribution {
    UNIFORM,
    NEARER_MORE_LIKELY,
    FURTHER_MORE_LIKELY,
    MIDPOINT_MORE_LIKELY,
}
