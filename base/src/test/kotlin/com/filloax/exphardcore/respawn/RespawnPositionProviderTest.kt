package com.filloax.exphardcore.respawn

import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.exphardcore.character.PlayerLifeDataTestSupport
import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.SharedConstants
import net.minecraft.core.registries.Registries
import net.minecraft.server.Bootstrap
import net.minecraft.server.level.ServerChunkCache
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets
import net.minecraft.world.level.levelgen.structure.StructureSet
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement
import net.minecraft.world.level.storage.LevelData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Same seeded random formula as real one
 */
private fun expectedRandom(worldSeed: Long, uuid: UUID, livesCount: Int): Random {
    val playerSeed = uuid.mostSignificantBits xor uuid.leastSignificantBits
    val offset = livesCount * 50
    return Random((worldSeed xor playerSeed) + offset)
}

class RespawnPositionProviderTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrapMinecraft() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    private lateinit var lifeDataSupport: PlayerLifeDataTestSupport

    private val uuid = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val worldSeed = 123456789L
    private val worldOrigin = BlockPos(0, 64, 0)

    @BeforeEach
    fun setUp() {
        lifeDataSupport = PlayerLifeDataTestSupport.install()
    }

    @AfterEach
    fun tearDown() {
        lifeDataSupport.close()
    }

    private fun mockLevel(seed: Long = worldSeed, originPos: BlockPos = worldOrigin): ServerLevel {
        val level = mock<ServerLevel>()
        whenever(level.seed).thenReturn(seed)
        val respawnData = LevelData.RespawnData.of(Level.OVERWORLD, originPos, 0f, 0f)
        whenever(level.respawnData).thenReturn(respawnData)
        return level
    }

    private fun mockPlayer(
        playerUuid: UUID = uuid,
        livesCount: Int = 0,
        currentLife: PlayerLifeData? = null,
    ): ServerPlayer {
        val player = mock<ServerPlayer>()
        whenever(player.uuid).thenReturn(playerUuid)
        lifeDataSupport.stubAllLives(player, List(livesCount) { mock<PlayerLifeData>() })
        lifeDataSupport.stubCurrentLife(player, currentLife)
        return player
    }

    private fun config(
        radiusMin: Int = 0,
        radiusMax: Int = 1000,
        minDistanceFromLastRespawn: Int = 0,
        center: RespawnCenter = RespawnCenter.WORLD_ORIGIN,
        distribution: RespawnDistribution = RespawnDistribution.UNIFORM,
        distributionMidpoint: Int = -1,
        forcedRespawnLocations: List<BlockPos>? = null,
        avoidOceans: Boolean = false,
    ): RespawnConfigDef = RespawnConfigDto(
        respawnRadiusMin = radiusMin,
        respawnRadiusMax = radiusMax,
        minDistanceFromLastRespawn = minDistanceFromLastRespawn,
        respawnCenter = center,
        respawnDistribution = distribution,
        respawnDistributionMidpoint = distributionMidpoint,
        forcedRespawnLocations = forcedRespawnLocations,
        avoidOceans = avoidOceans,
    )

    // --- RespawnPositionProvider: center selection ---

    @Test
    fun `world origin center offsets from level respawn position`() {
        val level = mockLevel()
        val player = mockPlayer()
        val provider = RespawnPositionProvider(level, player, config(center = RespawnCenter.WORLD_ORIGIN))

        val result = provider.getRespawnPosition()

        val random = expectedRandom(worldSeed, uuid, 0)
        val radius = random.nextDouble() * 1000
        val angle = random.nextDouble(2.0 * Math.PI)
        val dx = (radius * kotlin.math.cos(angle)).toInt()
        val dz = (radius * kotlin.math.sin(angle)).toInt()

        assertEquals(worldOrigin.offset(dx, 0, dz), result)
    }

    @Test
    fun `last respawn point center uses stored spawn point`() {
        val level = mockLevel()
        val storedSpawn = BlockPos(500, 70, -300)
        val life = mock<PlayerLifeData>()
        whenever(life.spawnPoint).thenReturn(storedSpawn)
        val player = mockPlayer(currentLife = life)

        val provider = RespawnPositionProvider(level, player, config(center = RespawnCenter.LAST_RESPAWN_POINT))
        val result = provider.getRespawnPosition()

        assertNotNull(result)
        // offset must be centered on storedSpawn, not world origin
        assertTrue(result!!.distSqr(storedSpawn) <= 1000.0 * 1000.0)
        assertTrue(result.distSqr(worldOrigin) > result.distSqr(storedSpawn) || storedSpawn == worldOrigin)
    }

    @Test
    fun `last respawn point center falls back to world origin with no prior life`() {
        val level = mockLevel()
        val player = mockPlayer(currentLife = null)

        val provider = RespawnPositionProvider(level, player, config(center = RespawnCenter.LAST_RESPAWN_POINT, radiusMax = 0))
        val result = provider.getRespawnPosition()

        // radiusMax 0 means radius is always 0, so result must equal the center exactly
        assertEquals(worldOrigin, result)
    }

    /** Wires up level.registryAccess()...placement() so getNearestStrongholds() can find [ringPositions]. */
    private fun stubStrongholdRings(level: ServerLevel, ringPositions: List<ChunkPos>): ConcentricRingsStructurePlacement {
        val placement = mock<ConcentricRingsStructurePlacement>()
        val structureSet = mock<StructureSet>()
        whenever(structureSet.placement()).thenReturn(placement)
        val holder = mock<Holder.Reference<StructureSet>>()
        whenever(holder.value()).thenReturn(structureSet)
        val structureSetRegistry = mock<Registry<StructureSet>>()
        whenever(structureSetRegistry.getOrThrow(BuiltinStructureSets.STRONGHOLDS)).thenReturn(holder)
        val registryAccess = mock<RegistryAccess>()
        whenever(registryAccess.lookupOrThrow(Registries.STRUCTURE_SET)).thenReturn(structureSetRegistry)
        whenever(level.registryAccess()).thenReturn(registryAccess)

        val chunkSource = mock<ServerChunkCache>()
        val generatorState = mock<ChunkGeneratorStructureState>()
        whenever(level.chunkSource).thenReturn(chunkSource)
        whenever(chunkSource.generatorState).thenReturn(generatorState)
        whenever(generatorState.getRingPositionsFor(any())).thenReturn(ringPositions)

        return placement
    }

    @Test
    fun `stronghold center picks among nearest candidates`() {
        val level = mockLevel()
        val player = mockPlayer()
        whenever(player.blockPosition()).thenReturn(BlockPos(0, 64, 0))

        val near = ChunkPos(10, 10)
        val far = ChunkPos(1000, 1000)
        stubStrongholdRings(level, listOf(far, near))

        val provider = RespawnPositionProvider(level, player, config(center = RespawnCenter.STRONGHOLD, radiusMax = 0))
        val result = provider.getRespawnPosition()

        // radiusMax 0 -> position equals the chosen stronghold candidate's world position exactly
        assertTrue(result == near.worldPosition || result == far.worldPosition)
    }

    @Test
    fun `stronghold center falls back to world origin when none found`() {
        val level = mockLevel()
        val player = mockPlayer()
        whenever(player.blockPosition()).thenReturn(BlockPos(0, 64, 0))
        stubStrongholdRings(level, listOf())

        val provider = RespawnPositionProvider(level, player, config(center = RespawnCenter.STRONGHOLD, radiusMax = 0))
        val result = provider.getRespawnPosition()

        assertEquals(worldOrigin, result)
    }

    // --- RespawnPositionProvider: radius distribution ---

    @Test
    fun `uniform distribution stays within max radius`() {
        val level = mockLevel()
        repeat(50) { i ->
            val player = mockPlayer(livesCount = i)
            val provider = RespawnPositionProvider(level, player, config(radiusMin = 0, radiusMax = 2000, distribution = RespawnDistribution.UNIFORM))
            val result = provider.getRespawnPosition()!!
            assertTrue(result.distSqr(worldOrigin) <= 2000.0 * 2000.0)
        }
    }

    @Test
    fun `zero range always returns center`() {
        val level = mockLevel()
        val player = mockPlayer()
        val provider = RespawnPositionProvider(level, player, config(radiusMin = 500, radiusMax = 500))
        val result = provider.getRespawnPosition()

        assertEquals(worldOrigin, result)
    }

    @Test
    fun `nearer more likely distribution is biased below uniform average`() {
        val level = mockLevel()
        val max = 1000
        var uniformTotal = 0.0
        var nearerTotal = 0.0
        val samples = 300

        repeat(samples) { i ->
            val uniformPlayer = mockPlayer(livesCount = i)
            val uniformProvider = RespawnPositionProvider(level, uniformPlayer, config(radiusMax = max, distribution = RespawnDistribution.UNIFORM))
            uniformTotal += worldOrigin.distSqr(uniformProvider.getRespawnPosition()!!).let { sqrt(it) }

            val nearerPlayer = mockPlayer(livesCount = i)
            val nearerProvider = RespawnPositionProvider(level, nearerPlayer, config(radiusMax = max, distribution = RespawnDistribution.NEARER_MORE_LIKELY))
            nearerTotal += worldOrigin.distSqr(nearerProvider.getRespawnPosition()!!).let { sqrt(it) }
        }

        assertTrue(nearerTotal / samples < uniformTotal / samples)
    }

    @Test
    fun `further more likely distribution is biased above uniform average`() {
        val level = mockLevel()
        val max = 1000
        var uniformTotal = 0.0
        var furtherTotal = 0.0
        val samples = 300

        repeat(samples) { i ->
            val uniformPlayer = mockPlayer(livesCount = i)
            val uniformProvider = RespawnPositionProvider(level, uniformPlayer, config(radiusMax = max, distribution = RespawnDistribution.UNIFORM))
            uniformTotal += worldOrigin.distSqr(uniformProvider.getRespawnPosition()!!).let { sqrt(it) }

            val furtherPlayer = mockPlayer(livesCount = i)
            val furtherProvider = RespawnPositionProvider(level, furtherPlayer, config(radiusMax = max, distribution = RespawnDistribution.FURTHER_MORE_LIKELY))
            furtherTotal += worldOrigin.distSqr(furtherProvider.getRespawnPosition()!!).let { sqrt(it) }
        }

        assertTrue(furtherTotal / samples > uniformTotal / samples)
    }

    @Test
    fun `midpoint distribution stays within range`() {
        val level = mockLevel()
        repeat(50) { i ->
            val player = mockPlayer(livesCount = i)
            val provider = RespawnPositionProvider(
                level, player,
                config(radiusMin = 200, radiusMax = 1200, distribution = RespawnDistribution.MIDPOINT_MORE_LIKELY, distributionMidpoint = 700)
            )
            val result = provider.getRespawnPosition()!!
            assertTrue(result.distSqr(worldOrigin) <= 1000.0 * 1000.0)
        }
    }

    // --- RespawnPositionConditionsProvider ---

    @Test
    fun `forced respawn locations short-circuit the base provider`() {
        val level = mockLevel()
        val player = mockPlayer()
        val forced = listOf(BlockPos(1, 2, 3), BlockPos(4, 5, 6))
        val baseProvider = IRespawnPositionProvider { throw AssertionError("base provider should not be called") }

        val provider = RespawnPositionConditionsProvider(
            level, player, baseProvider,
            config = config(forcedRespawnLocations = forced),
        )
        val result = provider.getRespawnPosition()

        assertTrue(result in forced)
    }

    @Test
    fun `passes through positions matching min distance from last respawn`() {
        val level = mockLevel()
        val lastSpawn = BlockPos(0, 64, 0)
        val life = mock<PlayerLifeData>()
        whenever(life.spawnPoint).thenReturn(lastSpawn)
        val player = mockPlayer(currentLife = life)

        val farPos = BlockPos(5000, 64, 0)
        val baseProvider = IRespawnPositionProvider { farPos }

        val provider = RespawnPositionConditionsProvider(
            level, player, baseProvider,
            config = config(minDistanceFromLastRespawn = 1000),
        )

        assertEquals(farPos, provider.getRespawnPosition())
    }

    @Test
    fun `retries base provider until min distance is satisfied`() {
        val level = mockLevel()
        val lastSpawn = BlockPos(0, 64, 0)
        val life = mock<PlayerLifeData>()
        whenever(life.spawnPoint).thenReturn(lastSpawn)
        val player = mockPlayer(currentLife = life)

        val tooClose = BlockPos(10, 64, 0)
        val farEnough = BlockPos(5000, 64, 0)
        var calls = 0
        val baseProvider = IRespawnPositionProvider {
            calls++
            if (calls < 3) tooClose else farEnough
        }

        val provider = RespawnPositionConditionsProvider(
            level, player, baseProvider,
            config = config(minDistanceFromLastRespawn = 1000),
        )

        assertEquals(farEnough, provider.getRespawnPosition())
        assertEquals(3, calls)
    }

    @Test
    fun `gives up and returns last attempt after max attempts exhausted`() {
        val level = mockLevel()
        val lastSpawn = BlockPos(0, 64, 0)
        val life = mock<PlayerLifeData>()
        whenever(life.spawnPoint).thenReturn(lastSpawn)
        val player = mockPlayer(currentLife = life)

        val alwaysTooClose = BlockPos(10, 64, 0)
        var calls = 0
        val baseProvider = IRespawnPositionProvider { calls++; alwaysTooClose }

        val provider = RespawnPositionConditionsProvider(
            level, player, baseProvider,
            maxAttempts = 5,
            config = config(minDistanceFromLastRespawn = 1000),
        )

        assertEquals(alwaysTooClose, provider.getRespawnPosition())
        assertEquals(5, calls)
    }

    @Test
    fun `null base provider result short-circuits without retry`() {
        val level = mockLevel()
        val player = mockPlayer()
        var calls = 0
        val baseProvider = IRespawnPositionProvider { calls++; null }

        val provider = RespawnPositionConditionsProvider(
            level, player, baseProvider,
            config = config(minDistanceFromLastRespawn = 1000),
        )

        assertNull(provider.getRespawnPosition())
        assertEquals(1, calls)
    }

    @Test
    fun `no prior life always passes the distance check`() {
        val level = mockLevel()
        val player = mockPlayer(currentLife = null)
        val pos = BlockPos(1, 2, 3)
        val baseProvider = IRespawnPositionProvider { pos }

        val provider = RespawnPositionConditionsProvider(
            level, player, baseProvider,
            config = config(minDistanceFromLastRespawn = 1_000_000),
        )

        assertEquals(pos, provider.getRespawnPosition())
    }
}
