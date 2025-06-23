package dev.redstudio.alfheim.lighting;

import atomicstryker.dynamiclights.client.DynamicLights;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import static dev.redstudio.alfheim.Alfheim.IS_DYNAMIC_LIGHTS_LOADED;

/// @author Luna Mira Lage (Desoroxxx)
/// @author embeddedt
/// @since 1.0
public final class LightUtil {

	public static int getLightValueForState(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
		if (IS_DYNAMIC_LIGHTS_LOADED) {
			return DynamicLights.getLightValue(blockState.getBlock(), blockState, blockAccess, blockPos); // Use the Dynamic Lights implementation
		} else {
			return blockState.getLightValue(blockAccess, blockPos); // Use the vanilla implementation
		}
	}
}
