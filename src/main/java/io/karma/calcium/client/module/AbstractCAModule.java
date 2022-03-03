package io.karma.calcium.client.module;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public abstract class AbstractCAModule implements ICAModule {
    protected final String id;
    protected final String name;

    public AbstractCAModule(final @NotNull String id, final @NotNull String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public @NotNull String getId() {
        return id;
    }

    @Override
    public @NotNull String getName() {
        return name;
    }
}
