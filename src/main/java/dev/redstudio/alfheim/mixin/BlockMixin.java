package dev.redstudio.alfheim.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import dev.redstudio.alfheim.api.ILightInfoProvider;
import dev.redstudio.alfheim.api.ILightLevelProvider;
import dev.redstudio.alfheim.api.ILitBlock;
import dev.redstudio.redcore.math.ClampUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 1.0
 */
@Mixin(Block.class)
public abstract class BlockMixin implements ILitBlock {

    @Shadow @Deprecated public abstract int getLightValue(final IBlockState blockState);

    /**
     * @reason Part of non-full block lighting fix
     * @author Luna Lage (Desoroxxx)
     */
    @Overwrite
    @SideOnly(Side.CLIENT)
    public int getPackedLightmapCoords(final IBlockState blockState, final IBlockAccess source, final BlockPos blockPos) {
        return source.getCombinedLight(blockPos, blockState.getLightValue(source, blockPos));
    }

    @Inject(method = "registerBlocks", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;useNeighborBrightness:Z", ordinal = 1, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void checkForLiquid(final CallbackInfo callbackInfo, @Local(ordinal = 15) final Block block, @Local(ordinal = 0) LocalBooleanRef flag) {
        boolean result = flag.get();

        result |= block instanceof BlockFluidBase;
        result |= block instanceof BlockLiquid;

        flag.set(result);
    }

    /**
     * @reason Part of non-full block lighting fix
     * @author Luna Lage (Desoroxxx)
     */
    @Overwrite
    @SideOnly(Side.CLIENT)
    public float getAmbientOcclusionLightValue(final IBlockState blockState) {
        final byte lightValue = (byte) ClampUtil.clampMinFirst(blockState.getLightValue() -1, 0, 15);

        if (lightValue == 0) {
            return blockState.isBlockNormalCube() ? 0.2F : 1;
        } else {
            return 1;
        }
    }

    @Override
    public int alfheim$getLightFor(final IBlockState blockState, final IBlockAccess blockAccess, final EnumSkyBlock lightType, final BlockPos blockPos) {
        int lightLevel = ((ILightLevelProvider) blockAccess).alfheim$getLight(lightType, blockPos);

        if (lightLevel == 15)
            return lightLevel;

        if (!blockState.useNeighborBrightness())
            return lightLevel;

        for (EnumFacing facing : EnumFacing.VALUES) {
            if (((ILightInfoProvider) blockState).alfheim$useNeighborBrightness(facing, blockAccess, blockPos)) {
                int opacity = ((ILightInfoProvider) blockState).alfheim$getLightOpacity(facing, blockAccess, blockPos);
                final int neighborLightLevel = ((ILightLevelProvider) blockAccess).alfheim$getLight(lightType, blockPos.offset(facing));

                if (opacity == 0 && (lightType != EnumSkyBlock.SKY || neighborLightLevel != EnumSkyBlock.SKY.defaultLightValue))
                    opacity = 1;

                lightLevel = Math.max(lightLevel, neighborLightLevel - opacity);

                if (lightLevel == 15)
                    return lightLevel;
            }
        }

        return lightLevel;
    }

    @Override
    public boolean alfheim$useNeighborBrightness(final IBlockState blockState, final EnumFacing facing, final IBlockAccess blockAccess, final BlockPos blockPos) {
        return facing == EnumFacing.UP;
    }

    @Override
    public int alfheim$getLightOpacity(final IBlockState blockState, final EnumFacing facing, final IBlockAccess blockAccess, final BlockPos blockPos) {
        return 0;
    }
}
