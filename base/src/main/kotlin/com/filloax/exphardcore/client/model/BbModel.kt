package com.filloax.exphardcore.client.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

private val JSON = Json { ignoreUnknownKeys = true }

/**
 * Minimal representation of a Blockbench project file (.bbmodel), limited to what
 * is needed to build a vanilla entity model out of it
 */
class BbModel(
    val name: String,
    val textureWidth: Int,
    val textureHeight: Int,
    val roots: List<BbNode>,
    /** Raw PNG bytes of the first texture, if embedded in the file */
    val texturePng: ByteArray?,
) {
    companion object {
        fun parse(text: String): BbModel = parse(JSON.decodeFromString<BbFile>(text))

        private fun parse(file: BbFile): BbModel {
            if (!file.meta.boxUv)
                throw BbModelParseException("Only box_uv models are supported (vanilla models are box-UV only)")

            val cubesByUuid = file.elements
                .filter { it.type == "cube" && it.export }
                .associateBy { it.uuid }
            val groupsByUuid = file.groups.associateBy { it.uuid }

            val roots = file.outliner.mapNotNull { parseNode(it, cubesByUuid, groupsByUuid) }

            return BbModel(
                name = file.name,
                textureWidth = file.resolution.width,
                textureHeight = file.resolution.height,
                roots = roots,
                texturePng = file.textures.firstOrNull()?.source?.let(::decodeDataUri),
            )
        }

        private fun parseNode(
            element: JsonElement,
            cubes: Map<String, BbCubeData>,
            groups: Map<String, BbGroupData>,
        ): BbNode? {
            // outliner entries are either plain uuid strings (cubes) or group objects
            if (element is JsonPrimitive) return cubes[element.content]?.let { BbNode.Cube(it.toBbCube()) }

            val obj = element.jsonObject
            val uuid = obj["uuid"]?.jsonPrimitive?.content
                ?: throw BbModelParseException("outliner entry is missing uuid")
            // newer format versions keep group data in the top-level "groups" array,
            // older ones inline it in the outliner node
            val data = groups[uuid] ?: JSON.decodeFromJsonElement<BbGroupData>(obj)
            if (data.export == false) return null

            return BbNode.Group(
                name = data.name,
                origin = data.origin,
                rotation = data.rotation,
                children = obj["children"]?.jsonArray
                    ?.mapNotNull { parseNode(it, cubes, groups) }
                    ?: emptyList(),
            )
        }

        private fun BbCubeData.toBbCube() = BbCube(
            uuid = uuid,
            name = name,
            from = from,
            to = to,
            origin = origin,
            rotation = rotation,
            uvOffset = uvOffset?.let { it[0] to it[1] } ?: (0 to 0),
            inflate = inflate,
            mirror = mirror,
        )

        private fun decodeDataUri(source: String): ByteArray? {
            val base64 = source.substringAfter("base64,", "")
            if (base64.isEmpty()) return null
            return Base64.getDecoder().decode(base64)
        }
    }
}

// Pre-parsing, uses k serialization
@Serializable
private data class BbFile(
    val name: String = "unnamed",
    val meta: BbMetaData = BbMetaData(),
    val resolution: BbResolutionData,
    val elements: List<BbCubeData> = emptyList(),
    val groups: List<BbGroupData> = emptyList(),
    val outliner: List<JsonElement> = emptyList(),
    val textures: List<BbTextureData> = emptyList(),
)

@Serializable
private data class BbMetaData(
    @SerialName("box_uv") val boxUv: Boolean = false,
)

@Serializable
private data class BbResolutionData(
    val width: Int,
    val height: Int,
)

@Serializable
private data class BbTextureData(
    val source: String? = null,
)

@Serializable
private data class BbGroupData(
    val uuid: String,
    val name: String = "group",
    val origin: FloatArray = FloatArray(3),
    val rotation: FloatArray = FloatArray(3),
    val export: Boolean? = null,
)

// covers both cube elements and other element types (like locators), filtered by [type] later
@Serializable
private data class BbCubeData(
    val uuid: String,
    val type: String? = null,
    val name: String = "cube",
    val export: Boolean = true,
    val from: FloatArray = FloatArray(3),
    val to: FloatArray = FloatArray(3),
    val origin: FloatArray = FloatArray(3),
    val rotation: FloatArray = FloatArray(3),
    @SerialName("uv_offset") val uvOffset: List<Int>? = null,
    val inflate: Float = 0f,
    @SerialName("mirror_uv") val mirror: Boolean = false,
)

sealed interface BbNode {
    class Group(
        val name: String,
        val origin: FloatArray,
        val rotation: FloatArray,
        val children: List<BbNode>,
    ) : BbNode

    class Cube(val cube: BbCube) : BbNode
}

class BbCube(
    val uuid: String,
    val name: String,
    val from: FloatArray,
    val to: FloatArray,
    val origin: FloatArray,
    val rotation: FloatArray,
    val uvOffset: Pair<Int, Int>,
    val inflate: Float,
    val mirror: Boolean,
) {
    fun hasRotation() = rotation.any { it != 0f }
}

class BbModelParseException(message: String) : Exception(message)
