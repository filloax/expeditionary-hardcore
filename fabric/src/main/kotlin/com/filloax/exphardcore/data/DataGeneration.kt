package com.filloax.exphardcore.data

import com.filloax.exphardcore.ExpeditionaryHardcoreTags
import com.filloax.exphardcore.character.PlayerModelDefinition
import com.filloax.exphardcore.character.PlayerModelDefinitions
import com.filloax.exphardcore.character.quirk.*
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.json.saveStable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.minecraft.client.data.models.model.*
import net.minecraft.client.renderer.item.ClientItem
import net.minecraft.core.registries.Registries
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagFile
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Item
import java.util.concurrent.CompletableFuture

class ExpeditionaryHardcoreDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        val pack = fabricDataGenerator.createPack()

        pack.addProvider(::ItemModelProvider)
        pack.addProvider(::PlayerModelDefinitionProvider)
        pack.addProvider(::LootTableTagProvider)
        pack.addProvider(::LifeQuirkDefinitionProvider)
    }
}


class PlayerModelDefinitionProvider(private val output: FabricPackOutput) : DataProvider {
    private val pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, PlayerModelDefinitions.DIRECTORY)

    override fun run(cache: CachedOutput): CompletableFuture<*> {
        return CompletableFuture.allOf(
            *listOf(
                "cydonia",
                "evil_cydonia",
            )
            .map(::createBuiltinModel)
            .map { (modelId, modelDefinition) ->
                saveStable(cache, PlayerModelDefinition.serializer(), modelDefinition, pathProvider.json(modelId))
            }.toTypedArray()
        )
    }

    private fun createBuiltinModel(name: String) = id(name).let { modelId ->
        Pair(
            modelId,
            PlayerModelDefinition(
                model=modelId,
                builtin=true
            )
        )
    }

    override fun getName() = "Expeditionary Hardcore Player Model Definitions"
}


class LifeQuirkDefinitionProvider(output: FabricPackOutput) : DataProvider {
    private val pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, LifeQuirkDefinitions.DIRECTORY)

    override fun run(cache: CachedOutput): CompletableFuture<*> {
        val json = Json
        val quirks = mapOf(
            id("slow") to LifeQuirkDefinition(
                icon = id("slow"),
                name = "effect.exphardcore.quirk_slow",
                description = "effect.exphardcore.quirk_slow.desc",
                attributeModifiers = listOf(
                    QuirkAttributeModifier(
                        Identifier.withDefaultNamespace("movement_speed"),
                        QuirkModifierOperation.ADD_MULTIPLIED_TOTAL,
                        -0.10,
                    ),
                ),
                builtin = true,
            ),
            id("frail") to LifeQuirkDefinition(
                icon = id("frail"),
                name = "effect.exphardcore.quirk_frail",
                description = "effect.exphardcore.quirk_frail.desc",
                attributeModifiers = listOf(
                    QuirkAttributeModifier(
                        Identifier.withDefaultNamespace("max_health"),
                        QuirkModifierOperation.ADD_MULTIPLIED_TOTAL,
                        -0.10,
                    ),
                ),
                builtin = true,
            ),
            id("heavy_boned") to LifeQuirkDefinition(
                icon = id("heavy_boned"),
                name = "effect.exphardcore.quirk_heavy_boned",
                description = "effect.exphardcore.quirk_heavy_boned.desc",
                behavior = QuirkBehaviorRef(
                    id("fall_impact"),
                    Json.encodeToJsonElement(
                        FallImpactBehavior.Params(
                            damageMultiplier = 1.25f,
                            effectDurationTicks = 60,
                            effectAmplifier = 0,
                            effects = listOf(MobEffects.SLOWNESS, MobEffects.BLINDNESS)
                                .map { it.unwrapKey().orElseThrow().identifier() }
                        )
                    ).jsonObject,
                ),
                builtin = true,
            ),
        )

        return CompletableFuture.allOf(
            *quirks.map { (quirkId, definition) ->
                saveStable(cache, LifeQuirkDefinition.serializer(), definition, pathProvider.json(quirkId))
            }.toTypedArray()
        )
    }

    override fun getName() = "Expeditionary Hardcore Life Quirk Definitions"
}


class ItemModelProvider(output: FabricPackOutput) : DataProvider {
    private val modelPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models")
    private val itemPathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items")

    override fun run(cache: CachedOutput): CompletableFuture<*> {
        val models = mutableMapOf<Identifier, ModelInstance>()
        val items = mutableMapOf<Item, ClientItem>()

        ExpeditionaryHardcoreItems.all.values.forEach { item ->
            val modelId = item.makeFlatModel { id, model -> models[id] = model }
            items[item] = ClientItem(ItemModelUtils.plainModel(modelId), ClientItem.Properties.DEFAULT)
        }

        return CompletableFuture.allOf(
            DataProvider.saveAll(cache, { model: ModelInstance -> model.get() }, modelPathProvider::json, models),
            DataProvider.saveAll(cache, ClientItem.CODEC, { item: Item -> itemPathProvider.json(item.builtInRegistryHolder().key().identifier()) }, items),
        )
    }

    private fun Item.makeFlatModel(output: (Identifier, ModelInstance) -> Unit) = ModelTemplates.FLAT_ITEM.create(
        ModelLocationUtils.getModelLocation(this),
        TextureMapping.layer0(this),
        output,
    )

    override fun getName() = "Expeditionary Hardcore Item Models"
}

// Manually build tag as loot tables are not present during datagen
class LootTableTagProvider(output: FabricPackOutput) : DataProvider {
    private val pathProvider = output.createPathProvider(PackOutput.Target.DATA_PACK, Registries.tagsDirPath(Registries.LOOT_TABLE))

    override fun run(cache: CachedOutput): CompletableFuture<*> {
        val tagFile = TagFile(
            listOf(
                TagEntry.element(id("loadout/sample_warrior")),
                TagEntry.element(id("loadout/sample_miner")),
            ),
            false,
        )
        val tagId = ExpeditionaryHardcoreTags.LOOT_TABLE_CHARACTER_LOADOUT.location()
        return DataProvider.saveStable(cache, TagFile.CODEC, tagFile, pathProvider.json(tagId))
    }

    override fun getName() = "Expeditionary Hardcore Loot Table Tags"
}

