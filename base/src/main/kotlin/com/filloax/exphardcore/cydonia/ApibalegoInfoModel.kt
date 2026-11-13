package com.filloax.exphardcore.cydonia

import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.fxlib.api.json.BlockPosSerializer
import com.filloax.fxlib.api.json.IdentifierSerializer
import com.filloax.fxlib.api.json.UUIDSerializer
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos
import net.minecraft.resources.Identifier
import java.util.UUID

@Serializable
data class ApibalegoLogbookCharacterData(
    @Serializable(with = UUIDSerializer::class)
    val playerId: UUID,
    val characterData: ApibalegoPlayerLifeData?,
    val diaryPages: List<String>,
    val diaryTitle: String?,
)

@Serializable
data class ApibalegoPlayerLifeHistory(
    @Serializable(with = UUIDSerializer::class)
    val playerId: UUID,
    val lives: List<ApibalegoPlayerLifeData>,
    val currentLifeIndex: Int,
)

@Serializable
data class ApibalegoPlayerLifeData(
    val name: String? = null,
    @Serializable(with = BlockPosSerializer::class)
    val spawnPoint: BlockPos,
    @Serializable(with = BlockPosSerializer::class)
    val deathPoint: BlockPos? = null,
    @Serializable(with = IdentifierSerializer::class)
    val modelId: Identifier,
    val spawnGameTime: Long? = null,
    val spawnRealTime: Long? = null,
    val deathGameTime: Long? = null,
    val deathRealTime: Long? = null,
) {
    companion object {
        fun fromLifeData(lifeData: PlayerLifeData) = ApibalegoPlayerLifeData(
            lifeData.name,
            lifeData.spawnPoint,
            lifeData.deathPoint,
            lifeData.modelId,
            lifeData.spawnGameTime,
            lifeData.spawnRealTime,
            lifeData.deathGameTime,
            lifeData.deathRealTime,
        )
    }
}