package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.api.ILitBlock;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.block.BlockSlab.EnumBlockHalf.TOP;

/// @author Luna Mira Lage (Desoroxxx)
/// @version 2023-09-06
/// @since 1.0
@Mixin(BlockSlab.class)
public abstract class BlockSlabMixin extends BlockMixin implements ILitBlock {

	@Shadow
	@Final
	public static PropertyEnum<BlockSlab.EnumBlockHalf> HALF;

	@Override
	public boolean alfheim$useNeighborBrightness(final IBlockState blockState, final EnumFacing facing, final IBlockAccess blockAccess, final BlockPos blockPos) {
		if (facing.getAxis() != EnumFacing.Axis.Y) {
			return false;
		}

		if (((BlockSlab) (Object) this).isFullCube(blockState)) {
			return false;
		}

		return facing == (blockState.getValue(HALF) == TOP ? EnumFacing.DOWN : EnumFacing.UP);
	}
}
