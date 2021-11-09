package io.karma.calcium;

import codechicken.lib.render.block.BlockRenderingRegistry;
import codechicken.lib.render.block.ICCBlockRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.List;

@Mod(CalciumMod.MODID)
public class CalciumMod {
    public static final String MODID = "calcium";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    @OnlyIn(Dist.CLIENT)
    private static List<ICCBlockRenderer> customRenderers;

    public CalciumMod() {
        final var bus = FMLJavaModLoadingContext.get().getModEventBus();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> bus.addListener(this::onClientSetup));
    }

    @Nonnull
    @OnlyIn(Dist.CLIENT)
    public static List<ICCBlockRenderer> getCustomRenderers() {
        return customRenderers;
    }

    @SuppressWarnings("unchecked")
    @OnlyIn(Dist.CLIENT)
    private void onClientSetup(@Nonnull FMLClientSetupEvent e) {
        try {
            final var field = BlockRenderingRegistry.class.getDeclaredField("blockRenderers");
            field.setAccessible(true);
            customRenderers = (List<ICCBlockRenderer>) field.get(null);
        }
        catch (Throwable t) {
            LOGGER.error("Could not retrieve custom block renderers");
        }
    }
}
