package com.filloax.exphardcore.respawn

import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.exphardcore.character.PlayerLifeDataTestSupport
import com.filloax.exphardcore.config.MultiplayerConfig
import net.minecraft.SharedConstants
import net.minecraft.core.BlockPos
import net.minecraft.server.Bootstrap
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.players.PlayerList
import net.minecraft.world.level.Level
import net.minecraft.world.level.storage.LevelData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID
import kotlin.random.Random

/**
 * Same seeded random formula as the production one (see seededRandom in RespawnPositionOverride.kt).
 */
private fun expectedRandom(worldSeed: Long, uuid: UUID, livesCount: Int): Random {
    val playerSeed = uuid.mostSignificantBits xor uuid.leastSignificantBits
    val offset = livesCount * 50
    return Random((worldSeed xor playerSeed) + offset)
}

/**
 * Covers the online-main path: a secondary member scatters within the configured
 * teammate radius around the main player's current position.
 *
 * The offline-main fallback (reads ServerAllPlayersLifeData by uuid via the
 * FxSavedData `loadData` extension) isn't unit-tested here: that extension is a
 * static function that can't be stubbed with Mockito, so it needs a running
 * server rather than mocks.
 */
class TeammateRespawnProviderTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun bootstrapMinecraft() {
            SharedConstants.tryDetectVersion()
            Bootstrap.bootStrap()
        }
    }

    private lateinit var lifeDataSupport: PlayerLifeDataTestSupport

    private val playerUuid = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val mainUuid = UUID.fromString("99999999-8888-7777-6666-555555555555")
    private val worldSeed = 123456789L
    private val worldOrigin = BlockPos(0, 64, 0)
    private val mainPos = BlockPos(2000, 70, -1500)

    private var savedMin = 0
    private var savedMax = 0

    @BeforeEach
    fun setUp() {
        lifeDataSupport = PlayerLifeDataTestSupport.install()
        savedMin = MultiplayerConfig.teammateRespawnRadiusMin
        savedMax = MultiplayerConfig.teammateRespawnRadiusMax
    }

    @AfterEach
    fun tearDown() {
        MultiplayerConfig.teammateRespawnRadiusMin = savedMin
        MultiplayerConfig.teammateRespawnRadiusMax = savedMax
        lifeDataSupport.close()
    }

    private fun mockLevel(onlineMain: ServerPlayer?): ServerLevel {
        val level = mock<ServerLevel>()
        whenever(level.seed).thenReturn(worldSeed)
        val respawnData = LevelData.RespawnData.of(Level.OVERWORLD, worldOrigin, 0f, 0f)
        whenever(level.respawnData).thenReturn(respawnData)

        val server = mock<MinecraftServer>()
        val playerList = mock<PlayerList>()
        whenever(playerList.getPlayer(mainUuid)).thenReturn(onlineMain)
        whenever(server.playerList).thenReturn(playerList)
        whenever(level.server).thenReturn(server)
        return level
    }

    private fun mockSecondary(livesCount: Int = 0): ServerPlayer {
        val player = mock<ServerPlayer>()
        whenever(player.uuid).thenReturn(playerUuid)
        lifeDataSupport.stubAllLives(player, List(livesCount) { mock<PlayerLifeData>() })
        return player
    }

    private fun mockMain(pos: BlockPos = mainPos): ServerPlayer {
        val main = mock<ServerPlayer>()
        whenever(main.blockPosition()).thenReturn(pos)
        return main
    }

    private fun expectedPos(center: BlockPos, min: Int, max: Int, livesCount: Int): BlockPos {
        val random = expectedRandom(worldSeed, playerUuid, livesCount)
        val radius = if (max <= min) max.toDouble() else min + random.nextDouble() * (max - min)
        val angle = random.nextDouble(2.0 * Math.PI)
        val dx = (radius * kotlin.math.cos(angle)).toInt()
        val dz = (radius * kotlin.math.sin(angle)).toInt()
        return center.offset(dx, 0, dz)
    }

    @Test
    fun `scatters around online main within configured radius`() {
        MultiplayerConfig.teammateRespawnRadiusMin = 0
        MultiplayerConfig.teammateRespawnRadiusMax = 64

        val level = mockLevel(onlineMain = mockMain())
        repeat(50) { i ->
            val player = mockSecondary(livesCount = i)
            val result = TeammateRespawnProvider(level, player, mainUuid).getRespawnPosition()
            assertTrue(result.distSqr(mainPos) <= 64.0 * 64.0, "result $result outside radius of $mainPos")
        }
    }

    @Test
    fun `respects minimum radius`() {
        MultiplayerConfig.teammateRespawnRadiusMin = 40
        MultiplayerConfig.teammateRespawnRadiusMax = 64

        val level = mockLevel(onlineMain = mockMain())
        repeat(50) { i ->
            val player = mockSecondary(livesCount = i)
            val result = TeammateRespawnProvider(level, player, mainUuid).getRespawnPosition()
            val dist = kotlin.math.sqrt(result.distSqr(mainPos))
            // horizontal-only offset truncated to int, so allow a couple blocks of slack below min
            assertTrue(dist >= 40.0 - 2.0, "result $result nearer than min radius (dist=$dist)")
            assertTrue(dist <= 64.0, "result $result beyond max radius (dist=$dist)")
        }
    }

    @Test
    fun `exact position matches seeded formula`() {
        MultiplayerConfig.teammateRespawnRadiusMin = 0
        MultiplayerConfig.teammateRespawnRadiusMax = 64

        val level = mockLevel(onlineMain = mockMain())
        val player = mockSecondary(livesCount = 3)
        val result = TeammateRespawnProvider(level, player, mainUuid).getRespawnPosition()

        assertEquals(expectedPos(mainPos, 0, 64, 3), result)
    }

    @Test
    fun `fixed radius when min equals max`() {
        MultiplayerConfig.teammateRespawnRadiusMin = 50
        MultiplayerConfig.teammateRespawnRadiusMax = 50

        val level = mockLevel(onlineMain = mockMain())
        val player = mockSecondary()
        val result = TeammateRespawnProvider(level, player, mainUuid).getRespawnPosition()

        assertEquals(expectedPos(mainPos, 50, 50, 0), result)
    }

    @Test
    fun `same inputs produce the same position`() {
        MultiplayerConfig.teammateRespawnRadiusMin = 0
        MultiplayerConfig.teammateRespawnRadiusMax = 64

        val level = mockLevel(onlineMain = mockMain())
        val a = TeammateRespawnProvider(level, mockSecondary(livesCount = 2), mainUuid).getRespawnPosition()
        val b = TeammateRespawnProvider(level, mockSecondary(livesCount = 2), mainUuid).getRespawnPosition()

        assertEquals(a, b)
    }
}
