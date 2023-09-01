package io.redstudioragnarok.alfheim.lighting;

import atomicstryker.dynamiclights.client.DynamicLights;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.common.Loader;

/**
 * @author Luna Lage (Desoroxxx)
 * @author embeddedt
 * @since 0.1
 */
public final class LightUtil {

    private static final boolean DYNAMIC_LIGHTS_LOADED = Loader.isModLoaded("dynamiclights");

    public static int getLightValueForState(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
        if (DYNAMIC_LIGHTS_LOADED)
            return DynamicLights.getLightValue(blockState.getBlock(), blockState, blockAccess, blockPos); // Use the Dynamic Lights implementation
        else
            return blockState.getLightValue(blockAccess, blockPos); // Use the vanilla implementation
    }
}
