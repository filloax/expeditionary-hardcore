package com.filloax.exphardcore.client.render

import com.filloax.exphardcore.client.model.PlayerModelOverrides
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.player.AvatarRenderer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier

/**
 * Player renderer that swaps the vanilla player model with the active
 * [PlayerModelOverrides] entry (falling back to vanilla when none returned)
 */
class CharacterAvatarRenderer(
    context: EntityRendererProvider.Context,
    slim: Boolean,
) : AvatarRenderer<AbstractClientPlayer>(context, slim) {
    private val vanillaModel: PlayerModel = model

    private fun applyOverrideModel() {
        model = PlayerModelOverrides.active?.model ?: vanillaModel
    }

    override fun submit(
        state: AvatarRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState,
    ) {
        applyOverrideModel()
        super.submit(state, poseStack, submitNodeCollector, camera)
    }

    override fun getTextureLocation(state: AvatarRenderState): Identifier =
        PlayerModelOverrides.active?.texture ?: super.getTextureLocation(state)

    // first-person pov: need to swap textures here too

    override fun renderRightHand(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        lightCoords: Int,
        skinTexture: Identifier,
        hasSleeve: Boolean,
    ) {
        applyOverrideModel()
        super.renderRightHand(
            poseStack, submitNodeCollector, lightCoords,
            PlayerModelOverrides.active?.texture ?: skinTexture, hasSleeve,
        )
    }

    override fun renderLeftHand(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        lightCoords: Int,
        skinTexture: Identifier,
        hasSleeve: Boolean,
    ) {
        applyOverrideModel()
        super.renderLeftHand(
            poseStack, submitNodeCollector, lightCoords,
            PlayerModelOverrides.active?.texture ?: skinTexture, hasSleeve,
        )
    }
}
