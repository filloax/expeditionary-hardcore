package com.filloax.exphardcore.client

import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode
import net.minecraft.network.chat.Component

// Should have same gamemodes as vanilla + expedition
enum class ExpeditionGameModeOption(val gameMode: SelectedGameMode, val expedition: Boolean) {
    SURVIVAL(SelectedGameMode.SURVIVAL, false),
    HARDCORE(SelectedGameMode.HARDCORE, false),
    CREATIVE(SelectedGameMode.CREATIVE, false),
    EXPEDITION(SelectedGameMode.SURVIVAL, true);

    fun label(): Component =
        if (expedition) Component.translatable("exphardcore.gamemode.expedition")
        else gameMode.displayName

    companion object {
        fun from(gameMode: SelectedGameMode, expedition: Boolean): ExpeditionGameModeOption =
            if (expedition) EXPEDITION else when (gameMode) {
                SelectedGameMode.HARDCORE -> HARDCORE
                SelectedGameMode.CREATIVE -> CREATIVE
                else -> SURVIVAL
            }
    }
}
