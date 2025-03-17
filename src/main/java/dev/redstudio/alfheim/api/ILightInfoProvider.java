package dev.redstudio.alfheim.api;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;

/// @author Luna Mira Lage (Desoroxxx)
/// @version 2023-09-06
/// @since 1.0
public interface ILightInfoProvider {

	int alfheim$getLightFor(final IBlockAccess iBlockAccess, final EnumSkyBlock lightType, final BlockPos blockPos);

	boolean alfheim$useNeighborBrightness(final EnumFacing facing, final IBlockAccess blockAccess, final BlockPos blockPos);

	int alfheim$getLightOpacity(final EnumFacing facing, final IBlockAccess blockAccess, final BlockPos blockPos);
}
