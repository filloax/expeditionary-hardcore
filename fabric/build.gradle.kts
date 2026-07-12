import com.filloax.gradle.*

plugins {
	// see buildSrc
	id("com.filloax.gradle.multiloader-loader")
	id("com.filloax.gradle.multiloader-gametest-loader")

	alias(libs.plugins.loom)
}

val utils = project.utils(versionCatalogs, ext)

val modid: String by project
val modVersion: String by project
val versionType: String? by project
val minecraftVersion = libs.versions.minecraft.asProvider().get()
val cydoniaMode = (property("cydoniaMode") as String).toBoolean()
val includeDeps = (property("includeDeps") as String).toBoolean()

val versionSuffix = if (versionType?.isBlank() == true) "" else "-$versionType"

version = "$modVersion-$minecraftVersion$versionSuffix-fabric"

if (includeDeps) println("Including dependencies for test mode")

loom {
	mixin.defaultRefmapName = "${modid}.refmap.json"

	mods {
		register(modid) {
			sourceSet(sourceSets.main.get())
		}
	}

    runs {
        named("client") {
            displayName = "Expeditionary Hardcore - Fabric Client"
            appendProjectPathToDisplayName.set(false)

            client()
            generateRunConfig = true
        }

        named("server") {
            displayName = "Expeditionary Hardcore - Fabric Server"
            appendProjectPathToDisplayName.set(false)

            server()
            generateRunConfig = true
        }

        create("data") {
            displayName = "Expeditionary Hardcore - Data Generation"
            appendProjectPathToDisplayName.set(false)

            client()
            vmArg("-Dfabric-api.datagen")
            vmArg("-Dfabric-api.datagen.output-dir=${file("../base/src/generated/resources")}")
            vmArg("-Dfabric-api.datagen.modid=${modid}")

            runDir("build/datagen")
        }
    }
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = "${modid}_test"
        enableGameTests = true
        enableClientGameTests = true
        eula = true
    }
}

dependencies {
	minecraft( libs.minecraft )
	implementation( libs.jsr305 )

	implementation( libs.fabric )
	implementation( libs.fabric.api ) {
		exclude(module = "fabric-api-deprecated")
	}

	listOf(
		libs.fabric.kotlin,
		utils.getResourcefulConfig("fabric"),
	).forEach {
		implementation(it)
		if (includeDeps)
			include(it)
	}

	implementation( libs.kotlin.serialization ) { exclude(module = "kotlin-stdlib") }

	utils.getFilloaxlib("fabric").let{
		implementation(it) { exclude(module = "kotlin-stdlib") }
		include(it)
	}
	utils.getApibalego("fabric").let{
		if (cydoniaMode) {
			implementation(it) { exclude(module = "kotlin-stdlib") }
			include(it)
		} else {
			compileOnly(it) { exclude(module = "kotlin-stdlib") }
			if (includeDeps)
				include(it)
		}
	}

	// only for IDE testing
	localRuntime(libs.modmenu)
	localRuntime(libs.authme)

}
