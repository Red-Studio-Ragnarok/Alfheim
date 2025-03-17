package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.api.ILightingEngineProvider;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// @author Luna Mira Lage (Desoroxxx)
/// @author Angeline (@jellysquid)
/// @version 2023-09-06
/// @since 1.0
@Mixin(ChunkProviderServer.class)
public abstract class ChunkProviderServerMixin {

	@Shadow
	@Final
	public WorldServer world;

	/// Injects a callback into the start of saveChunks(boolean) to force all light updates to be processed before saving.
	///
	/// @author Angeline (@jellysquid)
	@Inject(method = "saveChunks", at = @At("HEAD"))
	private void onSaveChunks(final boolean all, final CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
		((ILightingEngineProvider) world).getAlfheim$lightingEngine().processLightUpdates();
	}

	/// Injects a callback into the start of the tick() method after the save checks to process all pending light updates.
	/// This is not necessarily required, but we don't want our work queues getting too large.
	///
	/// @author Angeline (@jellysquid)
	@Inject(method = "tick", at = @At(value = "INVOKE", target = "Ljava/util/Set;isEmpty()Z", shift = At.Shift.AFTER))
	private void onTick(final CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
		((ILightingEngineProvider) world).getAlfheim$lightingEngine().processLightUpdates();
	}
}
