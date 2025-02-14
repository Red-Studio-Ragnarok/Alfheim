package dev.redstudio.alfheim.api;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;

/// @author Luna Lage (Desoroxxx)
/// @version 2023-09-06
/// @since 1.0
public interface ILightLevelProvider {

    int alfheim$getLight(final EnumSkyBlock lightType, final BlockPos blockPos);
}
