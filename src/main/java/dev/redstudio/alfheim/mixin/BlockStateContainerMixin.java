package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.api.ILitBlock;
import dev.redstudio.alfheim.api.ILightInfoProvider;
import net.minecraft.block.Block;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 1.0
 */
@Mixin(BlockStateContainer.class)
public abstract class BlockStateContainerMixin implements ILightInfoProvider {

    @Shadow @Final private Block block;

    @Override
    public int alfheim$getLightFor(IBlockAccess iBlockAccess, EnumSkyBlock lightType, BlockPos blockPos) {
        return ((ILitBlock) block).alfheim$getLightFor(((IBlockState) this), iBlockAccess, lightType, blockPos);
    }

    @Override
    public boolean alfheim$useNeighborBrightness(EnumFacing facing, IBlockAccess blockAccess, BlockPos blockPos) {
        return ((ILitBlock) block).alfheim$useNeighborBrightness(((IBlockState) this), facing, blockAccess, blockPos);
    }

    @Override
    public int alfheim$getLightOpacity(EnumFacing facing, IBlockAccess blockAccess, BlockPos blockPos) {
        return ((ILitBlock) block).alfheim$getLightOpacity(((IBlockState) this), facing, blockAccess, blockPos);
    }

    @Mixin(BlockStateContainer.StateImplementation.class)
    public static class StateImplementationMixin implements ILightInfoProvider {

        @Shadow @Final private Block block;

        @Override
        public int alfheim$getLightFor(IBlockAccess iBlockAccess, EnumSkyBlock lightType, BlockPos blockPos) {
            return ((ILitBlock) block).alfheim$getLightFor(((IBlockState) this), iBlockAccess, lightType, blockPos);
        }

        @Override
        public boolean alfheim$useNeighborBrightness(EnumFacing facing, IBlockAccess blockAccess, BlockPos blockPos) {
            return ((ILitBlock) block).alfheim$useNeighborBrightness(((IBlockState) this), facing, blockAccess, blockPos);
        }

        @Override
        public int alfheim$getLightOpacity(EnumFacing facing, IBlockAccess blockAccess, BlockPos blockPos) {
            return ((ILitBlock) block).alfheim$getLightOpacity(((IBlockState) this), facing, blockAccess, blockPos);
        }
    }
}
