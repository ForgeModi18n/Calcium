package io.karma.calcium.client.module.ctm.mixins;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;

import java.util.concurrent.TimeUnit;

@Mixin(value = AbstractCTMBakedModel.class, remap = false)
public abstract class AbstractCTMBakedModelMixin {
    // @formatter:off
    @Shadow private static Cache<ModelResourceLocation, AbstractCTMBakedModel> itemcache;
    @Shadow private static Cache<Object, AbstractCTMBakedModel> modelcache;
    // @formatter:on

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void onCLInit(final @NotNull CallbackInfo cbi) {
        final int numThreads = Runtime.getRuntime().availableProcessors();

        // @formatter:off
        itemcache = CacheBuilder.newBuilder()
            .expireAfterAccess(10L, TimeUnit.SECONDS)
            .concurrencyLevel(numThreads)
            .build();

        modelcache = CacheBuilder.newBuilder()
            .expireAfterAccess(1L, TimeUnit.MINUTES)
            .maximumSize(5000L)
            .concurrencyLevel(numThreads)
            .build();
        // @formatter:on
    }
}
