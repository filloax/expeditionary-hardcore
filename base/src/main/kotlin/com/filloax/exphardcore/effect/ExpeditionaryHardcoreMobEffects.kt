package com.filloax.exphardcore.effect

import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.registration.RegistryHolderDelegate
import net.minecraft.core.Holder
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory

object ExpeditionaryHardcoreMobEffects {
    val all = mutableMapOf<Identifier, RegistryHolderDelegate<MobEffect>>()

    // shared marker for every life quirk; the actual per-quirk icon/name/description
    // are rendered client-side (see QuirkEffectHover) rather than via separate effects
    val LIFE_QUIRK by make("life_quirk", QuirkMarkerEffect(MobEffectCategory.NEUTRAL, 0x8a7f66))

    private fun make(name: String, effect: MobEffect) = RegistryHolderDelegate<MobEffect>(id(name), effect).apply {
        if (all.containsKey(id))
            throw IllegalArgumentException("Effect $name already registered!")
        all[id] = this
    }

    fun registerEffects(registrator: (Identifier, MobEffect) -> Holder<MobEffect>) {
        all.values.forEach {
            it.initHolder(registrator(it.id, it.value))
        }
    }
}

// vanilla MobEffect constructor is protected; exists purely to mark the
// player's current quirk with an icon in the effects UI
class QuirkMarkerEffect(category: MobEffectCategory, color: Int) : MobEffect(category, color)
