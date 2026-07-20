package com.filloax.exphardcore.client

import com.filloax.exphardcore.network.ServerboundCharacterCreationPacket
import com.filloax.exphardcore.utils.id
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket

class CharacterCreationScreen(
    private val lastScreen: Screen?,
    private val initialValue: String = "",
) : Screen(Component.translatable("exphardcore.screen.name_entry.title")) {
    private lateinit var nameEdit: EditBox
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    // for the typewriter thing
    private var animating = false
    private var animTick = 0
    private var animName = ""

    override fun init() {
        // positioned inside the book page text column
        nameEdit = EditBox(font, (width - 192) / 2 + 36, NAME_Y, 114, 12, LABEL)
        nameEdit.value = initialValue
        nameEdit.setMaxLength(32)
        nameEdit.setResponder { updateSaveButtonStatus() }
        applyEditBoxGfx(nameEdit)
        addRenderableWidget(nameEdit)

        // button pos values from BookSignScreen
        saveButton = addRenderableWidget(
            Button.builder(CommonComponents.GUI_DONE) { onSave() }
                .bounds(this.width / 2 - 100, 196, 98, 20)
                .build()
        )
        cancelButton = addRenderableWidget(
            Button.builder(CommonComponents.GUI_CANCEL) { onClose() }
                .bounds(this.width / 2 + 2, 196, 98, 20)
                .build()
        )
        updateSaveButtonStatus()
    }

    override fun setInitialFocus() {
        setInitialFocus(nameEdit)
    }

    override fun keyPressed(event: KeyEvent): Boolean {
        if (animating) return true
        if (this.nameEdit.isFocused && !this.nameEdit.value.isEmpty() && event.isConfirmation) {
            this.onSave()
            return true
        } else {
            return super.keyPressed(event)
        }
    }

    override fun tick() {
        super.tick()
        if (!animating) return
        animTick++
        val total = VANISH_TICKS + animName.length * TICKS_PER_CHAR + END_TICKS
        if (animTick >= total) commitSave()
    }

    override fun onClose() {
        minecraft.gui.setScreen(lastScreen)
    }

    private fun updateSaveButtonStatus() {
        saveButton.active = nameEdit.value.isNotBlank()
    }

    private fun onSave() {
        if (animating) return
        animName = nameEdit.value
        animating = true
        animTick = 0
        nameEdit.visible = false
        nameEdit.isFocused = false
        saveButton.active = false
        cancelButton.active = false
    }

    private fun commitSave() {
        minecraft.player?.connection?.send(ServerboundCustomPayloadPacket(createCharacterCreationPacket()))
        onClose()
    }

    private fun createCharacterCreationPacket(): ServerboundCharacterCreationPacket {
        val name = nameEdit.value
        return ServerboundCharacterCreationPacket(name)
    }

    //#region gfx

    override fun isInGameUi(): Boolean {
        return true
    }

    override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        // copied over from booksignscreen
        super.extractBackground(graphics, mouseX, mouseY, partialTick)
        graphics.blit(
            RenderPipelines.GUI_TEXTURED,
            BACKGROUND_LOCATION,
            (this.width - 192) / 2,
            2,
            0.0f,
            0.0f,
            192,
            192,
            256,
            256
        )
        // show player
        this.minecraft.player?.let {
            val width = 60
            val height = 90
            val entX0 = (this.width - width) / 2
            val entY0 = 80
            InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics,
                entX0,
                entY0,
                entX0 + width,
                entY0 + height,
                40,
                0.0625f,
                // look 3/4 left
                entX0.toFloat() + width / 2 - 20,
                entY0.toFloat() + height / 2 - 10,
                it
            )
        }
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick)
        val xo = (this.width - 192) / 2
        centeredPageText(graphics, title.copy().withStyle(ChatFormatting.BOLD), xo, TITLE_Y)
        centeredPageText(graphics, LABEL, xo, LABEL_Y)
        if (animating) {
            // EditBox hidden, draw name typewriter style
            val shown = ((animTick - VANISH_TICKS) / TICKS_PER_CHAR).coerceIn(0, animName.length)
            centeredPageText(graphics, Component.literal(animName.substring(0, shown)), xo, NAME_Y + 2)
        }
    }

    private fun centeredPageText(graphics: GuiGraphicsExtractor, text: Component, xo: Int, y: Int) {
        val w = this.font.width(text)
        graphics.text(this.font, text, xo + 36 + (114 - w) / 2, y, -16777216, false)
    }

    private fun applyEditBoxGfx(box: EditBox) {
        // taken from BookSignScreen
        box.setBordered(false)
        box.setCentered(true)
        box.setTextColor(-16777216)
        box.setTextShadow(false)
    }

    //#endregion

    companion object {
        private val LABEL = Component.translatable("exphardcore.screen.name_entry.label")
        private val BACKGROUND_LOCATION = id("textures/gui/logbook_name_background.png")

        // page top is y=2
        private const val TITLE_Y = 25
        private const val LABEL_Y = 45
        private const val NAME_Y = 60

        // typewriter save animation
        private const val VANISH_TICKS = 4
        private const val TICKS_PER_CHAR = 2
        private const val END_TICKS = 10

        // Call this from wherever the menu should be opened (e.g. a keybind or a respawn hook).
        fun open(lastScreen: Screen? = null, initialValue: String = "") {
            Minecraft.getInstance().gui.setScreen(CharacterCreationScreen(lastScreen, initialValue))
        }
    }
}
