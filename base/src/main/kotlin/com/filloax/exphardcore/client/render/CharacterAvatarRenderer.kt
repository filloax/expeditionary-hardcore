package com.filloax.exphardcore.client.render

import com.filloax.exphardcore.client.model.PlayerModelOverrides
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.player.AbstractClientPlayer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.player.AvatarRenderer
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.state.level.CameraRenderState
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.Entity

/**
 * Player renderer that swaps the vanilla player model with the active [PlayerModelOverrides] entry
 * for whichever player is actually being rendered (falling back to vanilla when none is set) - so
 * different players can show different models to bystanders, not just the local client's own model.
 */
class CharacterAvatarRenderer(
    context: EntityRendererProvider.Context,
    slim: Boolean,
) : AvatarRenderer<AbstractClientPlayer>(context, slim) {
    private val vanillaModel: PlayerModel = model

    private fun overrideFor(entity: Entity?): PlayerModelOverrides.PlayerModelOverride? =
        entity?.let { PlayerModelOverrides.forPlayer(it) }

    // AvatarRenderState.id is the rendered entity's network id, set by vanilla in extractRenderState
    private fun overrideFor(state: AvatarRenderState): PlayerModelOverrides.PlayerModelOverride? =
        overrideFor(Minecraft.getInstance().level?.getEntity(state.id))

    private fun applyOverrideModel(override: PlayerModelOverrides.PlayerModelOverride?) {
        model = override?.model ?: vanillaModel
    }

    override fun submit(
        state: AvatarRenderState,
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        camera: CameraRenderState,
    ) {
        applyOverrideModel(overrideFor(state))
        super.submit(state, poseStack, submitNodeCollector, camera)
    }

    override fun getTextureLocation(state: AvatarRenderState): Identifier =
        overrideFor(state)?.texture ?: super.getTextureLocation(state)

    // first-person pov: always the local player, need to swap textures here too

    override fun renderRightHand(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        lightCoords: Int,
        skinTexture: Identifier,
        hasSleeve: Boolean,
    ) {
        val override = overrideFor(Minecraft.getInstance().player)
        applyOverrideModel(override)
        super.renderRightHand(
            poseStack, submitNodeCollector, lightCoords,
            override?.texture ?: skinTexture, hasSleeve,
        )
    }

    override fun renderLeftHand(
        poseStack: PoseStack,
        submitNodeCollector: SubmitNodeCollector,
        lightCoords: Int,
        skinTexture: Identifier,
        hasSleeve: Boolean,
    ) {
        val override = overrideFor(Minecraft.getInstance().player)
        applyOverrideModel(override)
        super.renderLeftHand(
            poseStack, submitNodeCollector, lightCoords,
            override?.texture ?: skinTexture, hasSleeve,
        )
    }
}
