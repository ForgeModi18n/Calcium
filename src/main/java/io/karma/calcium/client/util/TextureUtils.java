package io.karma.calcium.client.util;

import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public final class TextureUtils {
    private static final ResourceLocation missingLocation = new ResourceLocation("missingno");
    private static TextureAtlasSprite missingTexture;
    private static boolean areTexturesLoaded;

    // @formatter:off
    private TextureUtils() {}
    // @formatter:on

    public static void init(final @NotNull IEventBus bus) {
        bus.addListener(TextureUtils::onTextureStitchPre);
        bus.addListener(TextureUtils::onTextureStitchPost);
    }

    private static void onTextureStitchPre(final @NotNull TextureStitchEvent.Pre event) {
        final AtlasTexture texture = event.getMap();
        missingTexture = texture.getSprite(missingLocation);
    }

    private static void onTextureStitchPost(final @NotNull TextureStitchEvent.Post event) {
        areTexturesLoaded = true;
    }

    public static @NotNull TextureAtlasSprite getMissingTexture() {
        ensureTexturesLoaded();
        return missingTexture;
    }

    private static void ensureTexturesLoaded() {
        if (!areTexturesLoaded) {
            throw new IllegalStateException("Tried retrieving texture atlas sprite too early");
        }
    }
}
