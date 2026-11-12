package com.filloax.exphardcore.client.model

import net.minecraft.client.model.geom.ModelPart
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.util.RandomSource
import net.minecraft.util.Util

/**
 * [PlayerModel] built from a bbmodel, used to replace
 * player model + work with overlays like skeleton arrows
 */
class BbPlayerModel(root: ModelPart) : PlayerModel(root, false) {
    // vanilla picks a random cube from the result (StuckInBodyLayer), which crashes on empty parts
    private val nonEmptyBodyParts = listOf(head, body, leftArm, rightArm, leftLeg, rightLeg).filterNot { it.isEmpty }

    init {
        require(nonEmptyBodyParts.isNotEmpty()) { "Player model has no cubes directly in any standard body part" }
    }

    override fun getRandomBodyPart(random: RandomSource): ModelPart = Util.getRandom(nonEmptyBodyParts, random)
}
