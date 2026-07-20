package com.filloax.exphardcore.mixin.client;

import com.filloax.exphardcore.ExpeditionaryHardcore;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    private static final float CREDITS_SCALE = 0.5F;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void expeditionaryhardcore$drawCydoniaCredits(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!ExpeditionaryHardcore.cydoniaMode) return;

        Minecraft minecraft = Minecraft.getInstance();
        int color = ARGB.white(1.0F);
        int lineHeight = Math.round(9 * CREDITS_SCALE);

        expeditionaryhardcore$drawScaledText(graphics, minecraft, "Expeditionary Hardcore v" + modVersion("exphardcore") + "C by Filloax, Farcr, Reivaxelain", 2, 2, color);
        expeditionaryhardcore$drawScaledText(graphics, minecraft, "APIBalego v" + modVersion("apibalego") + " by Filloax and Krozzzt", 2, 2 + lineHeight, color);
    }

    private static void expeditionaryhardcore$drawScaledText(GuiGraphicsExtractor graphics, Minecraft minecraft, String text, int x, int y, int color) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(CREDITS_SCALE, CREDITS_SCALE);
        graphics.text(minecraft.font, text, 0, 0, color);
        graphics.pose().popMatrix();
    }

    private static String modVersion(String modId) {
        return FabricLoader.getInstance()
                .getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString().split("-")[0])
                .orElse("?");
    }
}
