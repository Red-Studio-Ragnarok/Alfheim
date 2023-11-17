package dev.redstudio.alfheim.mixin.client;

import dev.redstudio.redcore.math.ClampUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BlockModelRenderer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * @author Luna Lage (Desoroxxx)
 * @since 1.0
 */
@SideOnly(Side.CLIENT)
@Mixin(BlockModelRenderer.class)
public abstract class BlockModelRendererMixin {

    /**
     * This fixes <a href="https://bugs.mojang.com/browse/MC-225516">MC-225516</a> the original intent was that block that emit lights shouldn't have an ambient occlusion applied to them which is logical.
     * But some light-emitting blocks have very low light values which are barely visible for these blocks ambient occlusion should still be applied.
     * In Vanilla, I found that these low light values emitting blocks have a value of one which allows us to fix the issue by subtracting one to the light value check and then clamping it to 0-15.
     */
    @Redirect(
            method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/block/state/IBlockState;getLightValue(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)I", remap = false),
            require = 0
    )
    private int adjustGetLightValue(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
        return ClampUtil.clampMinFirst(blockState.getLightValue(blockAccess, blockPos) -1, 0, 15);
    }

    /**
     * This fixes <a href="https://bugs.mojang.com/browse/MC-225516">MC-225516</a> for OptiFine, the original intent was that block that emit lights shouldn't have an ambient occlusion applied to them which is logical.
     * But some light-emitting blocks have very low light values which are barely visible for these blocks ambient occlusion should still be applied.
     * In Vanilla, I found that these low light values emitting blocks have a value of one which allows us to fix the issue by subtracting one to the light value check and then clamping it to 0-15.
     */
    @Redirect(
            method = "renderModel(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/client/renderer/block/model/IBakedModel;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/renderer/BufferBuilder;ZJ)Z",
            at = @At(value = "INVOKE", target = "Lnet/optifine/reflect/ReflectorForge;getLightValue(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;)I", remap = false),
            require = 0
    )
    private int adjustGetLightValueOptiFine(final IBlockState blockState, final IBlockAccess blockAccess, final BlockPos blockPos) {
        return ClampUtil.clampMinFirst(blockState.getLightValue(blockAccess, blockPos) -1, 0, 15);
    }
}
