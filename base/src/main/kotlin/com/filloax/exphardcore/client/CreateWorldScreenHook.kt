package com.filloax.exphardcore.client

import com.filloax.exphardcore.expedition.ExpeditionGameRules
import com.filloax.exphardcore.mixin.client.GridLayoutAccessor
import com.filloax.exphardcore.mixin.client.LayoutChildWrapperAccessor
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.components.CycleButton
import net.minecraft.client.gui.components.tabs.TabNavigationBar
import net.minecraft.client.gui.layouts.GridLayout
import net.minecraft.client.gui.layouts.LayoutElement
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

        val navBar = screen.children().filterIsInstance<TabNavigationBar>().firstOrNull() ?: return
        val gameTabCells = navBar.tabs
            .mapNotNull { (it.layout as? GridLayout)?.let(::gridCells) }
            .firstOrNull { cells -> cells.any { it.child.isGameModeButton() } } ?: return

        val gameModeCell = gameTabCells.first { it.child.isGameModeButton() }
        val vanillaButton = gameModeCell.child as AbstractWidget
        val difficultyButton = gameTabCells
            .firstOrNull { it.child is CycleButton<*> && (it.child as CycleButton<*>).value is Difficulty }
            ?.child as? CycleButton<*>

        val uiState = screen.uiState
        val option = ExpeditionGameModeOption.from(uiState.gameMode, uiState.gameRules.get(ExpeditionGameRules.EXPEDITION))
        val replacement = buildGameModeButton(vanillaButton, uiState, option)

        // Ensure it goes in the right tab
        gameModeCell.setChild(replacement)
        swap(vanillaButton, replacement)

        uiState.addListener { st ->
            replacement.value = ExpeditionGameModeOption.from(st.gameMode, st.gameRules.get(ExpeditionGameRules.EXPEDITION))
            replacement.active = !st.isDebug
        }

        if (difficultyButton != null) {
            // Runs after vanilla's own difficulty listener, so it wins
            uiState.addListener { st -> enforceDifficultyLock(st, difficultyButton) }
            enforceDifficultyLock(uiState, difficultyButton)
        }
    }

    private fun gridCells(grid: GridLayout): List<LayoutChildWrapperAccessor> =
        (grid as GridLayoutAccessor).children.map { it as LayoutChildWrapperAccessor }

    private fun LayoutElement.isGameModeButton() = this is CycleButton<*> && value is SelectedGameMode

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
