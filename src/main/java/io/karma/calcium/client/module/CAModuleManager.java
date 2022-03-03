package io.karma.calcium.client.module;

import io.karma.calcium.CalciumMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLLoader;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Optional;

@OnlyIn(Dist.CLIENT)
public final class CAModuleManager {
    private static final HashMap<String, String> moduleClasses = new HashMap<>();
    private static final HashMap<String, ICAModule> modules = new HashMap<>();

    // @formatter:off
    private CAModuleManager() {}
    // @formatter:on

    public static void registerModule(final @NotNull String modId, final @NotNull String moduleClass) {
        if (moduleClasses.containsKey(modId)) {
            throw new IllegalStateException("Cannot register module twice");
        }

        moduleClasses.put(modId, moduleClass);
    }

    @SuppressWarnings("unchecked")
    public static <M extends ICAModule> @NotNull Optional<M> getModule(final @NotNull String id) {
        return Optional.ofNullable((M) modules.get(id));
    }

    public static void loadModules() {
        for (final Entry<String, String> entry : moduleClasses.entrySet()) {
            final String modId = entry.getKey();

            if (FMLLoader.getLoadingModList().getMods().stream().noneMatch(m -> m.getModId().equals(modId))) {
                continue;
            }

            final String moduleClassName = entry.getValue();
            CalciumMod.LOGGER.info("Found mod '{}', loading patch module {}", modId, moduleClassName);

            try {
                final Class<?> clazz = Class.forName(moduleClassName);

                if (!ICAModule.class.isAssignableFrom(clazz)) {
                    throw new IllegalStateException("Module must implement ICAModule");
                }

                final ICAModule module = (ICAModule) clazz.newInstance();
                modules.put(module.getId(), module);
            }
            catch (final @NotNull Throwable t) {
                CalciumMod.LOGGER.error("Could not load module {} - {}", moduleClassName, t.getMessage());
            }
        }
    }

    public static void initModules(final @NotNull FMLClientSetupEvent event) {
        for (final Entry<String, ICAModule> entry : modules.entrySet()) {
            entry.getValue().init(event);
        }
    }
}
