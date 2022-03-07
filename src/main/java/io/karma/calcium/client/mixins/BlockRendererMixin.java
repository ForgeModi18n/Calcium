package io.karma.calcium.client.mixins;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.BiomeColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData.Builder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.AbstractBlock.OffsetType;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Random;

@Mixin(value = BlockRenderer.class, remap = false, priority = 999)
public abstract class BlockRendererMixin {
    // @formatter:off
    @Unique private static final int[] DEFAULT_VERTEX_COLORS = { 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF };
    @Shadow @Final private BiomeColorBlender biomeColorBlender;
    @Shadow @Final private BlockColorsExtended blockColors;
    @Shadow @Final private QuadLightData cachedQuadLightData;
    @Shadow @Final private BlockOcclusionCache occlusionCache;
    @Shadow @Final private Random random;
    @Shadow @Final private LightPipelineProvider lighters;
    // @formatter:on

    // TODO: figure out how to get rid of this overwrite..
    @SuppressWarnings("all")
    @Overwrite
    public boolean renderModel( // @formatter:off
        final @NotNull IBlockDisplayReader world,
        final @NotNull BlockState state,
        final @NotNull BlockPos pos,
        final @NotNull IBakedModel model,
        final @NotNull ChunkModelBuffers buffers,
        final boolean cull,
        final long seed,
        @NotNull IModelData modelData
    ) { // @formatter:on
        modelData = model.getModelData(world, pos, state, modelData);

        final OffsetType offsetType = state.getBlock().getOffsetType();
        final long posSeed = MathHelper.getSeed(pos.getX(), 0, pos.getZ());
        final float xOffset = offsetType != OffsetType.NONE ? (((float) (posSeed & 15L) / 15F) - 0.5F) * 0.5F : 0F;
        final float yOffset = offsetType == OffsetType.XYZ ? (((float) (posSeed >> 4L & 15L) / 15F) - 1F) * 0.2F : 0F;
        final float zOffset = offsetType != OffsetType.NONE ? (((float) (posSeed >> 8L & 15L) / 15F) - 0.5F) * 0.5F : 0F;

        final LightMode lightMode = getLightingMode(state, model);
        final LightPipeline lightPipeline = lighters.getLighter(lightMode);

        final Direction[] directions = DirectionUtil.ALL_DIRECTIONS;
        final int numDirections = directions.length;

        boolean isRendered = false;

        for (int i = 0; i < numDirections; i++) {
            final Direction direction = directions[i];
            random.setSeed(seed);
            final List<BakedQuad> quads = model.getQuads(state, direction, random, modelData);

            if (quads.isEmpty() || (cull && !occlusionCache.shouldDrawSide(state, world, pos, direction))) {
                continue;
            }

            final ModelQuadFacing quadFacing = ModelQuadFacing.fromDirection(direction);
            renderQuadListFast(world, state, pos, lightPipeline, xOffset, yOffset, zOffset, buffers, quads, quadFacing);
            isRendered = true;
        }

        random.setSeed(seed);
        final List<BakedQuad> quads = model.getQuads(state, null, random, modelData);

        if (!quads.isEmpty()) {
            renderQuadListFast(world, state, pos, lightPipeline, xOffset, yOffset, zOffset, buffers, quads, ModelQuadFacing.UNASSIGNED);
            isRendered = true;
        }

        return isRendered;
    }

    @SuppressWarnings("all")
    @Overwrite
    private void renderQuadList( // @formatter:off
        final @NotNull IBlockDisplayReader world,
        final @NotNull BlockState state,
        final @NotNull BlockPos pos,
        final @NotNull LightPipeline lightPipeline,
        final @NotNull Vector3d offset,
        final @NotNull ChunkModelBuffers buffers,
        final @NotNull List<BakedQuad> quads,
        final @Nullable ModelQuadFacing facing
    ) { // @formatter:on
        final float xo = (float) offset.x;
        final float yo = (float) offset.y;
        final float zo = (float) offset.z;
        renderQuadListFast(world, state, pos, lightPipeline, xo, yo, zo, buffers, quads, facing);
    }

    @SuppressWarnings("all")
    @Overwrite
    private void renderQuad( // @formatter:off
        final @NotNull IBlockDisplayReader world,
        final @NotNull BlockState state,
        final @NotNull BlockPos pos,
        final @NotNull ModelVertexSink sink,
        final @NotNull Vector3d offset,
        final @NotNull IBlockColor colorProvider,
        final @NotNull BakedQuad bakedQuad,
        final @NotNull QuadLightData lightData,
        final @NotNull Builder renderData
    ) { // @formatter:on
        final float xo = (float) offset.x;
        final float yo = (float) offset.y;
        final float zo = (float) offset.z;
        renderQuadFast(world, state, pos, sink, xo, yo, zo, colorProvider, bakedQuad, lightData, renderData);
    }

