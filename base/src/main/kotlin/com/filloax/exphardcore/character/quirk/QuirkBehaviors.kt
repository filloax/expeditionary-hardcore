package com.filloax.exphardcore.character.quirk

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.network.AmbientSoundsPacket
import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.entity.getPersistData
import com.filloax.fxlib.api.json.IdentifierSerializer
import com.filloax.fxlib.api.networking.sendPacket
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import kotlin.jvm.optionals.getOrNull
import kotlin.math.ceil

/**
 * Quirks that do more than modify attributes
 */
interface QuirkBehavior {
    /** [damage] is the vanilla fall damage result, only called when > 0; returns the replacement */
    fun onFallDamage(player: ServerPlayer, damage: Int): Int = damage

    fun onTick(player: ServerPlayer) = Unit
    val onTickPeriod: Int get() = 20
}

object LifeQuirkBehaviors {
    val FALL_IMPACT = id("fall_impact")
    val RANDOM_AMBIENT_SOUND = id("random_ambient_sound")

    val factories: Map<Identifier, (JsonObject) -> QuirkBehavior> = mapOf(
        FALL_IMPACT to { params ->
            FallImpactBehavior(Json.decodeFromJsonElement(FallImpactBehavior.Params.serializer(), params))
        },
        RANDOM_AMBIENT_SOUND to { params ->
            RandomAmbientSoundBehavior(Json.decodeFromJsonElement(RandomAmbientSoundBehavior.Params.serializer(), params))
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

class RandomAmbientSoundBehavior(private val params: Params) : QuirkBehavior {
    @Serializable
    data class Params(
        val tickCheckFrequencyTicks: Int = 20,
        val minDelayTicks: Int = 20*5,
        val chanceEachCheck: Double = 0.01,
        @Serializable(with = IdentifierSerializer::class)
        val applyEffect: Identifier? = Identifier.withDefaultNamespace("darkness"),
        val applyEffectDurationTicks: Int = 5*20,
    )

    companion object {
        const val DATA_TAG_LAST_PLAYED_TIME = "exphard_random_ambient_sound_last_played_time"
    }

    override val onTickPeriod: Int get() = params.tickCheckFrequencyTicks

    override fun onTick(player: ServerPlayer) {
        val persistData = player.getPersistData()
        val currentTick = player.tickCount
        val lastTimePlayed = persistData.getInt(DATA_TAG_LAST_PLAYED_TIME).getOrNull()?.takeIf { it < currentTick } ?: 0

        if (currentTick - lastTimePlayed < params.minDelayTicks) return

        if (player.random.nextDouble() < params.chanceEachCheck) {
            play(player)
            persistData.putInt(DATA_TAG_LAST_PLAYED_TIME, player.tickCount)
        }
    }

    private fun play(player: ServerPlayer) {
        ExpeditionaryHardcore.LOGGER.debug("Playing ambient sound for player {}", player)
        player.sendPacket(AmbientSoundsPacket())

        if (params.applyEffect != null) {
            val effect = BuiltInRegistries.MOB_EFFECT.get(params.applyEffect).getOrNull() ?: return
            player.addEffect(MobEffectInstance(effect, params.applyEffectDurationTicks, 0))
        }
    }
}