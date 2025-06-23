package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.api.ILightInfoProvider;
import dev.redstudio.alfheim.api.ILightLevelProvider;
import dev.redstudio.alfheim.api.ILightingEngineProvider;
import dev.redstudio.alfheim.lighting.LightingEngine;
import lombok.Getter;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/// @author Luna Mira Lage (Desoroxxx)
/// @author Angeline (@jellysquid)
/// @since 1.0
@Mixin(World.class)
public abstract class WorldMixin implements ILightingEngineProvider, ILightLevelProvider {

	@Unique
	@Getter(lazy = true, onMethod_ = {@Override})
	private final LightingEngine alfheim$lightingEngine = new LightingEngine((World) (Object) this);

	@Shadow
	private int skylightSubtracted;

	@Shadow
	public abstract int getLightFor(final EnumSkyBlock lightType, final BlockPos blockPos);

	@Shadow
	public abstract int getLight(final BlockPos blockPos);

	@Shadow
	public abstract IBlockState getBlockState(final BlockPos blockPos);

	/// @reason Redirect to our lighting engine.
	/// @author Luna Mira Lage (Desoroxxx)
	@Overwrite
	public boolean checkLightFor(final EnumSkyBlock lightType, final BlockPos blockPos) {
		getAlfheim$lightingEngine().scheduleLightUpdate(lightType, blockPos);

		return true;
	}

	/// @reason Redirect to our lighting engine.
	/// @author Luna Mira Lage (Desoroxxx)
	@Overwrite
	public int getLight(final BlockPos blockPos, final boolean checkNeighbors) {
		if (!checkNeighbors)
			return getLight(blockPos);

		final IBlockState blockState = getBlockState(blockPos);

		return Math.max(((ILightInfoProvider) blockState).alfheim$getLightFor(((World) (Object) this), EnumSkyBlock.BLOCK, blockPos), ((ILightInfoProvider) blockState).alfheim$getLightFor(((World) (Object) this), EnumSkyBlock.SKY, blockPos) - skylightSubtracted);
	}

	/// @reason Redirect to our lighting engine.
	/// @author Luna Mira Lage (Desoroxxx)
	@Overwrite
	@SideOnly(Side.CLIENT)
	public int getLightFromNeighborsFor(final EnumSkyBlock lightType, final BlockPos blockPos) {
		return ((ILightInfoProvider) getBlockState(blockPos)).alfheim$getLightFor(((World) (Object) this), lightType, blockPos);
	}

	@Override
	public int alfheim$getLight(final EnumSkyBlock lightType, final BlockPos blockPos) {
		return getLightFor(lightType, blockPos);
	}
}
