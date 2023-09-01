package io.redstudioragnarok.alfheim.mixin;

import io.redstudioragnarok.alfheim.api.ILightInfoProvider;
import io.redstudioragnarok.alfheim.api.ILightLevelProvider;
import io.redstudioragnarok.alfheim.api.ILightingEngineProvider;
import io.redstudioragnarok.alfheim.lighting.LightingEngine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
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
 * @since 0.1
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

    /**
     * Directs the light update to the lighting engine and always returns a success value.
     */
    @Inject(method = "checkLightFor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isAreaLoaded(Lnet/minecraft/util/math/BlockPos;IZ)Z", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
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
