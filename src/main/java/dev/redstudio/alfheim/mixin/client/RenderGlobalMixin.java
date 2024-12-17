package dev.redstudio.alfheim.mixin.client;

import dev.redstudio.alfheim.api.ILightUpdatesProcessor;
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

import static dev.redstudio.alfheim.Alfheim.IS_NOTHIRIUM_LOADED;
import static dev.redstudio.alfheim.Alfheim.IS_VINTAGIUM_LOADED;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 1.0
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
     * @since 1.0
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
     * <p>
     * This also limits how many light updates are processed at once.
     *
     * @since 1.0
     */
    @Override
    public void alfheim$processLightUpdates() {
        if (setLightUpdates.isEmpty() || (!IS_NOTHIRIUM_LOADED && !IS_VINTAGIUM_LOADED && renderDispatcher.hasNoFreeRenderBuilders()))
            return;

        final Iterator<BlockPos> iterator = setLightUpdates.iterator();
        final float lightUpdateLimit = 2048 + ((float) setLightUpdates.size() / 4); // Todo: Rework this once again, this is currently pretty dumb.
                                                                                    //       It fixed the issue where the FPS on lower end hardware would plummet for a few seconds.
                                                                                    //       But it also reduced how smooth the frame rate was on higher end hardware.
                                                                                    //       Updating blocks is costly and will take a long time, this is why lower end hardware plummets for a few seconds.
                                                                                    //       Higher end hardware instead has somewhat of a FPS "buffer" which can handle it fine over multiple frames thus reducing frame-time spikes.
                                                                                    //       The technically the best way to do this (that I hadn't though of before) was to add current "FPS" to the equation.
                                                                                    //       If the FPS is low more updates would be done in one frame if it is high we can afford spreading light updates over multiple frames.

        short lightUpdatesProcessed = 0;
        while (iterator.hasNext() && lightUpdatesProcessed < lightUpdateLimit) {
            final BlockPos blockpos = iterator.next();

            iterator.remove();

            final int x = blockpos.getX();
            final int y = blockpos.getY();
            final int z = blockpos.getZ();

            markBlocksForUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1, false);

            lightUpdatesProcessed++;
        }
    }
}
