package com.filloax.exphardcore.expedition

import com.filloax.exphardcore.utils.id
import com.mojang.brigadier.arguments.BoolArgumentType
import com.mojang.serialization.Codec
import net.minecraft.resources.Identifier
import net.minecraft.world.flag.FeatureFlagSet
import net.minecraft.world.level.gamerules.GameRule
import net.minecraft.world.level.gamerules.GameRuleCategory
import net.minecraft.world.level.gamerules.GameRuleType
import net.minecraft.world.level.gamerules.GameRules
import java.util.function.ToIntFunction

/**
 * The mod's world flag lives in a vanilla GameRule: it rides the normal
 * create-world -> level.dat -> server pipeline (no cross-side static) and is
 * `/gamerule`-settable. `ExpeditionMode` reads/writes it as the source of truth.
 */
object ExpeditionGameRules {
    val ID_EXPEDITION: Identifier = id("expedition")

    val EXPEDITION: GameRule<Boolean> = GameRule(
        GameRuleCategory.MISC,
        GameRuleType.BOOL,
        BoolArgumentType.bool(),
        { visitor, rule -> visitor.visitBoolean(rule) },
        Codec.BOOL,
        { if (it) 1 else 0 },
        false,
        FeatureFlagSet.of(),
    )

    fun register(register: (Identifier, GameRule<*>) -> Unit) {
        register(ID_EXPEDITION, EXPEDITION)
    }
}
