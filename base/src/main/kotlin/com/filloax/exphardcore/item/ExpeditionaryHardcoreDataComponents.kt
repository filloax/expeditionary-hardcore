package com.filloax.exphardcore.item

import com.filloax.exphardcore.utils.id
import net.minecraft.core.component.DataComponentType
import net.minecraft.resources.Identifier

object ExpeditionaryHardcoreDataComponents {
    @JvmField
    val LOGBOOK_OWNER: DataComponentType<LogbookOwner> = DataComponentType.builder<LogbookOwner>()
        .persistent(LogbookOwner.CODEC)
        .networkSynchronized(LogbookOwner.STREAM_CODEC)
        .build()

    fun registerDataComponents(registrator: (Identifier, DataComponentType<*>) -> Unit) {
        registrator(id("logbook_owner"), LOGBOOK_OWNER)
    }
}
