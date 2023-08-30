package io.redstudioragnarok.alfheim.mixin;

import io.redstudioragnarok.alfheim.api.ILitBlock;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import static net.minecraft.block.BlockStairs.EnumHalf.TOP;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 0.1
 */
@Mixin(BlockStairs.class)
public abstract class BlockStairsMixin extends BlockMixin implements ILitBlock {

    @Shadow @Final public static PropertyEnum<BlockStairs.EnumHalf> HALF;

    @Override
    public boolean alfheim$useNeighborBrightness(final IBlockState blockState, final EnumFacing facing, final IBlockAccess blockAccess, final BlockPos blockPos) {
        if (facing.getAxis() != EnumFacing.Axis.Y)
            return false;

        return facing == (blockState.getValue(HALF) == TOP ? EnumFacing.DOWN : EnumFacing.UP);
    }
}
