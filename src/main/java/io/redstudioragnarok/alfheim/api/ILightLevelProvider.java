package io.redstudioragnarok.alfheim.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

/**
 * @author Luna Lage (Desoroxxx)
 * @since
 */
public interface ILightLevelProvider {

    int alfheim$getLight(final EnumSkyBlock lightType, final BlockPos blockPos);
}
