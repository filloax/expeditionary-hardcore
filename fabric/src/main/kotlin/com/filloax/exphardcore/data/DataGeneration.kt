package com.filloax.exphardcore.data

import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import com.filloax.exphardcore.character.PlayerModelDefinition
import com.filloax.exphardcore.character.PlayerModelDefinitions
import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.json.saveStable
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.minecraft.client.data.models.model.ItemModelUtils
import net.minecraft.client.data.models.model.ModelInstance
import net.minecraft.client.data.models.model.ModelLocationUtils
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.client.data.models.model.TextureMapping
import net.minecraft.client.renderer.item.ClientItem
import net.minecraft.core.HolderLookup
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.resources.Identifier
import net.minecraft.world.item.Item
import java.util.concurrent.CompletableFuture
import kotlin.collections.set

class ExpeditionaryHardcoreDataGenerator : DataGeneratorEntrypoint {
    override fun onInitializeDataGenerator(fabricDataGenerator: FabricDataGenerator) {
        fabricDataGenerator.createPack().addProvider(::ItemModelProvider)
        fabricDataGenerator.createPack().addProvider(::PlayerModelDefinitionProvider)
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



class TagProviderLootTable(output: FabricPackOutput, registries: CompletableFuture<HolderLookup.Provider>): LootTableTag(output, registries) {
    override fun addTags(arg: HolderLookup.Provider) {
        getOrCreateTagBuilder(ItemTags.DECORATED_POT_SHERDS)
            .add(GrowssethItems.GROWSSETH_POTTERY_SHERD)
    }
}

