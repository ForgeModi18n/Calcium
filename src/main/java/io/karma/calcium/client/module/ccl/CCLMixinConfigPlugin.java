package io.karma.calcium.client.module.ccl;

import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class CCLMixinConfigPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(final @NotNull String mixinPackage) {

    }

    @Override
    public @Nullable String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(final @NotNull String targetClassName, final @NotNull String mixinClassName) {
        if (!mixinClassName.startsWith("io.karma.calcium.client.module.ccl.mixins")) {
            return true;
        }

        try { // @formatter:off
            return FMLLoader.getLoadingModList()
                .getModFiles()
                .stream()
                .anyMatch(f -> f.getMods()
                    .stream()
                    .anyMatch(i -> i.getModId().equals("codechickenlib")));
        } // @formatter:on
        catch (final @NotNull Throwable t) {
            return false;
        }
    }

    @Override
    public void acceptTargets(final @NotNull Set<String> myTargets, final @NotNull Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(final @NotNull String targetClassName, final @NotNull ClassNode targetClass, final @NotNull String mixinClassName, final @NotNull IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(final @NotNull String targetClassName, final @NotNull ClassNode targetClass, final @NotNull String mixinClassName, final @NotNull IMixinInfo mixinInfo) {

    }
}
