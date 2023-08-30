package io.redstudioragnarok.alfheim.mixin.client;

import io.redstudioragnarok.alfheim.api.ILightUpdatesProcessor;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Iterator;
import java.util.Set;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 0.1
 */
@SideOnly(Side.CLIENT)
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin implements ILightUpdatesProcessor {

    @Shadow private ChunkRenderDispatcher renderDispatcher;
    @Shadow @Final private Set<BlockPos> setLightUpdates;

    @Shadow protected abstract void markBlocksForUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean updateImmediately);

    /**
     * Disable vanilla code to replace it with {@link #alfheim$processLightUpdates}
     *
     * @since 0.1
     */
    @Redirect(method = "updateClouds", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z", ordinal = 0))
    private boolean disableVanillaLightUpdates(final Set<BlockPos> instance) {
        return true;
    }

    /**
     * Fixes <a href="https://bugs.mojang.com/browse/MC-80966">MC-80966</a> by not checking if the chunk is empty or not.
     * <p>
     * Also improves performance by updating only the blocks in the set instead of every block in a 3x3 radius around each block in the set.
     * Another performance improvement is using || instead of && allowing to skip earlier when there is nothing to update.
     *
     * @since 0.1
     */
    @Override
    public void alfheim$processLightUpdates() {
        if (setLightUpdates.isEmpty() || renderDispatcher.hasNoFreeRenderBuilders())
            return;

        final Iterator<BlockPos> iterator = setLightUpdates.iterator();
        short lightUpdatesProcessed = 0; // Used to keep the number of light updates reasonable per frame.

        while (iterator.hasNext() && lightUpdatesProcessed < 512) {
            final BlockPos blockpos = iterator.next();

            iterator.remove();

            final int x = blockpos.getX();
            final int y = blockpos.getY();
            final int z = blockpos.getZ();

            markBlocksForUpdate(x, y, z, x, y, z, false);

            lightUpdatesProcessed++;
        }
    }
}
