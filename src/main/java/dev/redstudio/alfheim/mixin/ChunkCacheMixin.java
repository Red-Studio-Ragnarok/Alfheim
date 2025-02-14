package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.api.ILightInfoProvider;
import dev.redstudio.alfheim.api.ILightLevelProvider;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkCache;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

/// @author Luna Lage (Desoroxxx)
/// @version 2024-11-08
/// @since 1.0
@Mixin(ChunkCache.class)
public abstract class ChunkCacheMixin implements ILightLevelProvider {

    @SideOnly(Side.CLIENT) @Shadow public abstract int getLightFor(final EnumSkyBlock lightType, final BlockPos blockPos);

    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    /// @reason Redirect to our lighting engine.
    /// @author Luna Lage (Desoroxxx)
    @Overwrite
    @SideOnly(Side.CLIENT)
    private int getLightForExt(final EnumSkyBlock lightType, final BlockPos blockPos) {
        return ((ILightInfoProvider) getBlockState(blockPos)).alfheim$getLightFor(((ChunkCache) (Object) this), lightType, blockPos);
    }

    @Override
    public int alfheim$getLight(final EnumSkyBlock lightType, final BlockPos blockPos) {
        return getLightFor(lightType, blockPos);
    }
}
