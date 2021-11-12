package io.karma.calcium.client;

import codechicken.lib.colour.Colour;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import net.minecraft.util.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * A mostly allocation-free {@link IVertexBuilder} implementation
 * which pipes vertices into a {@link ModelVertexSink}.
 *
 * @author KitsuneAlex
 */
@OnlyIn(Dist.CLIENT)
public final class SinkingVertexBuilder implements IVertexBuilder {
    private static final ThreadLocal<SinkingVertexBuilder> instance = ThreadLocal.withInitial(SinkingVertexBuilder::new);
    private static final int bufferSize = 2097152;

    private final FloatBuffer buffer = ByteBuffer.allocateDirect(bufferSize << 2).asFloatBuffer();
    private final ByteBuffer normalBuffer = ByteBuffer.allocateDirect(bufferSize);
    private float x;
    private float y;
    private float z;
    private float u;
    private float v;
    private float r;
    private float g;
    private float b;
    private float a;
    private float bl;
    private float sl;
    private int currentVertex;
    private int currentQuadVertex;

    //@formatter:off
    private SinkingVertexBuilder() {}
    //@formatter:on

    @Nonnull
    public static SinkingVertexBuilder getInstance() {
        return instance.get();
    }

    @Nonnull
    @Override
    public IVertexBuilder vertex(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        return this;
    }

    @Nonnull
    @Override
    public IVertexBuilder color(int r, int g, int b, int a) {
        this.r = MathHelper.clamp(r / 255F, 0F, 1F);
        this.g = MathHelper.clamp(g / 255F, 0F, 1F);
        this.b = MathHelper.clamp(b / 255F, 0F, 1F);
        this.a = MathHelper.clamp(a / 255F, 0F, 1F);
        return this;
    }

    @Nonnull
    @Override
    public IVertexBuilder uv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Nonnull
    @Override
    public IVertexBuilder overlayCoords(int u, int v) {
        return this;
    }

    @Nonnull
    @Override
    public IVertexBuilder uv2(int u, int v) {
        bl = (float) u;
        sl = (float) v;
        return this;
    }

    @Nonnull
    @Override
    public IVertexBuilder normal(float x, float y, float z) {
        if (currentQuadVertex == 3) {
            final Direction dir = Direction.fromNormal((int) x, (int) y, (int) z);
            final ModelQuadFacing facing = dir != null ? ModelQuadFacing.fromDirection(dir) : ModelQuadFacing.UNASSIGNED;
            normalBuffer.put((byte) facing.ordinal());
        }

        return this;
    }

    @Override
    public void endVertex() {
        buffer.put(x);
        buffer.put(y);
        buffer.put(z);
        buffer.put(u);
        buffer.put(v);
        buffer.put(r);
        buffer.put(g);
        buffer.put(b);
        buffer.put(a);
        buffer.put(bl);
        buffer.put(sl);
        resetCurrentVertex();

        currentVertex++;

        if (currentQuadVertex < 3) {
            currentQuadVertex++;
        }
        else {
            currentQuadVertex = 0;
        }
    }

    public void reset() {
        buffer.clear();
        normalBuffer.clear();
        currentVertex = 0;
        currentQuadVertex = 0;
        resetCurrentVertex();
    }

    public void flush(@Nonnull ChunkModelBuffers buffers) {
        buffer.rewind();
        normalBuffer.rewind();

        final ModelQuadFacing[] facings = ModelQuadFacing.values();
        final int numQuads = currentVertex >> 2;
        final int[] sidedIndex = new int[facings.length];
        byte sidesChanged = 0;

        for (int i = 0; i < numQuads; i++) {
            final byte facingIdx = normalBuffer.get();
            final ModelQuadFacing facing = facings[facingIdx];

            final ModelVertexSink sink = buffers.getSink(facing);
            sink.ensureCapacity((++sidedIndex[facingIdx] << 2) + 4);

            writeQuadVertex(sink);
            writeQuadVertex(sink);
            writeQuadVertex(sink);
            writeQuadVertex(sink);

            sidesChanged |= (1 << facingIdx);
        }

        for (final ModelQuadFacing facing : facings) {
            if (((sidesChanged >> facing.ordinal()) & 1) == 0) {
                continue;
            }

            buffers.getSink(facing).flush();
        }
    }

    private void writeQuadVertex(@Nonnull ModelVertexSink sink) {
        final float x = buffer.get();
        final float y = buffer.get();
        final float z = buffer.get();
        final float u = buffer.get();
        final float v = buffer.get();
        final float r = buffer.get();
        final float g = buffer.get();
        final float b = buffer.get();
        final float a = buffer.get();
        final float bl = buffer.get();
        final float sl = buffer.get();

        final int color = Colour.flipABGR(Colour.packRGBA(r, g, b, a));
        final int light = ((int) sl << 16) | (int) bl;

        sink.writeQuad(x, y, z, color, u, v, light);
    }

    private void resetCurrentVertex() {
        x = y = z = 0F;
        u = v = 0F;
        r = g = b = a = 1F;
        bl = sl = 0F;
    }
}
