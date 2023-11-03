package dev.redstudio.alfheim.asm;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import zone.rong.mixinbooter.IEarlyMixinLoader;

import java.util.List;
import java.util.Map;

import static dev.redstudio.alfheim.ProjectConstants.LOGGER;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 1.0
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("dev.redstudio.alfheim.asm")
public final class AlfheimPlugin implements IFMLLoadingPlugin, IEarlyMixinLoader {

    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(final Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }

    @Override
    public List<String> getMixinConfigs() {
        return ImmutableList.of("mixins.alfheim.json");
    }

    @Override
    public boolean shouldMixinConfigQueue(final String mixinConfig) {
        switch (mixinConfig) {
            case "mixins.alfheim.json":
                if (isCubicChunksInstalled()) {
                    LOGGER.warn("Cubic Chunks was detected, it uses it's own lighting engine, Alfheim will not load");
                    return false;
                }

                return true;
            default:
                return true;
        }
    }

    /**
     * Checks if Cubic Chunks is installed.
     *
     * @return True if Cubic Chunks is installed, false otherwise.
     */
    public static boolean isCubicChunksInstalled() {
        try {
            Class.forName("io.github.opencubicchunks.cubicchunks.core.asm.CubicChunksCoreContainer");
        } catch (final ClassNotFoundException ignored) {
            return false;
        }

        return true;
    }
}
