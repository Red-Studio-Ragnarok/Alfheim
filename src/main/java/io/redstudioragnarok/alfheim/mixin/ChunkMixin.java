package io.redstudioragnarok.alfheim.mixin;

import io.redstudioragnarok.alfheim.utils.WorldChunkSlice;
import io.redstudioragnarok.alfheim.api.IChunkLighting;
import io.redstudioragnarok.alfheim.api.IChunkLightingData;
import io.redstudioragnarok.alfheim.api.ILightingEngineProvider;
import io.redstudioragnarok.alfheim.lighting.LightingEngine;
import io.redstudioragnarok.alfheim.lighting.LightingHooks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Luna Lage (Desoroxxx)
 * @author kappa-maintainer
 * @author embeddedt
 * @author Angeline (@jellysquid)
 * @since 0.1
 */
@Mixin(Chunk.class)
public abstract class ChunkMixin implements IChunkLighting, IChunkLightingData, ILightingEngineProvider {

    @Unique private static final EnumFacing[] HORIZONTAL = EnumFacing.Plane.HORIZONTAL.facings();

    @Unique private static final String SET_BLOCK_STATE_VANILLA = "setBlockState" + "(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)" + "Lnet/minecraft/block/state/IBlockState;";

    @Shadow @Final private ExtendedBlockStorage[] storageArrays;

    @Shadow @Final private int[] heightMap;

    @Shadow private int heightMapMinimum;

    @Shadow @Final private World world;

    @Final @Shadow private boolean[] updateSkylightColumns;

    @Final @Shadow public int x;

    @Final @Shadow public int z;

    @Shadow private boolean isGapLightingUpdated;

    @Shadow
    protected abstract int getBlockLightOpacity(int x, int y, int z);

    @Shadow
    public abstract boolean canSeeSky(BlockPos pos);

    /**
     * Callback injected into the Chunk ctor to cache a reference to the lighting engine from the world.
     *
     * @author Angeline (@jellysquid)
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstructed(CallbackInfo ci) {
        lightingEngine = ((ILightingEngineProvider) world).alfheim$getLightingEngine();
    }

    /**
     * Callback injected to the head of getLightSubtracted(BlockPos, int) to force deferred light updates to be processed.
     *
     * @author Angeline (@jellysquid)
     */
    @Inject(method = "getLightSubtracted", at = @At("HEAD"))
    private void onGetLightSubtracted(BlockPos pos, int amount, CallbackInfoReturnable<Integer> cir) {
        lightingEngine.processLightUpdates();
    }

    /**
     * Callback injected at the end of onLoad() to have previously scheduled light updates scheduled again.
     *
     * @author Angeline (@jellysquid)
     */
    @Inject(method = "onLoad", at = @At("RETURN"))
    private void onLoad(CallbackInfo ci) {
        LightingHooks.scheduleRelightChecksForChunkBoundaries(world, (Chunk) (Object) this);
    }

    // === REPLACEMENTS ===

