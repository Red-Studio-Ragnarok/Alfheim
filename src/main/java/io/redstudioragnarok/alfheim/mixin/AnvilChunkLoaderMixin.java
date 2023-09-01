package io.redstudioragnarok.alfheim.mixin;

import io.redstudioragnarok.alfheim.api.IChunkLightingData;
import io.redstudioragnarok.alfheim.api.ILightingEngineProvider;
import io.redstudioragnarok.alfheim.lighting.LightingHooks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.AnvilChunkLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Luna Lage (Desoroxxx)
 * @author Angeline (@jellysquid)
 * @since 0.1
 */
@Mixin(AnvilChunkLoader.class)
public abstract class AnvilChunkLoaderMixin {

    /**
     * Injects into the head of saveChunk() to forcefully process all pending light updates. Fail-safe.
     *
     * @author Angeline (@jellysquid)
     */
    @Inject(method = "saveChunk", at = @At("HEAD"))
    private void onConstructed(World world, Chunk chunkIn, CallbackInfo callbackInfo) {
        ((ILightingEngineProvider) world).alfheim$getLightingEngine().processLightUpdates();
    }

    /**
     * Injects the deserialization logic for chunk data on load so we can extract whether or not we've populated light yet.
     *
     * @author Angeline (@jellysquid)
     */
    @Inject(method = "readChunkFromNBT", at = @At("RETURN"))
    private void onReadChunkFromNBT(World world, NBTTagCompound compound, CallbackInfoReturnable<Chunk> cir) {
        Chunk chunk = cir.getReturnValue();

        LightingHooks.readNeighborLightChecksFromNBT(chunk, compound);

        ((IChunkLightingData) chunk).alfheim$setLightInitialized(compound.getBoolean("LightPopulated"));

    }

    /**
     * Injects the serialization logic for chunk data on save so we can store whether or not we've populated light yet.
     *
     * @author Angeline (@jellysquid)
     */
    @Inject(method = "writeChunkToNBT", at = @At("RETURN"))
    private void onWriteChunkToNBT(Chunk chunk, World world, NBTTagCompound compound, CallbackInfo ci) {
        LightingHooks.writeNeighborLightChecksToNBT(chunk, compound);

        compound.setBoolean("LightPopulated", ((IChunkLightingData) chunk).alfheim$isLightInitialized());
    }
}
