package com.filloax.exphardcore.data

import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.minecraft.client.data.models.model.ItemModelUtils
import net.minecraft.client.data.models.model.ModelInstance
import net.minecraft.client.data.models.model.ModelLocationUtils
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.client.data.models.model.TextureMapping
import net.minecraft.client.renderer.item.ClientItem
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
    }
}

/**
 * Vanilla's own ItemModelGenerators only builds models for the hardcoded vanilla item list
 * (its per-item methods are private), so modded items get their models/item-defs written
 * by hand here instead of through FabricModelProvider/ItemModelGenerators.
 *
 * All our items are flat (book-like) for now; give an item a bespoke [ClientItem] here if
 * that stops being true.
 */
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

