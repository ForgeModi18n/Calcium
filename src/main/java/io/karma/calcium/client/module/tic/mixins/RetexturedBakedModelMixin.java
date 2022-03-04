package io.karma.calcium.client.module.tic.mixins;

import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import slimeknights.mantle.client.model.RetexturedModel.BakedModel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(value = BakedModel.class, remap = false)
public final class RetexturedBakedModelMixin {
    // @formatter:off
    @Shadow @Mutable @Final private Map<ResourceLocation, IBakedModel> cache = new ConcurrentHashMap<>();
    // @formatter:on
}
