package io.redstudioragnarok.alfheim.api;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 0.1
 */
public interface ILitBlock {

    int alfheim$getLightFor(final IBlockState blockState, final IBlockAccess blockAccess, final EnumSkyBlock lightType, final BlockPos blockPos);

    boolean alfheim$useNeighborBrightness(final IBlockState blockState, final EnumFacing facing, final IBlockAccess blockAccess, final BlockPos blockPos);

    int alfheim$getLightOpacity(final IBlockState blockState, final EnumFacing facing, final IBlockAccess blockAccess, final BlockPos blockPos);
}
