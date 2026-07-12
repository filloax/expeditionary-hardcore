package com.filloax.exphardcore.client.model

import com.filloax.exphardcore.ExpeditionaryHardcore
import net.minecraft.client.model.geom.PartPose
import net.minecraft.client.model.geom.builders.CubeDeformation
import net.minecraft.client.model.geom.builders.CubeListBuilder
import net.minecraft.client.model.geom.builders.LayerDefinition
import net.minecraft.client.model.geom.builders.MeshDefinition
import net.minecraft.client.model.geom.builders.PartDefinition

/**
 * Converts a [BbModel] into a vanilla [LayerDefinition] shaped like the player model,
 * reusing vanilla animations.
 *
 * Requires groups named close to their standard player parts (ignoring case + some characters).
 * - Extra parts are parented depending on Blockbench outliner.
 * - Missing parts are created empty
 */
object BbModelConverter {
    // file may be more comment-heavy as rendering stuff makes my brain hurt
    // and it will be erased come tomorrow morning otherwise

    // vanilla root part name -> default pivot in Blockbench coordinates
    private val STANDARD_PARTS = mapOf(
        "head" to floatArrayOf(0f, 24f, 0f),
        "body" to floatArrayOf(0f, 24f, 0f),
        "right_arm" to floatArrayOf(5f, 22f, 0f),
        "left_arm" to floatArrayOf(-5f, 22f, 0f),
        "right_leg" to floatArrayOf(1.9f, 12f, 0f),
        "left_leg" to floatArrayOf(-1.9f, 12f, 0f),
    )

    // overlay part name -> standard parent it must be a child of
    private val OVERLAY_PARTS = mapOf(
        "hat" to "head",
        "jacket" to "body",
        "right_sleeve" to "right_arm",
        "left_sleeve" to "left_arm",
        "right_pants" to "right_leg",
        "left_pants" to "left_leg",
    )

    // blockbench default names
    private val NAME_ALIASES = mapOf(
        "hatlayer" to "hat", "headlayer" to "hat",
        "bodylayer" to "jacket",
        "rightarmlayer" to "right_sleeve", "leftarmlayer" to "left_sleeve",
        "rightleglayer" to "right_pants", "leftleglayer" to "left_pants",
    )

    // origin point of overall model
    private val VIRTUAL_ROOT_PIVOT = floatArrayOf(0f, 24f, 0f)

    fun convert(model: BbModel): LayerDefinition {
        val mesh = MeshDefinition()
        val builder = Builder(mesh)
        model.roots.forEach { builder.walk(it, builder.rootCtx) }
        builder.fillMissingStandardParts()
        return LayerDefinition.create(mesh, model.textureWidth, model.textureHeight)
    }

    private class BoneCtx(val part: PartDefinition, val pivot: FloatArray)

    private class Builder(mesh: MeshDefinition) {
        val rootCtx = BoneCtx(mesh.root, VIRTUAL_ROOT_PIVOT)
        val standardParts = mutableMapOf<String, BoneCtx>()
        val overlayParts = mutableMapOf<String, BoneCtx>()
        private val usedNames = mutableMapOf<PartDefinition, MutableSet<String>>()

        fun walk(node: BbNode, ctx: BoneCtx) {
            when (node) {
                is BbNode.Cube -> addCube(node.cube, ctx)
                is BbNode.Group -> walkGroup(node, ctx)
            }
        }

        private fun walkGroup(group: BbNode.Group, ctx: BoneCtx) {
            val mapped = mappedName(group.name)
            val groupCtx = when {
                mapped in STANDARD_PARTS -> standardPart(mapped!!, group.origin, group.rotation)
                mapped in OVERLAY_PARTS -> overlayPart(mapped!!, group.origin, group.rotation)
                // container groups (like a root group wrapping the whole model) are
                // skipped so the standard parts inside them stay direct root children
                containsStandardPart(group) -> {
                    if (group.rotation.any { it != 0f })
                        ExpeditionaryHardcore.LOGGER.warn(
                            "bbmodel group '{}' contains player parts but has a rotation, ignoring it", group.name
                        )
                    ctx
                }
                else -> childPart(ctx, group.name, group.origin, group.rotation)
            }
            group.children.forEach { walk(it, groupCtx) }
        }

