package io.redstudioragnarok.alfheim.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

/**
 * @author Luna Lage (Desoroxxx)
 * @author Angeline (@jellysquid)
 * @since 0.1
 */
public interface IChunkLighting {

    byte alfheim$getCachedLightFor(final EnumSkyBlock enumSkyBlock, final BlockPos pos);
}
