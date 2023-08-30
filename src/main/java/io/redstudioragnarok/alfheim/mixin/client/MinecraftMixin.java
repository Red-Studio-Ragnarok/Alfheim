package io.redstudioragnarok.alfheim.mixin.client;

import io.redstudioragnarok.alfheim.api.ILightUpdatesProcessor;
import io.redstudioragnarok.alfheim.api.ILightingEngineProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.profiler.Profiler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 0.1
 */
@SideOnly(Side.CLIENT)
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Shadow @Final public Profiler profiler;

    @Shadow public WorldClient world;
    @Shadow public RenderGlobal renderGlobal;

    @Inject(method = "runTick", at = @At(value = "CONSTANT", args = "stringValue=level", shift = At.Shift.BEFORE))
    private void onRunTick(final CallbackInfo callbackInfo) {
        this.profiler.endStartSection("processRenderGlobalLightUpdates");

        ((ILightingEngineProvider) this.world).alfheim$getLightingEngine().processLightUpdates();

        ((ILightUpdatesProcessor) this.renderGlobal).alfheim$processLightUpdates();
    }
}
