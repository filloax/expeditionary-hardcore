package com.filloax.exphardcore.data

import com.filloax.exphardcore.ExpeditionaryHardcoreTags
import com.filloax.exphardcore.character.PlayerModelDefinition
import com.filloax.exphardcore.character.PlayerModelDefinitions
import com.filloax.exphardcore.character.quirk.*
import com.filloax.exphardcore.item.ExpeditionaryHardcoreItems
import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.json.IdentifierSerializer
import com.filloax.fxlib.api.json.saveStable
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput
import net.minecraft.client.data.models.model.*
import net.minecraft.client.renderer.item.ClientItem
import net.minecraft.core.Holder
import net.minecraft.core.registries.Registries
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.resources.Identifier
import net.minecraft.tags.TagEntry
import net.minecraft.tags.TagFile
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Item
import java.util.concurrent.CompletableFuture
import kotlin.Double
import kotlin.Int

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
                "aeonia",
                "average-italian-nerd",
                "egobasego",
                "hime",
                "kris-shiny",
                "ninja",
                "omino-sekiro",
                "red",
                "ron-swanson",
                "scar",
                "sidecut-gurl",
                "skin1-corretta",
                "skin2-corretta",
                "skin3-corretta",
                "skin4-corretta",
                "skin5-corretta",
                "skin6-corretta",
                "sunburnt-white-man",
                "tequila-sunset",
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

    private fun modifier(
        attribute: Holder<Attribute>,
        amount: Double,
        operation: QuirkModifierOperation = QuirkModifierOperation.ADD_MULTIPLIED_TOTAL,
    ) = QuirkAttributeModifier(attribute.unwrapKey().orElseThrow().identifier(), operation, amount)

    private fun vanillaEffectQuirk(
        name: String,
        icon: Holder<MobEffect>,
        attributeModifiers: List<QuirkAttributeModifier> = listOf(),
        behavior: QuirkBehaviorRef? = null,
    ) = id(name) to LifeQuirkDefinition(
        icon = icon.unwrapKey().orElseThrow().identifier(),
        iconIsVanillaEffect = true,
        name = "effect.exphardcore.quirk.$name",
        description = "effect.exphardcore.quirk.$name.desc",
        attributeModifiers = attributeModifiers,
        behavior = behavior,
        builtin = true,
    )

    override fun run(cache: CachedOutput): CompletableFuture<*> {
        val quirks = mapOf(
            vanillaEffectQuirk(
                "slow", MobEffects.SLOWNESS,
                attributeModifiers = listOf(
                    modifier(Attributes.MOVEMENT_SPEED, -0.10),
                ),
            ),
            vanillaEffectQuirk(
                "frail", MobEffects.WITHER,
                attributeModifiers = listOf(
                    modifier(Attributes.MAX_HEALTH, -2.0, QuirkModifierOperation.ADD_VALUE),
                ),
            ),
            vanillaEffectQuirk(
                "clumsy", MobEffects.MINING_FATIGUE,
                attributeModifiers = listOf(
                    modifier(Attributes.ATTACK_SPEED, -0.10),
                ),
            ),
            vanillaEffectQuirk(
                "weak_ankles", MobEffects.SLOWNESS,
                attributeModifiers = listOf(
                    modifier(Attributes.SAFE_FALL_DISTANCE, -1.0, QuirkModifierOperation.ADD_VALUE),
                ),
            ),
            vanillaEffectQuirk(
                "smoker", MobEffects.WIND_CHARGED,
                attributeModifiers = listOf(
                    modifier(Attributes.OXYGEN_BONUS, -0.33, QuirkModifierOperation.ADD_MULTIPLIED_BASE),
                    modifier(Attributes.BURNING_TIME, 0.5, QuirkModifierOperation.ADD_MULTIPLIED_BASE),
                ),
            ),
            vanillaEffectQuirk(
                "weak_bones", MobEffects.WEAKNESS,
                attributeModifiers = listOf(
                    modifier(Attributes.BLOCK_BREAK_SPEED, -0.15, QuirkModifierOperation.ADD_MULTIPLIED_BASE),
                    modifier(Attributes.ATTACK_DAMAGE, -0.15, QuirkModifierOperation.ADD_MULTIPLIED_BASE),
                ),
            ),
            id("heavy_boned") to LifeQuirkDefinition(
                icon = id("heavy_boned"),
                iconIsVanillaEffect = false,
                name = "effect.exphardcore.quirk.heavy_boned",
                description = "effect.exphardcore.quirk.heavy_boned.desc",
                behavior = QuirkBehaviorRef(
                    LifeQuirkBehaviors.FALL_IMPACT,
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
            vanillaEffectQuirk(
                "paranoia", MobEffects.DARKNESS,
                behavior = QuirkBehaviorRef(
                    LifeQuirkBehaviors.RANDOM_AMBIENT_SOUND,
                    Json.encodeToJsonElement(
                        RandomAmbientSoundBehavior.Params(
                            tickCheckFrequencyTicks = 20,
                            minDelayTicks = 20*5,
                            chanceEachCheck = 0.01,
                            applyEffect = MobEffects.DARKNESS.unwrapKey().orElseThrow().identifier(),
                            applyEffectDurationTicks = 5*20,
                        )
                    ).jsonObject,
                ),
            ),
            vanillaEffectQuirk(
                "hungry", MobEffects.HUNGER,
                behavior = QuirkBehaviorRef(
                    LifeQuirkBehaviors.HUNGER_MODIFIER,
                    Json.encodeToJsonElement(
                        HungerModifierBehavior.Params(exhaustionMultiplier = 1.05f)
                    ).jsonObject,
                ),
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
                "cook",
                "exorcist",
                "explorer",
                "fisherman",
                "goldminer",
                "gravedigger",
                "lumberjack",
                "planeswalker",
                "scout",
                "shepard",
                "swordman",
                "thinkerer",
            ).map { TagEntry.element(id("loadout/$it")) },
            false,
        )
        val tagId = ExpeditionaryHardcoreTags.LOOT_TABLE_CHARACTER_LOADOUT.location()
        return DataProvider.saveStable(cache, TagFile.CODEC, tagFile, pathProvider.json(tagId))
    }

    override fun getName() = "Expeditionary Hardcore Loot Table Tags"
}

