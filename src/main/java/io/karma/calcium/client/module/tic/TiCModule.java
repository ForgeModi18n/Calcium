package io.karma.calcium.client.module.tic;

import io.karma.calcium.client.module.AbstractCAModule;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public final class TiCModule extends AbstractCAModule {
    public TiCModule() {
        super("tic", "Tinkers Construct");
    }

    @Override
    public void init(@NotNull FMLClientSetupEvent event) {

    }
}
