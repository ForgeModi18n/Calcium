package io.karma.calcium.client.module.ccl;

import codechicken.lib.render.block.BlockRenderingRegistry;
import codechicken.lib.render.block.ICCBlockRenderer;
import io.karma.calcium.CalciumMod;
import io.karma.calcium.client.module.AbstractCAModule;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.IRegistryDelegate;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public final class CCLModule extends AbstractCAModule {
    private Map<IRegistryDelegate<Block>, ICCBlockRenderer> customBlockRenderers;
    private Map<IRegistryDelegate<Fluid>, ICCBlockRenderer> customFluidRenderers;
    private List<ICCBlockRenderer> customGlobalRenderers;

    public CCLModule() {
        super("ccl", "CodeChickenLib");
    }

    public @NotNull List<ICCBlockRenderer> getCustomRenderers(final @Nonnull IBlockDisplayReader world, final @Nonnull BlockPos pos) {
        final BlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();

        final FluidState fluidState = state.getFluidState();
        final Fluid fluid = fluidState.getType();

        final ArrayList<ICCBlockRenderer> renderers = new ArrayList<>(customGlobalRenderers);

        for (final Map.Entry<IRegistryDelegate<Block>, ICCBlockRenderer> entry : customBlockRenderers.entrySet()) {
            final Block entryBlock = entry.getKey().get();

            if (entryBlock.is(block)) {
                renderers.add(entry.getValue());
            }
        }

        for (final Map.Entry<IRegistryDelegate<Fluid>, ICCBlockRenderer> entry : customFluidRenderers.entrySet()) {
            final Fluid entryFluid = entry.getKey().get();

            if (entryFluid.isSame(fluid)) {
                renderers.add(entry.getValue());
            }
        }

        return renderers;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void init(final @NotNull FMLClientSetupEvent event) {
        try {
            CalciumMod.LOGGER.info("Retrieving block renderers");
            final Field blockRenderersField = BlockRenderingRegistry.class.getDeclaredField("blockRenderers");
            blockRenderersField.setAccessible(true);
            customBlockRenderers = (Map<IRegistryDelegate<Block>, ICCBlockRenderer>) blockRenderersField.get(null);

            CalciumMod.LOGGER.info("Retrieving fluid renderers");
            final Field fluidRenderersField = BlockRenderingRegistry.class.getDeclaredField("fluidRenderers");
            fluidRenderersField.setAccessible(true);
            customFluidRenderers = (Map<IRegistryDelegate<Fluid>, ICCBlockRenderer>) fluidRenderersField.get(null);

            CalciumMod.LOGGER.info("Retrieving global renderers");
            final Field globalRenderersField = BlockRenderingRegistry.class.getDeclaredField("globalRenderers");
            globalRenderersField.setAccessible(true);
            customGlobalRenderers = (List<ICCBlockRenderer>) globalRenderersField.get(null);
        }
        catch (final @NotNull Throwable t) {
            CalciumMod.LOGGER.error("Could not retrieve custom renderers");
        }
    }
}
