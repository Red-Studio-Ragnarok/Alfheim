package io.redstudioragnarok.alfheim.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.redstudioragnarok.alfheim.api.ILightInfoProvider;
import io.redstudioragnarok.alfheim.api.ILightLevelProvider;
import io.redstudioragnarok.alfheim.api.ILitBlock;
import io.redstudioragnarok.redcore.utils.MathUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fluids.BlockFluidBase;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 0.1
 */
@Mixin(Block.class)
public abstract class BlockMixin implements ILitBlock {

    @Shadow @Deprecated public abstract int getLightValue(final IBlockState blockState);

    @Inject(method = "getPackedLightmapCoords", at = @At("HEAD"), cancellable = true)
    private void getCorrectPackedLightmapCoords(final IBlockState blockState, final IBlockAccess source, final BlockPos blockPos, final CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(source.getCombinedLight(blockPos, blockState.getLightValue(source, blockPos)));
    }

    @Inject(method = "registerBlocks", at = @At(value = "FIELD", target = "Lnet/minecraft/block/Block;useNeighborBrightness:Z", ordinal = 1, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    private static void checkForLiquid(final CallbackInfo callbackInfo, @Local(ordinal = 15) final Block block, @Local(ordinal = 0) LocalBooleanRef flag) {
        boolean result = flag.get();

        result |= block instanceof BlockFluidBase;
        result |= block instanceof BlockLiquid;

        flag.set(result);
    }

    @Inject(method = "getAmbientOcclusionLightValue", at = @At(value = "HEAD"), cancellable = true)
    private void fuckAO(final IBlockState blockState, final CallbackInfoReturnable<Float> callbackInfoReturnable) {
        final int lightValue = (int) MathUtil.clampMinFirst(blockState.getLightValue() -1, 0, 15);

        if (lightValue == 0)
            callbackInfoReturnable.setReturnValue(blockState.isBlockNormalCube() ? 0.2F : 1);
        else
            callbackInfoReturnable.setReturnValue(1F);
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
