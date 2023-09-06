package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.api.ILightLevelProvider;
import dev.redstudio.alfheim.api.ILightInfoProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.EnumSkyBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 1.0
 */
@Mixin(ChunkCache.class)
public abstract class ChunkCacheMixin implements ILightLevelProvider {

    @Shadow public abstract int getLightFor(final EnumSkyBlock lightType, final BlockPos blockPos);

    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    @Inject(method = "getLightForExt", at = @At("HEAD"), cancellable = true)
    private void getLightForExt(final EnumSkyBlock lightType, final BlockPos blockPos, final CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(((ILightInfoProvider) getBlockState(blockPos)).alfheim$getLightFor(((ChunkCache) (Object) this), lightType, blockPos));
    }

    @Override
    public int alfheim$getLight(final EnumSkyBlock lightType, final BlockPos blockPos) {
        return getLightFor(lightType, blockPos);
    }
}
