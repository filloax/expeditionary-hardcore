package com.filloax.exphardcore.client.model

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.ExpeditionaryHardcore.Companion.MOD_ID
import com.filloax.exphardcore.client.clientPlayerLifeData
import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig
import com.filloax.exphardcore.utils.id
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.model.player.PlayerModel
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import kotlin.random.Random

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

    private val DEFAULT_MODELS = listOf(id("cydonia"), id("evil_cydonia"))

    private val models = mutableMapOf<Identifier, PlayerModelOverride>()

    class PlayerModelOverride(
        val id: Identifier,
        val model: PlayerModel,
        val texture: Identifier,
    )

    @JvmStatic
    val active: PlayerModelOverride?
        get() = if (ExpeditionaryHardcoreConfig.replacePlayerModel) getLifeModel()?.let { models[it] } else null

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

    private fun getLifeModel(): Identifier? {
        val currentLifeData = clientPlayerLifeData ?: return null
        val random = Random(currentLifeData.id.leastSignificantBits)

        // TODO: better choice, embed it in life, server-side, etc.
        return DEFAULT_MODELS.random(random)
    }
}
