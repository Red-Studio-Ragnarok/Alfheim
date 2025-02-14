package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.Alfheim;
import dev.redstudio.alfheim.api.IChunkLightingData;
import dev.redstudio.alfheim.api.ILightingEngineProvider;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.redstudio.alfheim.ProjectConstants.LOGGER;

/// @author Luna Mira Lage (Desoroxxx)
/// @author Angeline (@jellysquid)
/// @version 2023-11-03
/// @since 1.0
@Mixin(AnvilChunkLoader.class)
public abstract class AnvilChunkLoaderMixin {

    @Unique private static final String NEIGHBOR_LIGHT_CHECKS_KEY = "NeighborLightChecks";

    /// Injects into the head of saveChunk() to forcefully process all pending light updates. Fail-safe.
    ///
    /// @author Angeline (@jellysquid)
    @Inject(method = "saveChunk", at = @At("HEAD"))
    private void onConstructed(final World world, final Chunk chunk, final CallbackInfo callbackInfo) {
        ((ILightingEngineProvider) world).alfheim$getLightingEngine().processLightUpdates();
    }

    /// Injects the deserialization logic for chunk data on load so we can extract whether or not we've populated light yet.
    ///
    /// @author Angeline (@jellysquid)
    @Inject(method = "readChunkFromNBT", at = @At("RETURN"))
    private void onReadChunkFromNBT(final World world, final NBTTagCompound compound, final CallbackInfoReturnable<Chunk> callbackInfoReturnable) {
        final Chunk chunk = callbackInfoReturnable.getReturnValue();

        alfheim$readNeighborLightChecksFromNBT(chunk, compound);

        ((IChunkLightingData) chunk).alfheim$setLightInitialized(compound.getBoolean("LightPopulated"));
    }

    /// Injects the serialization logic for chunk data on save, so we can store whether or not we've populated light yet.
    ///
    /// @author Angeline (@jellysquid)
    @Inject(method = "writeChunkToNBT", at = @At("RETURN"))
    private void onWriteChunkToNBT(final Chunk chunk, final World world, final NBTTagCompound compound, final CallbackInfo callbackInfo) {
        alfheim$writeNeighborLightChecksToNBT(chunk, compound);

        compound.setBoolean("LightPopulated", ((IChunkLightingData) chunk).alfheim$isLightInitialized());
    }

    @Unique
    private static void alfheim$readNeighborLightChecksFromNBT(final Chunk chunk, final NBTTagCompound compound) {
        if (!compound.hasKey(NEIGHBOR_LIGHT_CHECKS_KEY, 9))
            return;

        final NBTTagList tagList = compound.getTagList(NEIGHBOR_LIGHT_CHECKS_KEY, 2);

        if (tagList.tagCount() != Alfheim.FLAG_COUNT) {
            LOGGER.warn("Chunk field {} had invalid length, ignoring it (chunk coordinates: {} {})", NEIGHBOR_LIGHT_CHECKS_KEY, chunk.x, chunk.z);
            return;
        }

        ((IChunkLightingData) chunk).alfheim$initNeighborLightChecks();

        final short[] neighborLightChecks = ((IChunkLightingData) chunk).alfheim$getNeighborLightChecks();

        for (int i = 0; i < Alfheim.FLAG_COUNT; ++i)
            neighborLightChecks[i] = ((NBTTagShort) tagList.get(i)).getShort();
    }

    @Unique
    private static void alfheim$writeNeighborLightChecksToNBT(final Chunk chunk, final NBTTagCompound compound) {
        final short[] neighborLightChecks = ((IChunkLightingData) chunk).alfheim$getNeighborLightChecks();

        if (neighborLightChecks == null)
            return;

        boolean empty = true;

        final NBTTagList list = new NBTTagList();

        for (final short flags : neighborLightChecks) {
            list.appendTag(new NBTTagShort(flags));

            if (flags != 0)
                empty = false;
        }

        if (!empty)
            compound.setTag(NEIGHBOR_LIGHT_CHECKS_KEY, list);
    }
}
