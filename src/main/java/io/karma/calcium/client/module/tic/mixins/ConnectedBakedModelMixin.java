package io.karma.calcium.client.module.tic.mixins;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(targets = "slimeknights.mantle.client.model.connected.ConnectedModel$BakedModel", remap = false)
public final class ConnectedBakedModelMixin {
    // @formatter:off
    @Shadow @Mutable @Final private Map<String, String> nameMappingCache = new ConcurrentHashMap<>();
    // @formatter:on
}
