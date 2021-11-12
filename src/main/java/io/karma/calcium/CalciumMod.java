package io.karma.calcium;

import codechicken.lib.render.block.BlockRenderingRegistry;
import codechicken.lib.render.block.ICCBlockRenderer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockDisplayReader;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IRegistryDelegate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mod(CalciumMod.MODID)
public final class CalciumMod {
    public static final String MODID = "calcium";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @OnlyIn(Dist.CLIENT)
    private static Map<IRegistryDelegate<Block>, ICCBlockRenderer> customBlockRenderers;
    @OnlyIn(Dist.CLIENT)
    private static Map<IRegistryDelegate<Fluid>, ICCBlockRenderer> customFluidRenderers;
    @OnlyIn(Dist.CLIENT)
    private static List<ICCBlockRenderer> customGlobalRenderers;

    public CalciumMod() {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(this::onClientSetup));
    }

    @Nonnull
    public static List<ICCBlockRenderer> getCustomRenderers(@Nonnull IBlockDisplayReader world, @Nonnull BlockPos pos) {
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
    @OnlyIn(Dist.CLIENT)
    private void onClientSetup(@Nonnull FMLClientSetupEvent e) {
        try {
            final Field blockRenderersField = BlockRenderingRegistry.class.getDeclaredField("blockRenderers");
            blockRenderersField.setAccessible(true);
            customBlockRenderers = (Map<IRegistryDelegate<Block>, ICCBlockRenderer>) blockRenderersField.get(null);

            final Field fluidRenderersField = BlockRenderingRegistry.class.getDeclaredField("fluidRenderers");
            fluidRenderersField.setAccessible(true);
            customFluidRenderers = (Map<IRegistryDelegate<Fluid>, ICCBlockRenderer>) fluidRenderersField.get(null);

            final Field globalRenderersField = BlockRenderingRegistry.class.getDeclaredField("globalRenderers");
            globalRenderersField.setAccessible(true);
            customGlobalRenderers = (List<ICCBlockRenderer>) globalRenderersField.get(null);
        }
        catch (Throwable t) {
            LOGGER.error("Could not retrieve custom renderers");
        }
    }
}
