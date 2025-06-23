package dev.redstudio.alfheim.mixin.client;

import dev.redstudio.alfheim.api.ILightUpdatesProcessor;
import dev.redstudio.alfheim.utils.DeduplicatedLongQueue;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

import static dev.redstudio.alfheim.Alfheim.*;
import static net.minecraft.util.math.BlockPos.*;

/// @author Luna Mira Lage (Desoroxxx)
/// @version 2025-03-17
/// @since 1.0
@SideOnly(Side.CLIENT)
@Mixin(RenderGlobal.class)
public abstract class RenderGlobalMixin implements ILightUpdatesProcessor {

	@Unique
	private final DeduplicatedLongQueue alfheim$lightUpdatesQueue = new DeduplicatedLongQueue(8192);

	@Shadow
	private ChunkRenderDispatcher renderDispatcher;

	@Shadow
	protected abstract void markBlocksForUpdate(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean updateImmediately);

	/// @author Luna Mira Lage (Desoroxxx)
	/// @reason Use a deduplicated long queue instead of a set
	/// @since 1.5
	@Overwrite
	public void notifyLightSet(final BlockPos blockPos) {
		alfheim$lightUpdatesQueue.enqueue(blockPos.toLong());
	}

	/// Disable vanilla code to replace it with [#alfheim$processLightUpdates]
	///
	/// @since 1.0
	@Redirect(method = "updateClouds", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z", ordinal = 0))
	private boolean disableVanillaLightUpdates(final Set<BlockPos> instance) {
		return true;
	}

	/// Fixes [MC-80966](https://bugs.mojang.com/browse/MC-80966) by not checking if the chunk is empty or not.
	///
	/// It also improves performance by using a [DeduplicatedLongQueue] instead of a set.
	/// This removes the need to use an expensive iterator.
	/// It also reduces memory usage and GC pressure by using long primitives instead of a [BlockPos] object.
	///
	/// Another performance improvement is using || instead of && allowing to skip earlier when there is nothing to update.
	///
	/// @since 1.0
	@Override
	public void alfheim$processLightUpdates() {
		if (alfheim$lightUpdatesQueue.isEmpty() || (!IS_NOTHIRIUM_LOADED && !IS_VINTAGIUM_LOADED && !IS_CELERITAS_LOADED && renderDispatcher.hasNoFreeRenderBuilders())) {
			return;
		}

		while (!alfheim$lightUpdatesQueue.isEmpty()) {
			final long longPos = alfheim$lightUpdatesQueue.dequeue();

			final int x = (int) (longPos << 64 - X_SHIFT - NUM_X_BITS >> 64 - NUM_X_BITS);
			final int y = (int) (longPos << 64 - Y_SHIFT - NUM_Y_BITS >> 64 - NUM_Y_BITS);
			final int z = (int) (longPos << 64 - NUM_Z_BITS >> 64 - NUM_Z_BITS);

			markBlocksForUpdate(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1, false);
		}

		alfheim$lightUpdatesQueue.newDeduplicationSet();
	}
}
