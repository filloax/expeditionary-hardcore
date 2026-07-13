package com.filloax.exphardcore.network

import com.filloax.exphardcore.utils.id
import com.filloax.fxlib.api.networking.TrackedEntityData
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.resources.Identifier
import java.util.*

val DATA_TYPE_PLAYER_MODEL = id("player_model")

/**
 * For each player, its model
 */
val DATA_PLAYER_MODEL = TrackedEntityData(
    DATA_TYPE_PLAYER_MODEL,
    ByteBufCodecs.optional(Identifier.STREAM_CODEC),
)

fun registerAllTrackedData() {
    DATA_PLAYER_MODEL.register()
}