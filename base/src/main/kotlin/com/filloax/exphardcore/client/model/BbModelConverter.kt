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
 * Requires groups/cubes named close to their standard player parts (ignoring case + some characters).
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
        val builder = Builder()
        model.roots.forEach { builder.walk(it, builder.root) }
        // PlayerModel looks all of these up for things like arrow overlays
        STANDARD_PARTS.keys.forEach { builder.standardPart(it) }
        OVERLAY_PARTS.keys.forEach { builder.overlayPart(it) }

        val mesh = MeshDefinition()
        builder.root.children.values.forEach { it.build(mesh.root) }
        return LayerDefinition.create(mesh, model.textureWidth, model.textureHeight)
    }

    // Use to gradually build a part, as vanilla PartDefinition takes cubes at creation
    private class Bone(val name: String, val pivot: FloatArray, val pose: PartPose) {
        val cubes: CubeListBuilder = CubeListBuilder.create()
        val children = linkedMapOf<String, Bone>()

        // standard and overlay parts
        fun fixedChild(name: String, pivot: FloatArray, rotation: FloatArray = FloatArray(3)) =
            // misses pivot/rotation of duplicate group names, edge case anyways
            children.getOrPut(name) { Bone(name, pivot, pose(this.pivot, pivot, rotation)) }

        // non-standard parts
        fun extraChild(name: String, pivot: FloatArray, rotation: FloatArray): Bone {
            val base = sanitizeName(name)
            // ensure not replacing extra part with same name
            var unique = base
            var i = 2
            while (unique in children || unique in STANDARD_PARTS || unique in OVERLAY_PARTS) unique = "${base}_${i++}"
            return Bone(unique, pivot, pose(this.pivot, pivot, rotation)).also { children[unique] = it }
        }

        fun build(parent: PartDefinition) {
            val part = parent.addOrReplaceChild(name, cubes, pose)
            children.values.forEach { it.build(part) }
        }
    }

    private class Builder {
        val root = Bone("root", VIRTUAL_ROOT_PIVOT, PartPose.ZERO)

        fun walk(node: BbNode, parent: Bone) {
            when (node) {
                is BbNode.Cube -> addCube(node.cube, parent)
                is BbNode.Group -> walkGroup(node, parent)
            }
        }

        private fun walkGroup(group: BbNode.Group, parent: Bone) {
            val mapped = mappedName(group.name)
            val bone = when {
                mapped in STANDARD_PARTS -> standardPart(mapped!!, group.origin, group.rotation)
                mapped in OVERLAY_PARTS -> overlayPart(mapped!!, group.origin, group.rotation)
                // container groups (like a root group wrapping the whole model) are
                // skipped so the standard parts inside them stay direct root children
                containsStandardPart(group) -> {
                    if (group.rotation.any { it != 0f })
                        ExpeditionaryHardcore.LOGGER.warn(
                            "bbmodel group '{}' contains player parts but has a rotation, ignoring it", group.name
                        )
                    parent
                }
                else -> parent.extraChild(group.name, group.origin, group.rotation)
            }
            group.children.forEach { walk(it, bone) }
        }

        private fun addCube(cube: BbCube, parent: Bone) {
            val mapped = mappedName(cube.name)
            // in mc models a cube belongs to a part
            val owner = when (mapped) {
                // overlay cubes (like a "Hat Layer" element inside the Head group) go in
                // their own part so vanilla can toggle them from skin customization settings
                in OVERLAY_PARTS -> overlayPart(mapped!!).also {
                    val standard = standardPart(OVERLAY_PARTS.getValue(mapped))
                    if (parent !== root && parent !== standard && parent !== it)
                        ExpeditionaryHardcore.LOGGER.warn(
                            "bbmodel cube '{}' moved to {}, won't follow its group's animation", cube.name, mapped
                        )
                }
                // models without groups, cubes named like the part
                in STANDARD_PARTS if parent === root -> standardPart(mapped!!)
                else -> parent
            }
            val target = when {
                // vanilla cubes can't rotate, wrap in a part carrying the rotation
                cube.hasRotation() -> owner.extraChild(cube.name + "_r", cube.origin, cube.rotation)
                // owner root (default) -> is extra prt
                owner === root -> owner.extraChild(cube.name, owner.pivot, FloatArray(3))
                else -> owner
            }
            target.cubes
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
                )
        }

        fun standardPart(
            name: String,
            pivot: FloatArray = STANDARD_PARTS.getValue(name),
            rotation: FloatArray = FloatArray(3),
        ) = root.fixedChild(name, pivot, rotation)

        fun overlayPart(name: String, pivot: FloatArray? = null, rotation: FloatArray = FloatArray(3)): Bone {
            // small edge case: if overlay loaded before standard, in case there was custom pivot/rotation it gets lost
            // as this sets it to default
            val standard = standardPart(OVERLAY_PARTS.getValue(name))
            return standard.fixedChild(name, pivot ?: standard.pivot, rotation)
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
