package com.filloax.exphardcore.client

import com.filloax.exphardcore.network.ServerboundCharacterCreationPacket
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket

class CharacterCreationScreen(
    private val lastScreen: Screen?,
    private val initialValue: String = "",
) : Screen(Component.translatable("exphardcore.screen.name_entry.title")) {
    private lateinit var nameEdit: EditBox
    private lateinit var saveButton: Button

    override fun init() {
        nameEdit = EditBox(font, width / 2 - 100, height / 2 - 10, 200, 20, LABEL)
        nameEdit.value = initialValue
        nameEdit.setMaxLength(32)
        nameEdit.setResponder { updateSaveButtonStatus() }
        addWidget(nameEdit)

        saveButton = addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onSave() }
                .bounds(width / 2 - 100, height / 2 + 20, 95, 20)
                .build()
        )
        addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL) { onClose() }
                .bounds(width / 2 + 5, height / 2 + 20, 95, 20)
                .build()
        )
        updateSaveButtonStatus()
    }

    override fun setInitialFocus() {
        setInitialFocus(nameEdit)
    }

    private fun updateSaveButtonStatus() {
        saveButton.active = nameEdit.value.isNotBlank()
    }

    private fun onSave() {
        minecraft.player?.connection?.send(ServerboundCustomPayloadPacket(createCharacterCreationPacket()))
        onClose()
    }

    private fun createCharacterCreationPacket(): ServerboundCharacterCreationPacket {
        val name = nameEdit.value
        return ServerboundCharacterCreationPacket(name)
    }

    override fun onClose() {
        minecraft.gui.setScreen(lastScreen)
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        graphics.centeredText(font, title, width / 2, height / 2 - 40, -1)
        graphics.text(font, LABEL, width / 2 - 100 + 1, height / 2 - 22, -6250336)
        nameEdit.extractRenderState(graphics, mouseX, mouseY, partialTick)
    }

    companion object {
        private val LABEL = Component.translatable("exphardcore.screen.name_entry.label")

        // Call this from wherever the menu should be opened (e.g. a keybind or a respawn hook).
        fun open(lastScreen: Screen? = null, initialValue: String = "") {
            Minecraft.getInstance().gui.setScreen(CharacterCreationScreen(lastScreen, initialValue))
        }
    }
}
