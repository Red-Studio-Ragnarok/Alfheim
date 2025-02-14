package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.api.ILightingEngineProvider;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/// @author Luna Lage (Desoroxxx)
/// @version 2023-09-06
/// @since 1.0
@Mixin(SPacketChunkData.class)
public abstract class SPacketChunkDataMixin {

    /// Redirect a call to [Chunk#getWorld()] in the ctor to force light updates to be processed before creating the client payload.
    @Redirect(method = "<init>(Lnet/minecraft/world/chunk/Chunk;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getWorld()Lnet/minecraft/world/World;"))
    private World processLightUpdates(final Chunk chunk) {
        ((ILightingEngineProvider) chunk).alfheim$getLightingEngine().processLightUpdates();

        return chunk.getWorld();
    }
}
