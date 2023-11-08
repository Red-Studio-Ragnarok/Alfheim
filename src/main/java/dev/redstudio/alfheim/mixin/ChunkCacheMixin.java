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

/**
 * @author Luna Lage (Desoroxxx)
 * @since 1.0
 */
@Mixin(ChunkCache.class)
public abstract class ChunkCacheMixin implements ILightLevelProvider {

    @SideOnly(Side.CLIENT) @Shadow public abstract int getLightFor(final EnumSkyBlock lightType, final BlockPos blockPos);

    @Shadow public abstract IBlockState getBlockState(BlockPos pos);

    /**
     * Explicitly making this method we overwrite public to prevent access level conflicts at runtime.
     * Mods may adjust the visibility of this method to public, causing a crash if our overwritten method has different scope.
     *
     * @reason Redirect to our lighting engine.
     * @author Luna Lage (Desoroxxx)
     */
    @Overwrite
    @SideOnly(Side.CLIENT)
    public int getLightForExt(final EnumSkyBlock lightType, final BlockPos blockPos) {
        return ((ILightInfoProvider) getBlockState(blockPos)).alfheim$getLightFor(((ChunkCache) (Object) this), lightType, blockPos);
    }

    @Override
    public int alfheim$getLight(final EnumSkyBlock lightType, final BlockPos blockPos) {
        return getLightFor(lightType, blockPos);
    }
}
