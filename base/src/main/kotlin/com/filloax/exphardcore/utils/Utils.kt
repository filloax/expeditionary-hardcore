package com.filloax.exphardcore.utils

import com.filloax.exphardcore.ExpeditionaryHardcore
import net.minecraft.resources.Identifier
import java.util.Properties

fun id(str: String): Identifier {
    return Identifier.fromNamespaceAndPath(ExpeditionaryHardcore.MOD_ID, str)
}

fun loadPropertiesFile(fileName: String): Map<String, String> {
    val properties = Properties()

    val classLoader = Thread.currentThread().contextClassLoader
    val inputStream = classLoader.getResourceAsStream(fileName)
        ?: throw IllegalArgumentException("Properties file not found: $fileName")

    inputStream.use {
        properties.load(it)
    }

    return properties.stringPropertyNames().associateWith { properties.getProperty(it) }
}