    // @formatter:off
    @Shadow protected abstract @NotNull LightMode getLightingMode(final @NotNull BlockState state, final @NotNull IBakedModel model);
    // @formatter:on

    @Unique
    @SuppressWarnings("deprecation")
    private void renderQuadListFast( // @formatter:off
        final @NotNull IBlockDisplayReader world,
        final @NotNull BlockState state,
        final @NotNull BlockPos pos,
        final @NotNull LightPipeline lightPipeline,
        final float xOffset,
        final float yOffset,
        final float zOffset,
        final @NotNull ChunkModelBuffers buffers,
        final @NotNull List<BakedQuad> quads,
        final @Nullable ModelQuadFacing facing
    ) { // @formatter:on
        final int numQuads = quads.size();
        final int numVertices = numQuads << 2;
        final ModelVertexSink sink = buffers.getSink(facing);
        final Builder renderData = buffers.getRenderData();

        sink.ensureCapacity(numVertices);

        for (final BakedQuad quad : quads) {
            final Direction direction = quad.getDirection();
            final boolean hasAmbientOcclusion = quad.isShade();

            final IBlockColor colorProvider = blockColors.getColorProvider(state);
            final QuadLightData lightData = cachedQuadLightData;

            lightPipeline.calculate((ModelQuadView) quad, pos, lightData, direction, hasAmbientOcclusion);
            renderQuadFast(world, state, pos, sink, xOffset, yOffset, zOffset, colorProvider, quad, lightData, renderData);
        }

        sink.flush();
    }

    @Unique
    private void renderQuadFast( // @formatter:off
        final @NotNull IBlockDisplayReader world,
        final @NotNull BlockState state,
        final @NotNull BlockPos pos,
        final @NotNull ModelVertexSink sink,
        final float xOffset,
        final float yOffset,
        final float zOffset,
        final @NotNull IBlockColor colorProvider,
        final @NotNull BakedQuad bakedQuad,
        final @NotNull QuadLightData lightData,
        final @NotNull Builder renderData
    ) { // @formatter:on
        final ModelQuadView quadView = (ModelQuadView) bakedQuad;
        final ModelQuadOrientation orientation = ModelQuadOrientation.orient(lightData.br);
        int[] vertexBaseColors = biomeColorBlender.getColors(colorProvider, world, state, pos, quadView);

        if (vertexBaseColors == null || !bakedQuad.isTinted()) {
            vertexBaseColors = DEFAULT_VERTEX_COLORS;
        }

        for (int i = 0; i < 4; i++) {
            final int srcIndex = orientation.getVertexIndex(i);
            final float x = quadView.getX(srcIndex) + xOffset;
            final float y = quadView.getY(srcIndex) + yOffset;
            final float z = quadView.getZ(srcIndex) + zOffset;

            int c = ColorABGR.mul(quadView.getColor(srcIndex), lightData.br[srcIndex]);

            final int bcm = vertexBaseColors[srcIndex];
            final float bcmR = (float) ColorABGR.unpackRed(bcm) / 255F;
            final float bcmG = (float) ColorABGR.unpackGreen(bcm) / 255F;
            final float bcmB = (float) ColorABGR.unpackBlue(bcm) / 255F;
            c = ColorABGR.mul(c, bcmR, bcmG, bcmB);

            final float u = quadView.getTexU(srcIndex);
            final float v = quadView.getTexV(srcIndex);

            final int ql = quadView.getLight(srcIndex);
            final int ll = lightData.lm[srcIndex];
            int l = ll;

            if (ql > 0) { // If there is quad-specific lighting data
                final int qsl = ql & 0xFFFF;
                final int qbl = (ql >> 16) & 0xFFFF;

                final int lsl = ll & 0xFFFF;
                final int lbl = (ll >> 16) & 0xFFFF;

                final int sl = Math.max(qsl, lsl);
                final int bl = Math.max(qbl, lbl);
                l = sl | (bl << 16);
            }

            sink.writeQuad(x, y, z, c, u, v, l);
        }

        final TextureAtlasSprite texture = quadView.getSprite();

        if (texture != null) {
            renderData.addSprite(texture);
        }
    }
}
