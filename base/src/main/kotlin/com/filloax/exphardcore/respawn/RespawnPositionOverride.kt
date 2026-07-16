package com.filloax.exphardcore.respawn

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.character.ServerAllPlayersLifeData
import com.filloax.exphardcore.character.getAllExpeditionLives
import com.filloax.exphardcore.character.getExpeditionLifeOrNull
import com.filloax.exphardcore.character.team.TeamManager
import com.filloax.exphardcore.config.MultiplayerConfig
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.tags.BiomeTags
import net.minecraft.util.Mth
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement
import java.util.Optional
import java.util.Optional.*
import java.util.UUID
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

object RespawnPositionOverride {
    /**
     * Sets the respawn suggestion (if any), meaning the replacement for world spawn point to use
     * as origin for search (will then still use spawn_radius gamerule, etc.)
     */
    @JvmStatic
    fun pickRespawnSuggestion(level: ServerLevel, player: ServerPlayer, vanillaSuggestion: BlockPos): Optional<BlockPos> {
        val provider = level.providerFor(player)
        val result = provider.getRespawnPosition()

        ExpeditionaryHardcore.LOGGER.info("Respawn position override for player $player: $result")

        return ofNullable(result)
    }

    private fun ServerLevel.providerFor(player: ServerPlayer): IRespawnPositionProvider {
        val config = RespawnConfigResolver.currentRespawnConfig

        // Team secondary players respawn near their main player instead of the normal center
        if (TeamManager.multiplayerEnabled()) {
            val mainUuid = TeamManager.mainUuidFor(server, player.uuid)
            if (mainUuid != null && mainUuid != player.uuid) {
                return TeammateRespawnProvider(this, player, mainUuid)
            }
        }

        val baseProvider = RespawnPositionProvider(this, player, config)
        return RespawnPositionConditionsProvider(this, player, baseProvider, config=config)
    }
}

fun interface IRespawnPositionProvider {
    fun getRespawnPosition(): BlockPos?
}

/*
// Use for testing
open class SimpleRespawnPositionProvider(
    val level: ServerLevel,
    val player: ServerPlayer,
) : IRespawnPositionProvider {
    // TODO: seed-based and persistent memory of respawns for seed-baseness
    protected val random = Random

    override fun getRespawnPosition(): BlockPos? {
        val center = getSearchCenter()
        val dist = random.nextDouble(radius.toDouble())
        val angle = random.nextDouble(2.0 * Math.PI)
        val dx = (dist * Mth.cos(angle)).toInt()
        val dz = (dist * Mth.sin(angle)).toInt()

        return center.offset(dx, 0, dz)
    }

    protected open val radius = 10000 // todo config, website, etc.

    protected open fun getSearchCenter(): BlockPos {
        // for simple, just return world spawn
        return level.respawnData.globalPos.pos
    }
}
*/

class RespawnPositionProvider(
    val level: ServerLevel,
    val player: ServerPlayer,
    private val config: RespawnConfigDef,
) : IRespawnPositionProvider {
    private val random = seededRandom(level, player)
    private var cachedStrongholds: List<BlockPos>? = null

    override fun getRespawnPosition(): BlockPos? {
        val respawnSearchCenter = getCenterPos()
        val range = config.respawnRadiusMax - config.respawnRadiusMin
        val radius = getRadius(range, config.respawnDistributionMidpoint - config.respawnRadiusMin)
        val angle = random.nextDouble(2.0 * Math.PI)
        val dx = (radius * Mth.cos(angle)).toInt()
        val dz = (radius * Mth.sin(angle)).toInt()

        return respawnSearchCenter.offset(dx, 0, dz)
    }

    private fun getCenterPos(): BlockPos {
        return when (config.respawnCenter) {
            RespawnCenter.WORLD_ORIGIN -> level.respawnData.globalPos.pos
            RespawnCenter.LAST_RESPAWN_POINT -> {
                val playerLife = player.getExpeditionLifeOrNull() ?: return level.respawnData.globalPos.pos
                playerLife.spawnPoint
            }
            RespawnCenter.STRONGHOLD -> {
                val candidates = getNearestStrongholds(player.blockPosition(), 3)
                if (candidates.isNotEmpty()) candidates[random.nextInt(candidates.size)]
                else {
                    ExpeditionaryHardcore.LOGGER.error("No strongholds found, falling back to world origin")
                    level.respawnData.globalPos.pos
                }
            }
        }
    }
    
    // get random number from 0 to max depending on probability configuration
    private fun getRadius(max: Int, gaussianMidpoint: Int): Double {
        if (max <= 0) return 0.0

        return when (config.respawnDistribution) {
            RespawnDistribution.UNIFORM ->
                random.nextDouble() * max

            // biased towards 0
            RespawnDistribution.NEARER_MORE_LIKELY ->
                random.nextDouble().let { it * it } * max

            // biased towards max
            RespawnDistribution.FURTHER_MORE_LIKELY ->
                sqrt(random.nextDouble()) * max

            // gaussian with center at configured midpoint,
            // mixed with standard random so its flatter
            RespawnDistribution.MIDPOINT_MORE_LIKELY -> {
                // tested with a python thing, doing this results in
                // essentially a flatter probabaility curve
                // 0.3 uniform, 0.7 bell
                if (random.nextDouble() < 0.3) {
                    random.nextDouble() * max
                } else {
                    val midpoint = if (gaussianMidpoint < 0) {
                        max / 2.0
                    } else {
                        gaussianMidpoint.toDouble()
                    }
                    // +/-3 std dev roughly spans the full range
                    val stdDev = max / 6.0
                    nextGaussianInRange(midpoint, stdDev, max.toDouble())
                }
            }
        }
    }

    // gaussian clamped to 0, max value, also shifted so mean is different
    private fun nextGaussianInRange(mean: Double, stdDev: Double, max: Double): Double {
        var sample: Double
        val maxAttempts = 100
        var attempts = 0

        do {
            sample = mean + nextGaussian() * stdDev
            attempts ++
        } while (sample !in 0.0..max && attempts < maxAttempts)
        sample.coerceIn(0.0, max) // in case took too many attempts; do not do immediately otherwise prob for 0 spikes up
        return sample
    }

    // Box-Muller transform https://it.wikipedia.org/wiki/Trasformazione_di_Box-Muller as kotlin.random.Random has no built-in nextGaussian
    private fun nextGaussian(): Double {
        val u1 = 1.0 - random.nextDouble()
        val u2 = random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
    }

    private fun getNearestStrongholds(origin: BlockPos, n: Int): List<BlockPos> {
        cachedStrongholds?.let { return it.take(n) }

        ExpeditionaryHardcore.LOGGER.info("Getting nearest strongholds from $origin")

        val strongholdSet = level.registryAccess()
            .lookupOrThrow(Registries.STRUCTURE_SET)
            .getOrThrow(BuiltinStructureSets.STRONGHOLDS)

        val placement = strongholdSet.value().placement() as ConcentricRingsStructurePlacement

        return (level.chunkSource.generatorState
            .getRingPositionsFor(placement)
            ?: listOf())
            .map { it.worldPosition }
            .sortedBy { it.distSqr(origin) }
            .take(n)
            .also {
                cachedStrongholds = it
                ExpeditionaryHardcore.LOGGER.info("Found ${it.size} strongholds from $origin")
            }
    }
}

