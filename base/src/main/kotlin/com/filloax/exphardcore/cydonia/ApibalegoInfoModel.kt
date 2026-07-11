package com.filloax.exphardcore.cydonia

import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.fxlib.api.json.BlockPosSerializer
import com.filloax.fxlib.api.json.UUIDSerializer
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos
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
) {
    companion object {
        fun fromLifeData(lifeData: PlayerLifeData) = ApibalegoPlayerLifeData(lifeData.name, lifeData.spawnPoint)
    }
}