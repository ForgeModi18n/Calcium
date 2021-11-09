package io.karma.calcium.client.mixins;

import com.mojang.blaze3d.matrix.MatrixStack;
import io.karma.calcium.CalciumMod;
import io.karma.calcium.client.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nonnull;
import java.util.Random;

@Mixin(value = BlockRenderer.class, remap = false)
public class BlockRendererMixin {
    private static final ThreadLocal<MatrixStack> matrixStack = ThreadLocal.withInitial(MatrixStack::new);

    //@formatter:off
    @Shadow @Final private Random random;
    //@formatter:on

    @Inject(method = "renderModel", at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/client/renderer/model/IBakedModel;getModelData(Lnet/minecraft/world/IBlockDisplayReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraftforge/client/model/data/IModelData;)Lnet/minecraftforge/client/model/data/IModelData;"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void onRenderModel(@Nonnull IBlockDisplayReader world, @Nonnull BlockState state, @Nonnull BlockPos pos, @Nonnull IBakedModel model, @Nonnull ChunkModelBuffers buffers, boolean cull, long seed, @Nonnull CallbackInfoReturnable<Boolean> cbi, @Nonnull LightPipeline lighter, @Nonnull Vector3d offset, @Nonnull IModelData modelData) {
        for (final var renderer : CalciumMod.getCustomRenderers()) {
            if (renderer.canHandleBlock(world, pos, state)) {
                final var mStack = matrixStack.get();
                final var builder = SinkingVertexBuilder.getInstance();

                mStack.clear();

                builder.reset();
                cbi.setReturnValue(renderer.renderBlock(state, pos, world, mStack, builder, random, modelData));
                builder.flush(buffers);

                cbi.cancel();
            }
        }
    }
}