/**
 * Multiplayer team behavior: scatter within a small radius around
 * the main player's current position.
 * If the main is offline, fall back to their last stored spawn point,
 * then to world origin.
 */
class TeammateRespawnProvider(
    val level: ServerLevel,
    val player: ServerPlayer,
    private val mainUuid: UUID,
) : IRespawnPositionProvider {
    private val random = seededRandom(level, player)

    override fun getRespawnPosition(): BlockPos {
        val center = getCenterPos()
        val min = MultiplayerConfig.teammateRespawnRadiusMin
        val max = MultiplayerConfig.teammateRespawnRadiusMax
        val radius = if (max <= min) max.toDouble() else min + random.nextDouble() * (max - min)
        val angle = random.nextDouble(2.0 * Math.PI)
        val dx = (radius * Mth.cos(angle)).toInt()
        val dz = (radius * Mth.sin(angle)).toInt()

        return center.offset(dx, 0, dz)
    }

    private fun getCenterPos(): BlockPos {
        level.server.playerList.getPlayer(mainUuid)?.let { return it.blockPosition() }
        ServerAllPlayersLifeData.get(level.server).playerData[mainUuid]?.lastOrNull()?.let { return it.spawnPoint }
        return level.respawnData.globalPos.pos
    }
}

/**
 * Handles overarching conditions that either can require a retry
 * or skip the underlying provider altogether
 */
class RespawnPositionConditionsProvider(
    val level: ServerLevel,
    val player: ServerPlayer,
    val baseProvider: IRespawnPositionProvider,
    val maxAttempts: Int = 20,
    private val config: RespawnConfigDef,
) : IRespawnPositionProvider {
    private val random = seededRandom(level, player)

    override fun getRespawnPosition(): BlockPos? {
        config.forcedRespawnLocations?.let { forcedRespawnLocations ->
            val index = random.nextInt(forcedRespawnLocations.size)
            ExpeditionaryHardcore.LOGGER.info("Respawn position forced override for player $player: ${forcedRespawnLocations[index]}")
            return forcedRespawnLocations[index]
        }

        var attempts = 0
        var pos: BlockPos? = null
        while (attempts < maxAttempts) {
            pos = baseProvider.getRespawnPosition() ?: return null
            if (matchesConfig(pos)) return pos
            attempts ++
        }

        return pos
    }

    private fun matchesConfig(pos: BlockPos): Boolean {
        if (config.avoidOceans && isOcean(pos)) return false

        val lastRespawn = player.getExpeditionLifeOrNull()?.spawnPoint
        val lastRespawnDistance = lastRespawn?.distSqr(pos) ?: Double.MAX_VALUE

        return lastRespawnDistance > config.minDistanceFromLastRespawn
    }

    private fun isOcean(pos: BlockPos): Boolean {
        val surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.x, pos.z)
        val surfacePos = BlockPos(pos.x, surfaceY, pos.z)
        return level.getBiome(surfacePos).`is`(BiomeTags.IS_OCEAN)
    }
}

private fun seededRandom(level: ServerLevel, player: ServerPlayer): Random {
    val worldSeed: Long = level.seed
    val uuid: UUID = player.uuid
    val playerSeed = uuid.mostSignificantBits xor uuid.leastSignificantBits
    val offset = player.getAllExpeditionLives().size * 50

    return Random((worldSeed xor playerSeed) + offset)
}