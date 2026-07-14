package com.filloax.exphardcore.character.quirk

import com.filloax.exphardcore.ExpeditionaryHardcore
import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.exphardcore.character.getAllExpeditionLives
import com.filloax.exphardcore.character.getExpeditionLifeOrNull
import com.filloax.exphardcore.character.syncExpeditionLife
import com.filloax.exphardcore.config.ExpeditionaryHardcoreConfig
import com.filloax.exphardcore.effect.ExpeditionaryHardcoreMobEffects
import com.filloax.exphardcore.utils.id
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random

object LifeQuirkHandler {
    // one shared modifier id: ids are unique per attribute instance, so a single
    // quirk can still modify multiple attributes
    private val QUIRK_MODIFIER_ID = id("life_quirk")
    // separate rolls from loadout
    private const val QUIRK_SEED_SALT = 0x7175697268636FL
    private const val RECHECK_INTERVAL_TICKS = 100

    private val warnedMissingQuirks = mutableSetOf<Identifier>()

    fun newQuirkForPlayer(player: ServerPlayer, lifeData: PlayerLifeData) {
        val prevLifeData = player.getAllExpeditionLives().dropLast(1)
        assignQuirk(player, lifeData, rollQuirk(prevLifeData, lifeData.random(QUIRK_SEED_SALT)))
    }

    fun rerollQuirk(player: ServerPlayer, random: Random): Identifier? {
        val lifeData = player.getExpeditionLifeOrNull() ?: return null
        val prevLifeData = player.getAllExpeditionLives().dropLast(1)

        val quirkId = rollQuirk(prevLifeData, random)
        assignQuirk(player, lifeData, quirkId)
        player.syncExpeditionLife()
        return quirkId
    }

    fun setQuirk(player: ServerPlayer, quirkId: Identifier?): Boolean {
        if (quirkId != null && LifeQuirkDefinitions[quirkId] == null) return false
        val lifeData = player.getExpeditionLifeOrNull() ?: return false
        assignQuirk(player, lifeData, quirkId)
        player.syncExpeditionLife()
        return true
    }

    private fun rollQuirk(previousLives: List<PlayerLifeData>, random: Random): Identifier? {
        if (!ExpeditionaryHardcoreConfig.enableLifeQuirks) return null
        if (random.nextDouble() >= ExpeditionaryHardcoreConfig.quirkChance) return null
        return pickWeighted(previousLives, LifeQuirkDefinitions.all.filterValues { it.enabled }, random)
    }

    private fun assignQuirk(player: ServerPlayer, lifeData: PlayerLifeData, quirkId: Identifier?) {
        removeQuirkArtifacts(player)
        lifeData.quirk = quirkId?.let { qid ->
            LifeQuirkDefinitions[qid]?.let { def ->
                LifeQuirkClientInfo(qid, def.icon, def.name, def.description)
            }
        }
        lifeData.setDirty()
        applyQuirk(player)
    }

    internal fun pickWeighted(previousLives: List<PlayerLifeData>, defs: Map<Identifier, LifeQuirkDefinition>, random: Random): Identifier? {
        val unusedWeightMultiplier = 10
        val usedQuirks = previousLives.mapNotNull { it.quirk?.quirkId }.toSet()

        val weightedEntries = defs.mapValues { (id, def) -> def.weight.coerceAtLeast(0) * (if (id in usedQuirks) unusedWeightMultiplier else 1) }
            .entries
            // sorted so the pick is deterministic for a given seed regardless of datapack load order
            .sortedBy { it.key.toString() }

        val total = weightedEntries.sumOf { it.value }
        if (total <= 0) return null
        var roll = random.nextInt(total)
        for ((quirkId, weight) in weightedEntries) {
            roll -= weight
            if (roll < 0) return quirkId
        }
        return null
    }

    /**
     * Idempotent; also re-applies after relogs (transient attribute modifiers
     * aren't saved) and periodically to restore the marker after milk buckets
     */
    fun applyQuirk(player: ServerPlayer) {
        val quirkInfo = player.getExpeditionLifeOrNull()?.quirk ?: return
        val def = LifeQuirkDefinitions[quirkInfo.quirkId]
        if (def == null) {
            if (warnedMissingQuirks.add(quirkInfo.quirkId)) {
                ExpeditionaryHardcore.LOGGER.warn("Life quirk {} no longer exists, skipping (datapack changed?)", quirkInfo.quirkId)
            }
            return
        }

        if (!player.hasEffect(ExpeditionaryHardcoreMobEffects.LIFE_QUIRK)) {
            // showIcon=false: keeps it out of the gameplay HUD corner overlay while
            // still showing in the inventory effects list
            player.addEffect(MobEffectInstance(ExpeditionaryHardcoreMobEffects.LIFE_QUIRK, MobEffectInstance.INFINITE_DURATION, 0, false, false, false))
        }

        def.attributeModifiers.forEach { mod ->
            val instance = BuiltInRegistries.ATTRIBUTE.get(mod.attribute).getOrNull()?.let(player::getAttribute) ?: return@forEach
            val modifier = AttributeModifier(QUIRK_MODIFIER_ID, mod.amount, mod.operation.mcOperation)
            if (instance.getModifier(QUIRK_MODIFIER_ID) != modifier) {
                instance.addOrUpdateTransientModifier(modifier)
            }
        }

        if (player.health > player.maxHealth) {
            player.health = player.maxHealth
        }
    }

    private fun removeQuirkArtifacts(player: ServerPlayer) {
        BuiltInRegistries.ATTRIBUTE.listElements().forEach { holder ->
            player.getAttribute(holder)?.removeModifier(QUIRK_MODIFIER_ID)
        }
        player.removeEffect(ExpeditionaryHardcoreMobEffects.LIFE_QUIRK)
    }

    object MixinHelpers {
        @JvmStatic
        fun modifyFallDamage(player: ServerPlayer, damage: Int): Int {
            val quirkInfo = player.getExpeditionLifeOrNull()?.quirk ?: return damage
            val behavior = LifeQuirkDefinitions.behaviorOf(quirkInfo.quirkId) ?: return damage
            return behavior.onFallDamage(player, damage)
        }
    }

    object Callbacks {
        fun onPlayerServerJoin(player: ServerPlayer) {
            applyQuirk(player)
        }

        fun onPlayerServerTick(player: ServerPlayer) {
        }

        fun onStartServerTick(server: MinecraftServer) {
            if (server.tickCount % RECHECK_INTERVAL_TICKS != 0) return
            server.playerList.players.forEach { applyQuirk(it) }
        }
    }
}
