package dev.redstudio.alfheim.lighting;

import atomicstryker.dynamiclights.client.DynamicLights;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.common.Loader;

/// @author Luna Mira Lage (Desoroxxx)
/// @author embeddedt
/// @version 2023-09-06
/// @since 1.0
public final class LightUtil {

    private static final boolean DYNAMIC_LIGHTS_LOADED = Loader.isModLoaded("dynamiclights");

    public static int getLightValueForState(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
        if (DYNAMIC_LIGHTS_LOADED)
            return DynamicLights.getLightValue(blockState.getBlock(), blockState, blockAccess, blockPos); // Use the Dynamic Lights implementation
        else
            return blockState.getLightValue(blockAccess, blockPos); // Use the vanilla implementation
    }
}