    /**
     * Replaces the call in setLightFor(Chunk, EnumSkyBlock, BlockPos) with our hook.
     *
     * @author Angeline (@jellysquid)
     */
    @Redirect(method = "setLightFor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;generateSkylightMap()V"), expect = 0)
    private void setLightForRedirectGenerateSkylightMap(Chunk chunk, EnumSkyBlock type, BlockPos pos, int value) {
        LightingHooks.initSkylightForSection(world, (Chunk) (Object) this, storageArrays[pos.getY() >> 4]);
    }

    /**
     * @reason Overwrites relightBlock with a more efficient implementation.
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    private void relightBlock(int x, int y, int z) {
        int i = heightMap[z << 4 | x] & 255;
        int j = i;

        if (y > i) {
            j = y;
        }

        while (j > 0 && getBlockLightOpacity(x, j - 1, z) == 0) {
            --j;
        }

        if (j != i) {
            heightMap[z << 4 | x] = j;

            if (world.provider.hasSkyLight()) {
                LightingHooks.relightSkylightColumn(world, (Chunk) (Object) this, x, z, i, j);
            }

            int l1 = heightMap[z << 4 | x];

            if (l1 < heightMapMinimum) {
                heightMapMinimum = l1;
            }
        }
    }

    /**
     * @reason Hook for calculating light updates only as needed. {@link ChunkMixin#alfheim$getCachedLightFor(EnumSkyBlock, BlockPos)} does not call this hook.
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    public int getLightFor(EnumSkyBlock type, BlockPos pos) {
        lightingEngine.processLightUpdatesForType(type);

        return alfheim$getCachedLightFor(type, pos);
    }

    /**
     * @reason Hooks into checkLight() to check chunk lighting and returns immediately after, voiding the rest of the function.
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    public void checkLight() {

        LightingHooks.checkChunkLighting((Chunk) (Object) this, world);
    }

    /**
     * @reason Optimized version of recheckGaps. Avoids chunk fetches as much as possible.
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    private void recheckGaps(boolean onlyOne) {
        world.profiler.startSection("recheckGaps");

        WorldChunkSlice slice = new WorldChunkSlice(world, x, z);

        if (world.isAreaLoaded(new BlockPos(x * 16 + 8, 0, z * 16 + 8), 16)) {
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    if (recheckGapsForColumn(slice, x, z)) {
                        if (onlyOne) {
                            world.profiler.endSection();

                            return;
                        }
                    }
                }
            }

            isGapLightingUpdated = false;
        }

        world.profiler.endSection();
    }

    private boolean recheckGapsForColumn(WorldChunkSlice slice, int x, int z) {
        int i = x + z * 16;

        if (updateSkylightColumns[i]) {
            updateSkylightColumns[i] = false;

            int height = getHeightValue(x, z);

            int x1 = x * 16 + x;
            int z1 = z * 16 + z;

            int max = recheckGapsGetLowestHeight(slice, x1, z1);

            recheckGapsSkylightNeighborHeight(slice, x1, z1, height, max);

            return true;
        }

        return false;
    }

    private int recheckGapsGetLowestHeight(WorldChunkSlice slice, int x, int z) {
        int max = Integer.MAX_VALUE;

        for (EnumFacing facing : HORIZONTAL) {
            int j = x + facing.getXOffset();
            int k = z + facing.getZOffset();
            Chunk chunk = slice.getChunkFromWorldCoords(j, k);
            if (chunk != null) {
                max = Math.min(max, slice.getChunkFromWorldCoords(j, k).getLowestHeight());
            }

        }

        return max;
    }

    private void recheckGapsSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int height, int max) {
        checkSkylightNeighborHeight(slice, x, z, max);

        for (EnumFacing facing : HORIZONTAL) {
            int j = x + facing.getXOffset();
            int k = z + facing.getZOffset();

            checkSkylightNeighborHeight(slice, j, k, height);
        }
    }

    private void checkSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int maxValue) {
        if (slice.getChunkFromWorldCoords(x, z) == null) {
            return;
        }
        int i = slice.getChunkFromWorldCoords(x, z).getHeightValue(x & 15, z & 15);

        if (i > maxValue) {
            updateSkylightNeighborHeight(slice, x, z, maxValue, i + 1);
        } else if (i < maxValue) {
            updateSkylightNeighborHeight(slice, x, z, i, maxValue + 1);
        }
    }

    private void updateSkylightNeighborHeight(WorldChunkSlice slice, int x, int z, int startY, int endY) {
        if (endY > startY) {
            if (!slice.isLoaded(x, z, 16)) {
                return;
            }

            for (int i = startY; i < endY; ++i) {
                world.checkLightFor(EnumSkyBlock.SKY, new BlockPos(x, i, z));
            }

        }
    }

    @Shadow
    public abstract int getHeightValue(int i, int j);

    // === INTERFACE IMPL ===

    private short[] neighborLightChecks;

    private boolean isLightInitialized;

    private LightingEngine lightingEngine;

    @Override
    public short[] getNeighborLightChecks() {
        return neighborLightChecks;
    }

    @Override
    public void setNeighborLightChecks(short[] data) {
        neighborLightChecks = data;
    }

    @Override
    public LightingEngine alfheim$getLightingEngine() {
        return lightingEngine;
    }

    @Override
    public boolean isLightInitialized() {
        return isLightInitialized;
    }

    @Override
    public void setLightInitialized(boolean lightInitialized) {
        isLightInitialized = lightInitialized;
    }

    @Shadow
    protected abstract void setSkylightUpdated();

    @Override
    public void setSkylightUpdatedPublic() {
        setSkylightUpdated();
    }

    @Override
    public byte alfheim$getCachedLightFor(final EnumSkyBlock lightType, final BlockPos blockPos) {
        final int x = blockPos.getX() & 15;
        final int y = blockPos.getY();
        final int z = blockPos.getZ() & 15;

        final ExtendedBlockStorage extendedblockstorage = storageArrays[y >> 4];

        if (extendedblockstorage == Chunk.NULL_BLOCK_STORAGE) {
            if (canSeeSky(blockPos))
                return (byte) lightType.defaultLightValue;
            else
                return 0;
        } else if (lightType == EnumSkyBlock.SKY) {
            if (!world.provider.hasSkyLight())
                return 0;
            else
                return (byte) extendedblockstorage.getSkyLight(x, y & 15, z);
        } else {
            if (lightType == EnumSkyBlock.BLOCK)
                return (byte) extendedblockstorage.getBlockLight(x, y & 15, z);
            else
                return (byte) lightType.defaultLightValue;
        }
    }

    // === END OF INTERFACE IMPL ===

    /**
     * Redirects the construction of the ExtendedBlockStorage in setBlockState(BlockPos, IBlockState). We need to initialize
     * the skylight data for the constructed section as soon as possible.
     *
     * @author Angeline (@jellysquid)
     */
    @Redirect(method = SET_BLOCK_STATE_VANILLA, at = @At(value = "NEW", args = "class=net/minecraft/world/chunk/storage/ExtendedBlockStorage"), expect = 0)
    private ExtendedBlockStorage setBlockStateCreateSectionVanilla(int y, boolean storeSkylight) {
        return initSection(y, storeSkylight);
    }

    private ExtendedBlockStorage initSection(int y, boolean storeSkylight) {
        ExtendedBlockStorage storage = new ExtendedBlockStorage(y, storeSkylight);

        LightingHooks.initSkylightForSection(world, (Chunk) (Object) this, storage);

        return storage;
    }

    /**
     * Modifies the flag variable of setBlockState(BlockPos, IBlockState) to always be false after it is set.
     *
     * @author Angeline (@jellysquid)
     */
    @ModifyVariable(method = SET_BLOCK_STATE_VANILLA, at = @At(value = "STORE", ordinal = 1), index = 13, name = "flag", allow = 1)
    private boolean setBlockStateInjectGenerateSkylightMapVanilla(boolean generateSkylight) {
        return false;
    }

    /**
     * Prevent propagateSkylightOcclusion from being called.
     *
     * @author embeddedt
     */
    @Redirect(method = SET_BLOCK_STATE_VANILLA, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;propagateSkylightOcclusion(II)V"))
    private void doPropagateSkylight(Chunk chunk, int i1, int i2) {
        /* No-op, we don't want skylight propagated */
    }

    /**
     * Prevent getLightFor from being called.
     *
     * @author embeddedt
     */
    @Redirect(method = SET_BLOCK_STATE_VANILLA, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getLightFor(Lnet/minecraft/world/EnumSkyBlock;Lnet/minecraft/util/math/BlockPos;)I"))
    private int getFakeLightFor(Chunk chunk, EnumSkyBlock skyBlock, BlockPos blockPos) {
        return 0;
    }
}
