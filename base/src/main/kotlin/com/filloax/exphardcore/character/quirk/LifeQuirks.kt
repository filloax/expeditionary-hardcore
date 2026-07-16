package com.filloax.exphardcore.character.quirk

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig
import com.filloax.fxlib.api.json.IdentifierSerializer
import com.filloax.fxlib.api.json.IntToBooleanSerializer
import com.filloax.fxlib.api.json.KotlinJsonResourceReloadListener
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import kotlin.collections.set

object LifeQuirksResolver {
    /**
     * If API enabled, and builtins are disabled, will
     * remove builtins to allow overriding the list entirely
     */
    val quirks
        get() = if (!disableBuiltins)
            LifeQuirkDefinitions.all
        else
            LifeQuirkDefinitions.all.filterValues { !it.builtin }

    val enableQuirks: Boolean
        get() = ExpeditionaryHardcoreConfig.enableLifeQuirks && (
            if (ExpeditionaryHardcore.modCompat.isApibalegoLoaded)
                LifeQuirksApibalego.apiEnabledQuirks
            else
                true
        )

    private val disableBuiltins: Boolean
        get() = (
                if (ExpeditionaryHardcore.modCompat.isApibalegoLoaded)
                    LifeQuirksApibalego.apiDisabledBuiltins
                else
                    false
                )

    fun init() {
        LifeQuirksApibalego.init()
    }
}

/**
 * A minor per-life effect: attribute tweaks and/or hardcoded behavior.
 *
 * @param icon Texture resource id for the quirk's icon, resolved to
 * `<namespace>/textures/quirk_icon/<icon>.png`, or to a vanilla effect ID,
 * depending on [iconIsVanillaEffect]
 * @param attributeModifiers Simple numeric changes to attributes
 * @param behavior Custom behavior that does more than change attributes
 */
@Serializable
data class LifeQuirkDefinition(
    @Serializable(with = IdentifierSerializer::class)
    val icon: Identifier,
    val iconIsVanillaEffect: Boolean,
    val weight: Int = 10,
    val enabled: Boolean = true,
    // translation key or plain test (null: uses effect name)
    val name: String? = null,
    val description: String? = null,
    val attributeModifiers: List<QuirkAttributeModifier> = listOf(),
    val behavior: QuirkBehaviorRef? = null,
    val builtin: Boolean = false,
) {
}

@Serializable
data class QuirkAttributeModifier(
    @Serializable(with = IdentifierSerializer::class)
    val attribute: Identifier,
    val operation: QuirkModifierOperation,
    val amount: Double,
)

@Serializable
enum class QuirkModifierOperation(val mcOperation: AttributeModifier.Operation) {
    @SerialName("add_value") ADD_VALUE(AttributeModifier.Operation.ADD_VALUE),
    @SerialName("add_multiplied_base") ADD_MULTIPLIED_BASE(AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
    @SerialName("add_multiplied_total") ADD_MULTIPLIED_TOTAL(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
}

@Serializable
data class QuirkBehaviorRef(
    @Serializable(with = IdentifierSerializer::class)
    val type: Identifier,
    val params: JsonObject = JsonObject(emptyMap()),
)

/**
 * Client-facing part of a rolled quirk, stored in the life data so it syncs
 */
@Serializable
data class LifeQuirkClientInfo(
    @Serializable(with = IdentifierSerializer::class)
    val quirkId: Identifier,
    @Serializable(with = IdentifierSerializer::class)
    val icon: Identifier,
    @Serializable(with = IntToBooleanSerializer::class)
    val iconIsVanillaEffect: Boolean,
    val name: String? = null,
    val description: String? = null,
) {
}


object LifeQuirkDefinitions {
    const val DIRECTORY = "exphard_quirks"

    private var definitions: Map<Identifier, LifeQuirkDefinition> = mapOf()
    private var behaviors: Map<Identifier, QuirkBehavior> = mapOf()

    val all: Map<Identifier, LifeQuirkDefinition> get() = definitions

    operator fun get(id: Identifier): LifeQuirkDefinition? = definitions[id]

    fun behaviorOf(quirkId: Identifier): QuirkBehavior? = behaviors[quirkId]

    class ReloadListener : KotlinJsonResourceReloadListener(Json, DIRECTORY) {
        override fun apply(
            elements: Map<Identifier, JsonElement>,
            resourceManager: ResourceManager,
            profiler: ProfilerFiller,
        ) {
            val defs = mutableMapOf<Identifier, LifeQuirkDefinition>()
            val behaviorInstances = mutableMapOf<Identifier, QuirkBehavior>()
            elements.forEach { (id, element) ->
                try {
                    val def = Json.decodeFromJsonElement(LifeQuirkDefinition.serializer(), element)
                    validateQuirk(def) { behaviorInstances[id] = it }
                    defs[id] = def
                } catch (e: Exception) {
                    ExpeditionaryHardcore.LOGGER.error("Failed to load life quirk {}", id, e)
                }
            }
            definitions = defs
            behaviors = behaviorInstances
        }

        private fun validateQuirk(def: LifeQuirkDefinition, behaviorConsumer: (QuirkBehavior) -> Unit) {
            // resolve behaviors now so bad data fails at reload
            def.behavior?.let { ref ->
                val factory = LifeQuirkBehaviors.factories[ref.type]
                    ?: error("unknown behavior type ${ref.type}")
                behaviorConsumer(factory(ref.params))
            }
            // check attributes
            def.attributeModifiers.forEach { mod ->
                if (!BuiltInRegistries.ATTRIBUTE.get(mod.attribute).isPresent) {
                    error("attribute ${mod.attribute} is not a registered attribute")
                }
            }
        }
    }
}
