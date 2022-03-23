package io.karma.calcium;

import io.karma.calcium.client.module.CAModuleManager;
import io.karma.calcium.client.util.TextureUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

@Mod(CalciumMod.MODID)
public final class CalciumMod {
    public static final String MODID = "calcium";
    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public CalciumMod() {
        final IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            TextureUtils.init(bus);

            bus.addListener(this::onClientSetup);

            LOGGER.info("Loading modules");
            CAModuleManager.registerModule("codechickenlib", "io.karma.calcium.client.module.ccl.CCLModule");
            CAModuleManager.registerModule("tconstruct", "io.karma.calcium.client.module.tic.TiCModule");
            CAModuleManager.registerModule("ctm", "io.karma.calcium.client.module.ctm.CTMModule");
            CAModuleManager.loadModules();
        });
    }

    @OnlyIn(Dist.CLIENT)
    private void onClientSetup(final @NotNull FMLClientSetupEvent event) {
        CAModuleManager.initModules(event);
    }
}
