package io.karma.calcium.client.module;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public interface ICAModule {
    @NotNull String getId();

    @NotNull String getName();

    void init(final @NotNull FMLClientSetupEvent event);
}
