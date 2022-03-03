package io.karma.calcium.client.module.tic.mixins;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(targets = "slimeknights.tconstruct.library.client.model.block.FluidTextureModel$Baked", remap = false)
public final class BakedFluidTextureModelMixin {
    // @formatter:off
    @Shadow @Mutable @Final private Map<FluidStack, IBakedModel> cache = new ConcurrentHashMap<>();
    // @formatter:on
}
