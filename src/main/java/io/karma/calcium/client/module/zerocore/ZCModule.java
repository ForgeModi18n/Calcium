package io.karma.calcium.client.module.zerocore;

import io.karma.calcium.client.module.AbstractCAModule;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public final class ZCModule extends AbstractCAModule {
    public ZCModule() {
        super("zerocore", "ZeroCore");
    }

    @Override
    public void init(@NotNull FMLClientSetupEvent event) {

    }
}
