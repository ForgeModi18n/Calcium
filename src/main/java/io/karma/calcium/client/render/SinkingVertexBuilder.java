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
import java.util.Arrays;

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
    private final int[] sideCount = new int[ModelQuadFacing.values().length];
    private int currentVertex;

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
        color = Colour.flipABGR(Colour.packRGBA(r, g, b, a)); // We need ABGR so we compose it on the fly
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
        light = (v << 16) | u; // Compose lightmap coords into raw light value 0xVVVV_UUUU
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
        final Direction dir = Direction.fromNormal((int) nx, (int) ny, (int) nz);
        final int normal = dir != null ? dir.ordinal() : -1;
        final int writePos = currentVertex << 5;

        // Write the current quad vertex's normal, position, UVs, color and raw light values
        buffer.putInt(writePos, normal);
        buffer.putFloat(writePos + 4, x);
        buffer.putFloat(writePos + 8, y);
        buffer.putFloat(writePos + 12, z);
        buffer.putFloat(writePos + 16, u);
        buffer.putFloat(writePos + 20, v);
        buffer.putInt(writePos + 24, color);
        buffer.putInt(writePos + 28, light);
        // We store 28 bytes per vertex

        resetCurrentVertex(); // Reset the current vertex values
        currentVertex++;
    }

    public void reset() {
        buffer.clear();
        buffer.rewind();
        currentVertex = 0;
        Arrays.fill(sideCount, 0);
        resetCurrentVertex();
    }

    public void flush(@Nonnull ChunkModelBuffers buffers) {
        final ModelQuadFacing[] facings = ModelQuadFacing.values();
        byte sideMask = 0;

        for (int vertexIdx = 0; vertexIdx < currentVertex; vertexIdx += 4) {
            int readPos = vertexIdx << 5;
            final int normal = buffer.getInt(readPos); // Fetch first normal for pre-selecting the vertex sink

            final Direction dir = normal != -1 ? Direction.values()[normal] : null;
            final ModelQuadFacing facing = dir != null ? ModelQuadFacing.fromDirection(dir) : ModelQuadFacing.UNASSIGNED;
            final ModelVertexSink sink = buffers.getSink(facing);

            sink.ensureCapacity(++sideCount[normal] << 2);

            writeQuadVertex(sink, readPos, 0);
            writeQuadVertex(sink, readPos, 1);
            writeQuadVertex(sink, readPos, 2);
            writeQuadVertex(sink, readPos, 3);

            sideMask |= 1 << facing.ordinal();
        }

        for (final ModelQuadFacing facing : facings) {
            if (((sideMask >> facing.ordinal()) & 1) == 0) {
                continue;
            }

            buffers.getSink(facing).flush();
        }
    }

    private void writeQuadVertex(@Nonnull ModelVertexSink sink, int readPos, int quadVertexIdx) {
        final int readIdx = readPos + (quadVertexIdx << 5);

        final float x = buffer.getFloat(readIdx + 4);
        final float y = buffer.getFloat(readIdx + 8);
        final float z = buffer.getFloat(readIdx + 12);
        final float u = buffer.getFloat(readIdx + 16);
        final float v = buffer.getFloat(readIdx + 20);
        final int color = buffer.getInt(readIdx + 24);
        final int light = buffer.getInt(readIdx + 28);

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
