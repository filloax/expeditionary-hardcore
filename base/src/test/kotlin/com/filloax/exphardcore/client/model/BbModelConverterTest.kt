package com.filloax.exphardcore.client.model

import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.util.RandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.math.PI

class BbModelConverterTest {
    companion object {
        private lateinit var model: BbModel
        private lateinit var root: ModelPart

        @JvmStatic
        @BeforeAll
        fun load() {
            val text = BbModelConverterTest::class.java.getResourceAsStream("/cydonia.bbmodel")!!
                .reader().use { it.readText() }
            model = BbModel.parse(text)
            root = BbModelConverter.convert(model).bakeRoot()
        }
    }

    // parts are wrapped one level per source cube since vanilla PartDefinitions
    // can't receive cubes after creation
    private fun singleCube(part: ModelPart): ModelPart.Cube = part.getRandomCube(RandomSource.create(0))

    @Test
    fun `parses model metadata`() {
        assertEquals("cydonia", model.name)
        assertEquals(64, model.textureWidth)
        assertEquals(64, model.textureHeight)
        assertNotNull(model.texturePng)
        // PNG magic
        assertEquals(0x89.toByte(), model.texturePng!![0])
        assertEquals('P'.code.toByte(), model.texturePng!![1])
    }

    @Test
    fun `creates all parts required by PlayerModel`() {
        // the constructor looks all of them up and throws if any is missing
        PlayerModel(root, false)
    }

    @Test
    fun `standard parts have vanilla pivots`() {
        val head = root.getChild("head")
        assertEquals(0f, head.x)
        assertEquals(0f, head.y)
        assertEquals(0f, head.z)

        val leftLeg = root.getChild("left_leg")
        assertEquals(1.9f, leftLeg.x)
        assertEquals(12f, leftLeg.y)
        assertEquals(0f, leftLeg.z)

        val rightArm = root.getChild("right_arm")
        assertEquals(-5f, rightArm.x)
        assertEquals(2f, rightArm.y)
    }

    @Test
    fun `head cube matches vanilla geometry`() {
        val cube = singleCube(root.getChild("head").getChild("head"))
        assertEquals(-4f, cube.minX)
        assertEquals(-8f, cube.minY)
        assertEquals(-4f, cube.minZ)
        assertEquals(4f, cube.maxX)
        assertEquals(0f, cube.maxY)
        assertEquals(4f, cube.maxZ)
    }

    @Test
    fun `overlay cubes go in vanilla layer parts`() {
        // Cube min/max don't include the CubeDeformation (it only grows the polygons)
        val hat = singleCube(root.getChild("head").getChild("hat").getChild("hat_layer"))
        assertEquals(-4f, hat.minX)
        assertEquals(-8f, hat.minY)
        assertEquals(4f, hat.maxX)

        val leftPants = root.getChild("left_leg").getChild("left_pants")
        assertEquals(-2f, singleCube(leftPants.getChild("left_leg_layer")).minX)
        assertTrue(root.getChild("body").hasChild("jacket"))
        assertTrue(root.getChild("left_arm").hasChild("left_sleeve"))
        assertTrue(root.getChild("right_arm").hasChild("right_sleeve"))
        assertTrue(root.getChild("right_leg").hasChild("right_pants"))
    }

    @Test
    fun `extra groups become child parts inheriting animation`() {
        val bun = root.getChild("head").getChild("bun")
        // bbmodel rotation [-17.5, 0, 0] -> negated x, radians
        assertEquals(17.5f * PI.toFloat() / 180f, bun.xRot, 1e-5f)
        assertEquals(-(28.89587f - 24f), bun.y, 1e-5f)
        assertEquals(3.00926f, bun.z, 1e-5f)
        assertTrue(bun.getAllParts().size > 1)
    }

    @Test
    fun `rotated cubes get wrapped in synthetic parts`() {
        val head = root.getChild("head")
        // the two side cubes with [0, -25/25, 0] rotation
        val wrapper = head.getChild("cube_r")
        assertEquals(25f * PI.toFloat() / 180f, wrapper.yRot, 1e-5f)
        assertTrue(head.hasChild("cube_r_2"))
    }
}