        //
        private fun addCube(cube: BbCube, ctx: BoneCtx) {
            val overlay = mappedName(cube.name)?.takeIf { it in OVERLAY_PARTS }
            val target = when {
                // overlay cubes (like a "Hat Layer" element inside the Head group) go in
                // their own part so vanilla can toggle them from skin customization settings
                overlay != null -> overlayPart(overlay, ctx.pivot, FloatArray(3))
                // vanilla cubes can't rotate, wrap in a part carrying the rotation
                cube.hasRotation() -> childPart(ctx, cube.name + "_r", cube.origin, cube.rotation)
                else -> ctx
            }
            target.part.addOrReplaceChild(
                uniqueName(target.part, sanitizeName(cube.name)),
                CubeListBuilder.create()
                    .texOffs(cube.uvOffset.first, cube.uvOffset.second)
                    .mirror(cube.mirror)
                    // similar to pose, flips coords due to MC coords being different than Blockbench
                    // (flip x and y)
                    .addBox(
                        target.pivot[0] - cube.to[0],
                        target.pivot[1] - cube.to[1],
                        cube.from[2] - target.pivot[2],
                        cube.to[0] - cube.from[0],
                        cube.to[1] - cube.from[1],
                        cube.to[2] - cube.from[2],
                        CubeDeformation(cube.inflate),
                    ),
                PartPose.ZERO,
            )
        }

        private fun standardPart(name: String, pivot: FloatArray, rotation: FloatArray) =
            standardParts.getOrPut(name) {
                BoneCtx(rootCtx.part.addOrReplaceChild(name, CubeListBuilder.create(), pose(rootCtx.pivot, pivot, rotation)), pivot)
            }

        private fun overlayPart(name: String, pivot: FloatArray, rotation: FloatArray): BoneCtx =
            overlayParts.getOrPut(name) {
                val parent = standardParts[OVERLAY_PARTS.getValue(name)]
                    ?: standardPart(OVERLAY_PARTS.getValue(name), STANDARD_PARTS.getValue(OVERLAY_PARTS.getValue(name)), FloatArray(3))
                BoneCtx(parent.part.addOrReplaceChild(name, CubeListBuilder.create(), pose(parent.pivot, pivot, rotation)), pivot)
            }

        private fun childPart(parent: BoneCtx, name: String, pivot: FloatArray, rotation: FloatArray) = BoneCtx(
            parent.part.addOrReplaceChild(
                uniqueName(parent.part, sanitizeName(name)),
                CubeListBuilder.create(),
                pose(parent.pivot, pivot, rotation),
            ),
            pivot,
        )

        fun fillMissingStandardParts() {
            STANDARD_PARTS.forEach { (name, pivot) ->
                if (name !in standardParts) standardPart(name, pivot, FloatArray(3))
            }
            OVERLAY_PARTS.keys.forEach { name ->
                if (name !in overlayParts)
                    overlayPart(name, standardParts.getValue(OVERLAY_PARTS.getValue(name)).pivot, FloatArray(3))
            }
        }

        private fun uniqueName(parent: PartDefinition, name: String): String {
            val used = usedNames.getOrPut(parent) { mutableSetOf() }
            var result = name
            var i = 2
            while (!used.add(result)) result = "${name}_${i++}"
            return result
        }
    }

    // Convert from blockbench coordinates to vanilla coordinates
    private fun pose(parentPivot: FloatArray, pivot: FloatArray, rotation: FloatArray): PartPose {
        val x = -(pivot[0] - parentPivot[0])
        val y = -(pivot[1] - parentPivot[1])
        val z = pivot[2] - parentPivot[2]
        return if (rotation.all { it == 0f }) {
            if (x == 0f && y == 0f && z == 0f) PartPose.ZERO else PartPose.offset(x, y, z)
        } else PartPose.offsetAndRotation(
            x, y, z,
            -Math.toRadians(rotation[0].toDouble()).toFloat(),
            -Math.toRadians(rotation[1].toDouble()).toFloat(),
            Math.toRadians(rotation[2].toDouble()).toFloat(),
        )
    }

    private fun containsStandardPart(group: BbNode.Group): Boolean = group.children.any {
        it is BbNode.Group && (mappedName(it.name) in STANDARD_PARTS || containsStandardPart(it))
    }

    /**
     * Normalize name, first match in aliases, then _PARTS maps keys
     */
    private fun mappedName(name: String): String? {
        val normalized = name.lowercase().replace(" ", "").replace("_", "")
        NAME_ALIASES[normalized]?.let { return it }
        return (STANDARD_PARTS.keys + OVERLAY_PARTS.keys).firstOrNull { it.replace("_", "") == normalized }
    }

    private fun sanitizeName(name: String) = name.lowercase().replace(Regex("[^a-z0-9]+"), "_")
}
