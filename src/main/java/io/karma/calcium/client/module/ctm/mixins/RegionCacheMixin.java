package io.karma.calcium.client.module.ctm.mixins;

import io.karma.calcium.client.util.CLong2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import team.chisel.ctm.client.util.RegionCache;

import java.lang.ref.WeakReference;

@Mixin(value = RegionCache.class, remap = false)
public abstract class RegionCacheMixin {
    // @formatter:off
    @Shadow private WeakReference<IBlockReader> passthrough;
    @Unique private final CLong2ObjectOpenHashMap<BlockState> newStateCache = new CLong2ObjectOpenHashMap<>();
    // @formatter:on

    @Overwrite
    public RegionCache updateWorld(final @NotNull IBlockReader passthrough) {
        if (this.passthrough.get() != passthrough) {
            newStateCache.clear();
        }

        this.passthrough = new WeakReference<>(passthrough);
        return RegionCache.class.cast(this);
    }

    @Overwrite
    public @NotNull BlockState getBlockState(final @NotNull BlockPos pos) {
        final long key = pos.asLong();
        BlockState ret = newStateCache.get(key);

        if (ret == null) {
            newStateCache.put(key, ret = this.getPassthrough().getBlockState(pos));
        }

        return ret;
    }

    // @formatter:off
    @Shadow protected abstract IBlockReader getPassthrough();
    // @formatter:on
}
