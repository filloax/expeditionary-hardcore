import com.filloax.gradle.*
import java.security.MessageDigest

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

	accessWidenerPath = file("src/main/resources/exphardcore.accesswidener")

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
            runDirectory.set(project.layout.projectDirectory.dir("run/client"))
        }

        named("server") {
            displayName = "Expeditionary Hardcore - Fabric Server"
            appendProjectPathToDisplayName.set(false)

            server()
            generateRunConfig = true
            runDirectory.set(project.layout.projectDirectory.dir("run/server"))
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
			implementation(it) {
				exclude(module = "kotlin-stdlib")
				// exclude modmenu from apibalego until it gets fixed on that side
				// I say, as if I wasn't the one that should fix it
				exclude(group = "com.terraformersmc", module = "modmenu")
//				capabilities {
//					requireCapability("com.github.filloax:apibalego-fabric")
//				}
			}
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

class DatapackInfo(val name: String, val id: String, val url: String, val sha: String, zipPath: String, dirPath: String) {
	val zipPath = layout.buildDirectory.file(zipPath)
	val dirPath = layout.buildDirectory.dir(dirPath)
}

// Cydonia mode: bundle No Spoiler Recipe Book datapack + maybe others later in jar
if (cydoniaMode) {

	val dataPacks = listOf(
		DatapackInfo(
			"No Spoiler Recipe Book",
			"no-spoiler-recipe-book",
			"https://cdn.modrinth.com/data/nCce3xAY/versions/7UU6onTJ/No%20Spoiler%20Recipe%20Book%2026.2.zip",
			"3da05b45645252e66f9087981791e7c95f6c3ca6",
			"datapacks/no-spoiler-recipe-book.zip",
			"datapacks/no-spoiler-recipe-book"
		)
	)

	val downloadDatapacks by tasks.registering {
		description = "Downloads the bundled datapacks for Cydonia mode"

		dataPacks.forEach { pack ->
			inputs.property("url-${pack.id}", pack.url)
			inputs.property("sha1-${pack.id}", pack.sha)
		}
		outputs.files(dataPacks.map { it.zipPath })

		doLast {
			dataPacks.forEach { pack ->
				val dest = pack.zipPath.get().asFile
				dest.parentFile.mkdirs()
				println("Downloading ${pack.name} from ${pack.url} to ${dest.absolutePath}")
				uri(pack.url).toURL().openStream().use { input ->
					dest.outputStream().use { input.copyTo(it) }
				}
				println("Downloaded ${pack.name} to ${dest.absolutePath}")
				val sha1 = MessageDigest.getInstance("SHA-1").digest(dest.readBytes())
					.joinToString("") { "%02x".format(it) }
				if (sha1 != pack.sha) {
					dest.delete()
					throw GradleException("${pack.name} checksum mismatch: expected ${pack.sha}, got $sha1")
				}
			}
		}
	}

	val extractDatapacks by tasks.registering(Copy::class) {
		description = "Extracts the builtin datapacks into the mod resources"
		dependsOn(downloadDatapacks)
		dataPacks.forEach { pack ->
			// skip its pack.mcmeta/pack.png, the mod jar already has its own
			from(zipTree(pack.zipPath.get().asFile)) { include("data/**") }
			into(pack.dirPath)
		}
	}

	tasks.processResources {
		from(extractDatapacks)
	}
}
