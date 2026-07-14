package com.filloax.exphardcore.character.quirk

import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.json.IdentifierSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import kotlin.math.ceil

/**
 * Quirks that do more than modify attributes
 */
interface QuirkBehavior {
    /** [damage] is the vanilla fall damage result, only called when > 0; returns the replacement */
    fun onFallDamage(player: ServerPlayer, damage: Int): Int = damage
    // TODO
    fun onTick(player: ServerPlayer) = Unit
    val onTickPeriod: Int get() = 20
}

object LifeQuirkBehaviors {
    val factories: Map<Identifier, (JsonObject) -> QuirkBehavior> = mapOf(
        id("fall_impact") to { params ->
            FallImpactBehavior(Json.decodeFromJsonElement(FallImpactBehavior.Params.serializer(), params))
        },
    )
}

/** Increased fall damage plus brief effects (blindness/slowness by default) on landing */
class FallImpactBehavior(private val params: Params) : QuirkBehavior {
    @Serializable
    data class Params(
        val damageMultiplier: Float = 1.25f,
        val effectDurationTicks: Int = 20,
        val effects: List<@Serializable(with = IdentifierSerializer::class) Identifier> = listOf(
            Identifier.withDefaultNamespace("blindness"),
            Identifier.withDefaultNamespace("slowness"),
        ),
        val effectAmplifier: Int = 0,
    )

    override fun onFallDamage(player: ServerPlayer, damage: Int): Int {
        params.effects.forEach { effectId ->
            BuiltInRegistries.MOB_EFFECT.get(effectId).ifPresent { holder ->
                player.addEffect(MobEffectInstance(holder, params.effectDurationTicks, params.effectAmplifier))
            }
        }
        return ceil(damage * params.damageMultiplier).toInt()
    }
}