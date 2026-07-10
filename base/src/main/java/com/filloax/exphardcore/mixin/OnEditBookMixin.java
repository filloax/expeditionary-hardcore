package com.filloax.exphardcore.mixin;

import com.filloax.exphardcore.cydonia.ApibalegoInfoSender;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// Only really required in cydonia mode
@Mixin(ServerGamePacketListenerImpl.class)
public class OnEditBookMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "updateBookContents", at = @At("RETURN"))
    void expeditionaryhardcore$onUpdateBookContents(
            List<FilteredText> contents, int slot, CallbackInfo ci,
            @Local(name = "carried") ItemStack stack
    ) {
        ApibalegoInfoSender.onEditBook(stack, player, contents.stream().map(FilteredText::raw).toList());
    }

    @Inject(method = "signBook", at = @At("RETURN"))
    void expeditionaryhardcore$onSignbook(
            FilteredText title, List<FilteredText> contents, int slot, CallbackInfo ci,
            @Local(name = "carried") ItemStack stack
    ) {
        ApibalegoInfoSender.onSignBook(stack, player, contents.stream().map(FilteredText::raw).toList(), title.raw());
    }
}
