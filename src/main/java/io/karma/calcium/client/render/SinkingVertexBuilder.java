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

    private final ByteBuffer buffer = ByteBuffer.allocateDirect(2097152).order(ByteOrder.nativeOrder());
    private int currentVertex;
    private int currentQuadVertex;

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
        if (currentQuadVertex == 0) {
            final Direction dir = Direction.fromNormal((int) nx, (int) ny, (int) nz);
            final ModelQuadFacing facing = dir != null ? ModelQuadFacing.fromDirection(dir) : ModelQuadFacing.UNASSIGNED;
            buffer.putInt(facing.ordinal());
        }

        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putFloat(u);
        buffer.putFloat(v);
        buffer.putInt(color);
        buffer.putInt(light);

        resetCurrentVertex();

        if (currentQuadVertex < 3) {
            currentQuadVertex++;
        }
        else {
            currentQuadVertex = 0;
        }

        currentVertex++;
    }

    public void reset() {
        buffer.rewind();

        resetCurrentVertex();
        currentVertex = 0;
        currentQuadVertex = 0;
    }

    public void flush(@Nonnull ChunkModelBuffers buffers) {
        buffer.rewind();

        final ModelQuadFacing[] facings = ModelQuadFacing.values();
        final int[] sidedCount = new int[facings.length];
        byte normalMask = 0;

        for (int i = 0; i < currentVertex; i += 4) {
            final int normal = buffer.getInt();

            final ModelQuadFacing facing = facings[normal];
            final ModelVertexSink sink = buffers.getSink(facing);

            sink.ensureCapacity(++sidedCount[normal] << 2);
            writeQuadVertex(sink);
            writeQuadVertex(sink);
            writeQuadVertex(sink);
            writeQuadVertex(sink);

            normalMask |= 1 << normal;
        }

        for (final ModelQuadFacing facing : facings) {
            if (((normalMask >> facing.ordinal()) & 1) == 0) {
                continue;
            }

            buffers.getSink(facing).flush();
        }
    }

    private void writeQuadVertex(@Nonnull ModelVertexSink sink) {
        final float x = buffer.getFloat();
        final float y = buffer.getFloat();
        final float z = buffer.getFloat();
        final float u = buffer.getFloat();
        final float v = buffer.getFloat();
        final int color = buffer.getInt();
        final int light = buffer.getInt();

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
