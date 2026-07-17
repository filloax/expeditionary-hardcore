package com.filloax.exphardcore.client

import com.filloax.exphardcore.expedition.ExpeditionGameRules
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState.SelectedGameMode
import net.minecraft.network.chat.Component
import net.minecraft.world.Difficulty

/**
 * Helper to swap gamemode button, delegates mechanics to loader
 */
object CreateWorldScreenHook {
    private val GAME_MODE_NAME: Component = Component.translatable("selectWorld.gameMode")

    fun install(
        screen: Screen,
        swap: (vanilla: AbstractWidget, replacement: AbstractWidget) -> Unit,
    ) {
        if (screen !is CreateWorldScreen) return

        val children = screen.children()
        val gameModeButton = children.firstOrNull {
            it is CycleButton<*> && it.value is SelectedGameMode
        } as? AbstractWidget ?: return
        val difficultyButton = children.firstOrNull {
            it is CycleButton<*> && it.value is Difficulty
        } as? CycleButton<*>

        val uiState = screen.uiState
        val option = ExpeditionGameModeOption.from(uiState.gameMode, uiState.gameRules.get(ExpeditionGameRules.EXPEDITION))
        swap(gameModeButton, buildGameModeButton(gameModeButton, uiState, option))

        if (difficultyButton != null) {
            // Runs after vanilla's own difficulty listener, so it wins
            uiState.addListener { st -> enforceDifficultyLock(st, difficultyButton) }
            enforceDifficultyLock(uiState, difficultyButton)
        }
    }

    private fun buildGameModeButton(
        vanilla: AbstractWidget,
        uiState: WorldCreationUiState,
        option: ExpeditionGameModeOption,
    ): CycleButton<ExpeditionGameModeOption> =
        CycleButton.builder({ it.label() }, option)
            .withValues(ExpeditionGameModeOption.entries)
            .create(vanilla.x, vanilla.y, vanilla.width, vanilla.height, GAME_MODE_NAME) { _, value ->
                // Need to set on uiState so we can send to server as it gets created
                uiState.gameRules.rules.set(ExpeditionGameRules.EXPEDITION, value.expedition)
                // Delegate to the real game-mode change for its side effects.
                uiState.gameMode = value.gameMode
                if (value.expedition) uiState.difficulty = Difficulty.HARD
            }

    private fun enforceDifficultyLock(uiState: WorldCreationUiState, difficultyButton: CycleButton<*>) {
        if (!uiState.gameRules.get(ExpeditionGameRules.EXPEDITION)) return
        if (difficultyButton.value != Difficulty.HARD) {
            @Suppress("UNCHECKED_CAST")
            (difficultyButton as CycleButton<Difficulty>).value = Difficulty.HARD
        }
        difficultyButton.active = false
    }
}
