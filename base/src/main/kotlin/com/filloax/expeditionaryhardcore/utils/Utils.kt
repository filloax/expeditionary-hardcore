package com.filloax.expeditionaryhardcore.utils

import com.filloax.expeditionaryhardcore.ExpeditionaryHardcore
import net.minecraft.resources.Identifier

fun id(str: String): Identifier {
    return Identifier.fromNamespaceAndPath(ExpeditionaryHardcore.MOD_ID, str)
}