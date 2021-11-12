package io.karma.calcium.client.render;

import codechicken.lib.colour.Colour;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import net.minecraft.util.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A allocation-free {@link IVertexBuilder} implementation
 * which pipes vertices into a {@link ModelVertexSink}.
 *
 * @author KitsuneAlex
 */
@OnlyIn(Dist.CLIENT)
public final class SinkingVertexBuilder implements IVertexBuilder {
    private static final ThreadLocal<SinkingVertexBuilder> instance = ThreadLocal.withInitial(SinkingVertexBuilder::new);

    private final ByteBuffer quadBuffer = ByteBuffer.allocateDirect(160).order(ByteOrder.nativeOrder());

    private float x;
    private float y;
    private float z;
    private float nx;
    private float ny;
    private float nz;
    private float u;
    private float v;
    private int color;
    private int light;

    private int currentQuad;
    private int currentQuadVertexIndex;
    private byte sideMask;
    private ChunkModelBuffers buffers;

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
        color = Colour.flipABGR(Colour.packRGBA(r, g, b, a));
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
        light = (v << 16) | u;
        return this;
    }

    @Nonnull
    @Override
    public IVertexBuilder normal(float x, float y, float z) {
        nx = x;
        ny = y;
        nz = z;
        return this;
    }

    @Override
    public void endVertex() {
        quadBuffer.putFloat(x);
        quadBuffer.putFloat(y);
        quadBuffer.putFloat(z);
        quadBuffer.putFloat(u);
        quadBuffer.putFloat(v);
        quadBuffer.putInt(color);
        quadBuffer.putInt(light);

        if (currentQuadVertexIndex == 3) {
            quadBuffer.rewind();
            sideMask |= writeQuad();
            quadBuffer.clear();
            quadBuffer.rewind();

            currentQuad++;
            currentQuadVertexIndex = 0;
        }
        else {
            currentQuadVertexIndex++;
        }

        resetCurrentVertex();
    }

    public void reset() {
        currentQuad = 0;
        currentQuadVertexIndex = 0;
        sideMask = 0;
        buffers = null;
        resetCurrentVertex();
    }

    public void setBuffers(@Nonnull ChunkModelBuffers buffers) {
        this.buffers = buffers;
    }

    public void flush() {
        for (final ModelQuadFacing facing : ModelQuadFacing.values()) {
            if ((sideMask >> facing.ordinal() & 1) == 0) {
                continue;
            }

            buffers.getSink(facing).flush();
        }
    }

    private byte writeQuad() {
        final Direction dir = Direction.fromNormal((int) nx, (int) ny, (int) nz);
        final ModelQuadFacing facing = dir != null ? ModelQuadFacing.fromDirection(dir) : ModelQuadFacing.UNASSIGNED;
        final ModelVertexSink sink = buffers.getSink(facing);

        sink.ensureCapacity((currentQuad + 1) << 2);
        writeQuadVertex(sink);
        writeQuadVertex(sink);
        writeQuadVertex(sink);
        writeQuadVertex(sink);

        return (byte) (1 << facing.ordinal());
    }

    private void writeQuadVertex(@Nonnull ModelVertexSink sink) {
        final float x = quadBuffer.getFloat();
        final float y = quadBuffer.getFloat();
        final float z = quadBuffer.getFloat();
        final float u = quadBuffer.getFloat();
        final float v = quadBuffer.getFloat();
        final int color = quadBuffer.getInt();
        final int light = quadBuffer.getInt();
        sink.writeQuad(x, y, z, color, u, v, light);
    }

    private void resetCurrentVertex() {
        x = y = z = 0F;
        nx = ny = nz = 0F;
        u = v = 0F;
        color = 0xFFFF_FFFF;
        light = 0;
    }
}
