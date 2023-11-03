package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.api.ILightInfoProvider;
import dev.redstudio.alfheim.api.ILightLevelProvider;
import dev.redstudio.alfheim.api.ILightingEngineProvider;
import dev.redstudio.alfheim.lighting.LightingEngine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Luna Lage (Desoroxxx)
 * @author Angeline (@jellysquid)
 * @since 1.0
 */
@Mixin(World.class)
public abstract class WorldMixin implements ILightingEngineProvider, ILightLevelProvider {

    @Unique private LightingEngine alfheim$lightingEngine;

    @Shadow private int skylightSubtracted;

    @Shadow public abstract int getLightFor(final EnumSkyBlock lightType, final BlockPos blockPos);

    @Shadow public abstract int getLight(final BlockPos blockPos);

    @Shadow public abstract IBlockState getBlockState(final BlockPos blockPos);

    /**
     * Initialize the lighting engine on world construction.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(final CallbackInfo callbackInfo) {
        alfheim$lightingEngine = new LightingEngine((World) (Object) this);
    }

    // Todo: We should be the only ones overwriting theses methods, cancelling like that is just a bad overwrite, in Dev 2 try to overwrite them and see if chaos unfolds

    /**
     * Directs the light update to the lighting engine and always returns a success value.
     */
    @Inject(method = "checkLightFor", at = @At(value = "HEAD"), cancellable = true)
    private void redirectLightUpdate(final EnumSkyBlock lightType, final BlockPos blockPos, final CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        alfheim$lightingEngine.scheduleLightUpdate(lightType, blockPos);

        callbackInfoReturnable.setReturnValue(true);
    }

    @Inject(method = "getLight(Lnet/minecraft/util/math/BlockPos;Z)I", at = @At("HEAD"), cancellable = true)
    private void getLight(final BlockPos blockPos, final boolean checkNeighbors, final CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        if (!checkNeighbors)
            callbackInfoReturnable.setReturnValue(getLight(blockPos));

        final IBlockState blockState = getBlockState(blockPos);

        callbackInfoReturnable.setReturnValue(Math.max(((ILightInfoProvider) blockState).alfheim$getLightFor(((World) (Object) this), EnumSkyBlock.BLOCK, blockPos), ((ILightInfoProvider) blockState).alfheim$getLightFor(((World) (Object) this), EnumSkyBlock.SKY, blockPos) - skylightSubtracted));
    }

    @SideOnly(Side.CLIENT)
    @Inject(method = "getLightFromNeighborsFor", at = @At("HEAD"), cancellable = true)
    private void getLightFromNeighborsFor(final EnumSkyBlock lightType, final BlockPos blockPos, final CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        callbackInfoReturnable.setReturnValue(((ILightInfoProvider) getBlockState(blockPos)).alfheim$getLightFor(((World) (Object) this), lightType, blockPos));
    }

    @Override
    public LightingEngine alfheim$getLightingEngine() {
        return alfheim$lightingEngine;
    }

    @Override
    public int alfheim$getLight(final EnumSkyBlock lightType, final BlockPos blockPos) {
        return getLightFor(lightType, blockPos);
    }
}
