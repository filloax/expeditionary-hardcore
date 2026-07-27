package com.filloax.exphardcore.mixin.client;

import net.minecraft.client.gui.layouts.LayoutElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * AbstractLayout.AbstractChildWrapper is protected, so it can't be named directly.
 */
@Mixin(targets = "net.minecraft.client.gui.layouts.AbstractLayout$AbstractChildWrapper")
public interface LayoutChildWrapperAccessor {
    @Accessor("child")
    LayoutElement getChild();

    @Mutable
    @Accessor("child")
    void setChild(LayoutElement child);
}
