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

// FilloaxLib bundles its own AT (needed e.g. for FxSavedData) in its jar, but ModDevGradle only
// bakes ATs declared by this project into the dev-environment Minecraft jar - it does not pick up
// ATs shipped inside dependency jars. So extract it here and register it explicitly, or dev runs
// (client/gametest) crash with IllegalAccessError as soon as any FxSavedData loads.
// FilloaxLib now publishes this properly (neoForge.accessTransformers.publish(...), see its
// neoforge/build.gradle.kts) as of the version after 0.40.0-26.2. Once exphardcore is bumped to
// consume that release, replace this whole block with the one-liner in the dependencies block:
//   accessTransformers(utils.getFilloaxlib("neoforge"))
//val filloaxlibAtDep: Configuration by configurations.creating {
//    isCanBeConsumed = false
//    isCanBeResolved = true
//}
//
//val filloaxlibAtFile = layout.buildDirectory.file("filloaxlibAt/META-INF/accesstransformer.cfg")
//
//val extractFilloaxlibAt = tasks.register<Copy>("extractFilloaxlibAccessTransformer") {
//    from({ zipTree(filloaxlibAtDep.singleFile) }) {
//        include("META-INF/accesstransformer.cfg")
//    }
//    into(layout.buildDirectory.dir("filloaxlibAt"))
//}

neoForge {
    version = libs.versions.neoforge.asProvider().get()

    accessTransformers.files.from(project(":base").file("src/main/resources/META-INF/accesstransformer.cfg"))

//    accessTransformers.files.from(filloaxlibAtFile).builtBy(extractFilloaxlibAt)

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
//        filloaxlibAtDep(it) { exclude(module = "kotlin-stdlib") }
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
