package io.karma.calcium.client.module.ccl.mixins;

import codechicken.lib.render.block.ICCBlockRenderer;
import com.mojang.blaze3d.matrix.MatrixStack;
import io.karma.calcium.client.module.CAModuleManager;
import io.karma.calcium.client.module.ccl.CCLModule;
import io.karma.calcium.client.module.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.Random;

@Mixin(value = BlockRenderer.class, remap = false)
public final class BlockRendererMixin {
    //@formatter:off
    @Unique private final ThreadLocal<MatrixStack> matrixStack = ThreadLocal.withInitial(MatrixStack::new);
    @Unique private final CCLModule cclModule = CAModuleManager.<CCLModule>getModule("ccl").orElseThrow(() -> new RuntimeException("Could not retrieve module"));
    @Shadow @Final private Random random;
    //@formatter:on

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true)
    private void onRenderModel(IBlockDisplayReader world, BlockState state, BlockPos pos, IBakedModel model, ChunkModelBuffers buffers, boolean cull, long seed, IModelData modelData, @Nonnull CallbackInfoReturnable<Boolean> cbi) {
        modelData = model.getModelData(world, pos, state, modelData); // Add support for this forge hook

        final MatrixStack mStack = matrixStack.get();
        final SinkingVertexBuilder builder = SinkingVertexBuilder.getInstance();

        for (final ICCBlockRenderer renderer : cclModule.getCustomRenderers(world, pos)) {
            if (renderer.canHandleBlock(world, pos, state)) {
                mStack.clear();

                builder.reset();
                cbi.setReturnValue(renderer.renderBlock(state, pos, world, mStack, builder, random, modelData));
                builder.flush(buffers);

                cbi.cancel();
            }
        }
    }
}
