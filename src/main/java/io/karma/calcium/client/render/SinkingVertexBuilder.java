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

        // Write the current quad vertex's normal, position, UVs, color and raw light values
        buffer.putInt(normal);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putFloat(u);
        buffer.putFloat(v);
        buffer.putInt(color);
        buffer.putInt(light);
        // We store 32 bytes per vertex

        resetCurrentVertex(); // Reset the current vertex values
        currentVertex++;
    }

    public void reset() {
        buffer.rewind();
        currentVertex = 0;
        Arrays.fill(sideCount, 0);
        resetCurrentVertex();
    }

    public void flush(@Nonnull ChunkModelBuffers buffers) {
        final ModelQuadFacing[] facings = ModelQuadFacing.values();
        final int numQuads = currentVertex >> 2;

        for (int quadIdx = 0; quadIdx < numQuads; quadIdx++) {
            final int normal = buffer.getInt((quadIdx << 2) << 5);
            final Direction dir = normal != -1 ? Direction.values()[normal] : null;
            final ModelQuadFacing facing = dir != null ? ModelQuadFacing.fromDirection(dir) : ModelQuadFacing.UNASSIGNED;
            sideCount[facing.ordinal()]++;
        }

        for (final ModelQuadFacing facing : facings) {
            final int count = sideCount[facing.ordinal()];
            if (count == 0) {
                continue;
            }
            buffers.getSink(facing).ensureCapacity(count << 2);
        }

        final int byteSize = currentVertex << 5;
        byte sideMask = 0;

        buffer.rewind();

        while (buffer.position() < byteSize) {
            final int normal = buffer.getInt(); // Fetch first normal for pre-selecting the vertex sink
            final Direction dir = normal != -1 ? Direction.values()[normal] : null;
            final ModelQuadFacing facing = dir != null ? ModelQuadFacing.fromDirection(dir) : ModelQuadFacing.UNASSIGNED;
            final int facingIdx = facing.ordinal();

            final ModelVertexSink sink = buffers.getSink(facing);

            writeQuadVertex(sink);
            buffer.getInt();
            writeQuadVertex(sink);
            buffer.getInt();
            writeQuadVertex(sink);
            buffer.getInt();
            writeQuadVertex(sink);

            sideMask |= 1 << facingIdx;
        }

        for (final ModelQuadFacing facing : facings) {
            if (((sideMask >> facing.ordinal()) & 1) == 0) {
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
