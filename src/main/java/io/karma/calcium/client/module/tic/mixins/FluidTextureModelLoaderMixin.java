package io.karma.calcium.client.module.tic.mixins;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.fluid.Fluid;
import net.minecraftforge.client.model.BakedModelWrapper;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(targets = "slimeknights.mantle.client.model.fluid.FluidTextureModel$Loader", remap = false)
public final class FluidTextureModelLoaderMixin {
    // @formatter:off
    @Shadow @Mutable @Final private Map<Fluid, BakedModelWrapper<IBakedModel>> modelCache;
    // @formatter:on

    @Inject(method = "<init>()V", at = @At("TAIL"))
    private void onPostInit(final @NotNull CallbackInfo cbi) {
        modelCache = new ConcurrentHashMap<>();
    }
}
