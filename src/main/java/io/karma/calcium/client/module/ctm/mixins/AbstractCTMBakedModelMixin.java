package io.karma.calcium.client.module.ctm.mixins;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.karma.calcium.client.util.TextureUtils;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.chisel.ctm.client.model.AbstractCTMBakedModel;
import team.chisel.ctm.client.state.CTMContext;

import java.util.concurrent.TimeUnit;

@Mixin(value = AbstractCTMBakedModel.class, remap = false)
public abstract class AbstractCTMBakedModelMixin {
    @Shadow
    @Final
    protected static ModelProperty<CTMContext> CTM_CONTEXT;
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

    @ModifyVariable(method = "getOverrideSprite", at = @At(value = "STORE"))
    private @NotNull TextureAtlasSprite onGetOverrideSprite(final @Nullable TextureAtlasSprite texture) {
        return texture != null ? texture : TextureUtils.getMissingTexture();
    }

    @Inject(method = "getModelData", at = @At("HEAD"), cancellable = true)
    private void onGetModelData(final @NotNull IBlockDisplayReader world, final @NotNull BlockPos pos, final @NotNull BlockState state, final @Nullable IModelData tileData, final @NotNull CallbackInfoReturnable<IModelData> cbi) {
        if (tileData == null) {
            cbi.setReturnValue(new ModelDataMap.Builder().withInitial(CTM_CONTEXT, new CTMContext(world, pos)).build());
            cbi.cancel();
        }
    }
}
