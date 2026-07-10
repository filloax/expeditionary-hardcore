package com.filloax.exphardcore.cydonia

import com.filloax.exphardcore.character.PlayerLifeData
import com.filloax.fxlib.api.json.BlockPosSerializer
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos

@Serializable
data class ApibalegoLogbookCharacterData(
    val characterData: ApibalegoPlayerLifeData?,
    val diaryPages: List<String>,
    val diaryTitle: String?,
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