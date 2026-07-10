import com.filloax.gradle.*

plugins {
    // see buildSrc
    id("com.filloax.gradle.multiloader-loader")
    // id("com.filloax.gradle.multiloader-gametest-loader")  // disabled: neoforge gametests don't work

    alias(libs.plugins.moddevgradle)
}

val utils = project.utils(versionCatalogs, ext)

val modid: String by project
val modVersion: String by project
val versionType: String? by project
val minecraftVersion = libs.versions.minecraft.asProvider().get()
val cydoniaMode = (property("cydoniaMode") as String).toBoolean()
val includeDeps = (property("includeDeps") as String).toBoolean()

val versionSuffix = if (versionType?.isBlank() == true) "" else "-$versionType"

version = "$modVersion-$minecraftVersion$versionSuffix-neoforge"

if (includeDeps) println("Including dependencies for test mode")

neoForge {
    version = libs.versions.neoforge.asProvider().get()

    runs {
        create("client") {
            client()
            ideName = "Expeditionary Hardcore - NeoForge Client"
        }

        create("server") {
            server()
            ideName = "Expeditionary Hardcore - NeoForge Server"
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            systemProperty("neoforge.enabledGameTestNamespaces", modid)

            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modid) {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation( libs.jsr305 )

    listOf(
        libs.kotlinforge,
        utils.getResourcefulConfig("neoforge"),
    ).forEach {
        implementation(it)
        if (includeDeps)
            jarJar(it)
    }

    utils.getFilloaxlib("neoforge").let{
        implementation(it) { exclude(module = "kotlin-stdlib") }
        jarJar(it)
    }
    utils.getApibalego("neoforge").let{
        if (cydoniaMode) {
            implementation(it) { exclude(module = "kotlin-stdlib") }
            jarJar(it)
        } else {
            compileOnly(it) { exclude(module = "kotlin-stdlib") }
            if (includeDeps)
                jarJar(it)
        }
    }
}
