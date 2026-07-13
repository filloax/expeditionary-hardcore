package com.filloax.exphardcore.client.model

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.ExpeditionaryHardcore.Companion.MOD_ID
import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig
import com.filloax.exphardcore.network.DATA_PLAYER_MODEL
import com.filloax.fxlib.api.networking.getTrackedData
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.world.entity.Entity
import kotlin.jvm.optionals.getOrNull

/**
 * Client-side registry of player model replacements loaded from bbmodel files
 * (see [BbModelConverter]), keyed by id.
 * Uses models from resources/exphard_playermodels, and Apibalego (WIP)
 */
object PlayerModelOverrides {
    const val MODELS_PATH = "exphard_playermodels"

    val reloadListener = ResourceManagerReloadListener { resourceManager: ResourceManager ->
        models.clear()
        resourceManager.listResources(MODELS_PATH) { it.path.endsWith(".bbmodel") }
            .forEach { (fileId, resource) ->
                val id = fileId.withPath { path ->
                    path.removePrefix("$MODELS_PATH/").removeSuffix(".bbmodel")
                }
                resource.openAsReader().use { reader ->
                    loadModel(id, reader.readText())
                }
            }
    }

    private val models = mutableMapOf<Identifier, PlayerModelOverride>()

    class PlayerModelOverride(
        val id: Identifier,
        val model: PlayerModel,
        val texture: Identifier,
    )

    fun forPlayer(entity: Entity): PlayerModelOverride? {
        if (!ExpeditionaryHardcoreConfig.replacePlayerModel) return null
        val resourceId = entity.getTrackedData(DATA_PLAYER_MODEL)?.getOrNull() ?: return null
        return models[resourceId]
    }

    fun loadModel(id: Identifier, text: String) {
        try {
            val bbModel = BbModel.parse(text)
            val root = BbModelConverter.convert(bbModel).bakeRoot()
            val texture = registerTexture(id, bbModel)
                ?: run {
                    ExpeditionaryHardcore.LOGGER.warn("Player model {} has no embedded texture, skipping", id)
                    return
                }
            models[id] = PlayerModelOverride(id, PlayerModel(root, false), texture)
            ExpeditionaryHardcore.LOGGER.info("Loaded player model {}", id)
        } catch (e: Exception) {
            ExpeditionaryHardcore.LOGGER.error("Failed to load player model {}", id, e)
        }
    }

    private fun registerTexture(id: Identifier, bbModel: BbModel): Identifier? {
        val png = bbModel.texturePng ?: return null
        val texture = DynamicTexture({ "$MOD_ID player model $id" }, NativeImage.read(png))
        val textureId = id.withPath { "dynamic/$MODELS_PATH/$it" }
        Minecraft.getInstance().textureManager.register(textureId, texture)
        return textureId
    }
}
