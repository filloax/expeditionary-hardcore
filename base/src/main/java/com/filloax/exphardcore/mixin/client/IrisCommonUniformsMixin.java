package com.filloax.exphardcore.mixin.client;

import com.filloax.exphardcore.client.compat.IrisCompat;
import net.irisshaders.iris.gl.uniform.UniformUpdateFrequency;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.IntSupplier;

// pseudo + require 0 to avoid crashing if Iris changes internally, hopefully
@Pseudo
@Mixin(targets = "net.irisshaders.iris.uniforms.CommonUniforms", remap = false)
public class IrisCommonUniformsMixin {
    // dh compatible packs use this for fog (could have wonkiness but it is a specific enough feature anyways)
    @ModifyArg(
        method = "generalCommonUniforms",
        at = @At(
            value = "INVOKE",
            target = "Lnet/irisshaders/iris/gl/uniform/UniformHolder;uniform1i(Lnet/irisshaders/iris/gl/uniform/UniformUpdateFrequency;Ljava/lang/String;Ljava/util/function/IntSupplier;)Lnet/irisshaders/iris/gl/uniform/UniformHolder;"
        ),
        index = 2,
        require = 0,
        remap = false
    )
    private static IntSupplier expeditionaryhardcore$overrideDhRenderDistance(UniformUpdateFrequency frequency, String name, IntSupplier supplier) {
        if (!"dhRenderDistance".equals(name)) return supplier;
        return () -> IrisCompat.overrideDhRenderDistance(supplier.getAsInt());
    }
}
