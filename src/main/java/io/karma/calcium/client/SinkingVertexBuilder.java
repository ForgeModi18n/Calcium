package io.karma.calcium.client;

import codechicken.lib.colour.Colour;
import codechicken.lib.util.Copyable;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import net.minecraft.util.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.ArrayList;

@OnlyIn(Dist.CLIENT)
public final class SinkingVertexBuilder implements IVertexBuilder {
    private static final ThreadLocal<SinkingVertexBuilder> instance = ThreadLocal.withInitial(SinkingVertexBuilder::new);

    private final ArrayList<Vertex> vertices = new ArrayList<>();
    private final Vertex currentVertex = new Vertex();

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
        currentVertex.x = (float) x;
        currentVertex.y = (float) y;
        currentVertex.z = (float) z;
        return this;
    }

    @Nonnull
    @Override
    public IVertexBuilder color(int r, int g, int b, int a) {
        currentVertex.color = Colour.packRGBA(r, g, b, a);
        return this;
    }

    @Nonnull
    @Override
    public IVertexBuilder uv(float u, float v) {
        currentVertex.u = u;
        currentVertex.v = v;
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
        currentVertex.lU = u;
        currentVertex.lV = v;
        return this;
    }

    @Nonnull
    @Override
    public IVertexBuilder normal(float x, float y, float z) {
        currentVertex.nx = x;
        currentVertex.ny = y;
        currentVertex.nz = z;
        return this;
    }

    @Override
    public void endVertex() {
        vertices.add(currentVertex.copy());
        currentVertex.reset();
    }

    public void reset() {
        vertices.clear();
        currentVertex.reset();
    }

    public void flush(@Nonnull ChunkModelBuffers buffers) {
        final var numVertices = vertices.size();

        if (numVertices % 4 != 0) {
            throw new IllegalStateException("Invalid number of vertices");
        }

        byte changedSides = 0B00000000;

        for (var i = 0; i < numVertices; i += 4) {
            final var firstVertex = vertices.get(i);
            final var dir = Direction.fromNormal((int) firstVertex.nx, (int) firstVertex.ny, (int) firstVertex.nz);
            final var facing = dir == null ? ModelQuadFacing.UNASSIGNED : ModelQuadFacing.fromDirection(dir);
            final var sink = buffers.getSink(facing);

            firstVertex.writeToSink(sink);
            vertices.get(i + 1).writeToSink(sink);
            vertices.get(i + 2).writeToSink(sink);
            vertices.get(i + 3).writeToSink(sink);

            changedSides |= 0B00000001 << facing.ordinal();
        }

        for (final var facing : ModelQuadFacing.values()) {
            if (((changedSides >> facing.ordinal()) & 0B00000001) == 0) {
                continue;
            }

            buffers.getSink(facing).flush();
        }
    }

    private static class Vertex implements Copyable<Vertex> {
        public float x;
        public float y;
        public float z;
        public float nx;
        public float ny;
        public float nz;
        public float u;
        public float v;
        public int lU;
        public int lV;
        public int color;

        public Vertex() {
        }

        public Vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v, int lU, int lV, int color) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.u = u;
            this.v = v;
            this.lU = lU;
            this.lV = lV;
            this.color = color;
        }

        public void reset() {
            x = y = z = 0F;
            nx = ny = nz = 0F;
            u = v = 0F;
            lU = lV = 0;
            color = 0;
        }

        public void writeToSink(@Nonnull ModelVertexSink sink) {
            // Stupid name, this writes a vertex of a quad, not a quad..
            // This also uses ARGB instead of RGBA, so flip that around!
            sink.writeQuad(x, y, z, Colour.flipABGR(color), u, v, lV << 16 | lU);
        }

        @Nonnull
        @Override
        public Vertex copy() {
            return new Vertex(x, y, z, nx, ny, nz, u, v, lU, lV, color);
        }
    }
}
