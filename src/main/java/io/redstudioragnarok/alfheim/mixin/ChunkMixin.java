package io.redstudioragnarok.alfheim.mixin;

import io.redstudioragnarok.alfheim.api.IChunkLighting;
import io.redstudioragnarok.alfheim.api.IChunkLightingData;
import io.redstudioragnarok.alfheim.api.ILightingEngineProvider;
import io.redstudioragnarok.alfheim.lighting.LightingEngine;
import io.redstudioragnarok.alfheim.lighting.LightingHooks;
import io.redstudioragnarok.alfheim.utils.WorldChunkSlice;
import net.minecraft.block.state.IBlockState;
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

    @Final @Shadow private boolean[] updateSkylightColumns;

    @Shadow private boolean dirty;
    @Shadow private boolean isTerrainPopulated;
    @Shadow private boolean isGapLightingUpdated;

    @Final @Shadow public int x;
    @Final @Shadow public int z;

    @Shadow @Final private int[] heightMap;

    @Shadow private int heightMapMinimum;

    @Shadow @Final private World world;

    @Shadow @Final private ExtendedBlockStorage[] storageArrays;

    @Shadow protected abstract int getBlockLightOpacity(final int x, final int y, final int z);

    @Shadow public abstract boolean canSeeSky(final BlockPos blockPos);

    @Shadow protected abstract void setSkylightUpdated();

    @Shadow public abstract int getHeightValue(final int x, final int z);

    @Unique private static final EnumFacing[] alfheim$ENUM_FACING_HORIZONTAL = EnumFacing.Plane.HORIZONTAL.facings();

    @Unique private boolean alfheim$isLightInitialized;

    @Unique private short[] alfheim$neighborLightChecks;

    @Unique private LightingEngine alfheim$lightingEngine;

    /**
     * Callback injected into the Chunk ctor to cache a reference to the lighting engine from the world.
     *
     * @author Angeline (@jellysquid)
     */
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void onConstructed(final CallbackInfo callbackInfo) {
        alfheim$lightingEngine = ((ILightingEngineProvider) world).alfheim$getLightingEngine();
    }

    /**
     * Callback injected to the head of {@link Chunk#getLightSubtracted(BlockPos, int)} to force deferred light updates to be processed.
     *
     * @author Angeline (@jellysquid)
     */
    @Inject(method = "getLightSubtracted", at = @At("HEAD"))
    private void onGetLightSubtracted(final BlockPos blockPos, final int amount, final CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        alfheim$lightingEngine.processLightUpdates();
    }

    /**
     * Callback injected at the end of {@link Chunk#onLoad()} to have previously scheduled light updates scheduled again.
     *
     * @author Angeline (@jellysquid)
     */
    @Inject(method = "onLoad", at = @At("RETURN"))
    private void onLoad(final CallbackInfo callbackInfo) {
        LightingHooks.scheduleRelightChecksForChunkBoundaries(world, (Chunk) (Object) this);
    }

    /**
     * Replaces the call in {@link Chunk#setLightFor(EnumSkyBlock, BlockPos, int)} with our hook.
     *
     * @author Angeline (@jellysquid)
     */
    @Redirect(method = "setLightFor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;generateSkylightMap()V"), expect = 0)
    private void setLightForRedirectGenerateSkylightMap(final Chunk chunk, final EnumSkyBlock lightType, final BlockPos blockPos, final int value) {
        LightingHooks.initSkylightForSection(this.world, (Chunk) (Object) this, storageArrays[blockPos.getY() >> 4]);
    }

    /**
     * @reason Overwrites relightBlock with a more efficient implementation.
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    private void relightBlock(final int x, final int y, final int z) {
        int heightMapY = heightMap[z << 4 | x] & 255;
        int newHeightMapY = heightMapY;

        if (y > heightMapY)
            newHeightMapY = y;

        while (newHeightMapY > 0 && getBlockLightOpacity(x, newHeightMapY - 1, z) == 0)
            --newHeightMapY;

        if (newHeightMapY == heightMapY)
            return;

        heightMap[z << 4 | x] = newHeightMapY;

        if (world.provider.hasSkyLight())
            LightingHooks.relightSkylightColumn(world, (Chunk) (Object) this, x, z, heightMapY, newHeightMapY);

        final int heightMapY1 = heightMap[z << 4 | x];

        if (heightMapY1 < heightMapMinimum)
            heightMapMinimum = heightMapY1;
    }

    /**
     * @reason Calculate light updates only as needed.
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    public int getLightFor(final EnumSkyBlock lightType, final BlockPos pos) {
        alfheim$lightingEngine.processLightUpdatesForType(lightType);

        return alfheim$getCachedLightFor(lightType, pos);
    }

    /**
     * @reason Check chunk lighting and returns immediately after.
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    public void checkLight() {
        isTerrainPopulated = true;

        LightingHooks.checkChunkLighting((Chunk) (Object) this, world);
    }

    /**
     * @reason Avoids chunk fetches as much as possible.
     * @author Angeline (@jellysquid), Luna Lage (Desoroxxx)
     */
    @Overwrite
    private void recheckGaps(final boolean onlyOne) {
        if (!world.isAreaLoaded(new BlockPos(x * 16 + 8, 0, z * 16 + 8), 16))
            return;

        final WorldChunkSlice slice = new WorldChunkSlice(world.getChunkProvider(), x, z);

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (!alfheim$recheckGapsForColumn(slice, x, z))
                    continue;

                if (onlyOne)
                    return;
            }
        }

        isGapLightingUpdated = false;
    }

    @Unique
    private boolean alfheim$recheckGapsForColumn(final WorldChunkSlice slice, final int x, final int z) {
        final int i = x + z * 16;

        if (updateSkylightColumns[i]) {
            updateSkylightColumns[i] = false;

            final int x1 = this.x * 16 + x;
            final int z1 = this.z * 16 + z;

            alfheim$recheckGapsSkylightNeighborHeight(slice, x1, z1, getHeightValue(x, z), alfheim$recheckGapsGetLowestHeight(slice, x1, z1));

            return true;
        }

        return false;
    }

    @Unique
    private int alfheim$recheckGapsGetLowestHeight(final WorldChunkSlice slice, final int x, final int z) {
        int max = Integer.MAX_VALUE;

        for (final EnumFacing facing : alfheim$ENUM_FACING_HORIZONTAL) {
            final Chunk chunk = slice.getChunkFromWorldCoords(x + facing.getXOffset(), z + facing.getZOffset());

            if (chunk != null)
                max = Math.min(max, chunk.getLowestHeight());
        }

        return max;
    }

    @Unique
    private void alfheim$recheckGapsSkylightNeighborHeight(final WorldChunkSlice slice, final int x, final int z, final int height, final int max) {
        alfheim$checkSkylightNeighborHeight(slice, x, z, max);

        for (final EnumFacing facing : alfheim$ENUM_FACING_HORIZONTAL)
            alfheim$checkSkylightNeighborHeight(slice, x + facing.getXOffset(), z + facing.getZOffset(), height);
    }

    @Unique
    private void alfheim$checkSkylightNeighborHeight(final WorldChunkSlice slice, final int x, final int z, final int maxValue) {
        if (slice.getChunkFromWorldCoords(x, z) == null)
            return;

        final int y = slice.getChunkFromWorldCoords(x, z).getHeightValue(x & 15, z & 15);

        if (y > maxValue)
            alfheim$updateSkylightNeighborHeight(slice, x, z, maxValue, y + 1);
        else if (y < maxValue)
            alfheim$updateSkylightNeighborHeight(slice, x, z, y, maxValue + 1);
    }

    @Unique
    private void alfheim$updateSkylightNeighborHeight(final WorldChunkSlice slice, final int x, final int z, final int startY, final int endY) {
        if (endY < startY)
            return;

        if (!slice.isLoaded(x, z, 16))
            return;

        for (int y = startY; y < endY; ++y)
            world.checkLightFor(EnumSkyBlock.SKY, new BlockPos(x, y, z));

        dirty = true;
    }

    /**
     * Redirects the construction of the ExtendedBlockStorage in {@link Chunk#setBlockState(BlockPos, IBlockState)}.
     * We need to initialize the skylight data for the constructed section as soon as possible.
     *
     * @author Angeline (@jellysquid)
     */
    @Redirect(method = "setBlockState", at = @At(value = "NEW", args = "class=net/minecraft/world/chunk/storage/ExtendedBlockStorage"), expect = 0)
    private ExtendedBlockStorage setBlockStateCreateSectionVanilla(final int y, final boolean hasSkyLight) {
        final ExtendedBlockStorage extendedBlockStorage = new ExtendedBlockStorage(y, hasSkyLight);

        LightingHooks.initSkylightForSection(world, (Chunk) (Object) this, extendedBlockStorage);

        return extendedBlockStorage;
    }

    /**
     * Modifies the flag variable of {@link Chunk#setBlockState(BlockPos, IBlockState)} to always be false after it is set, preventing the generation of the sky lightmap.
     *
     * @author Angeline (@jellysquid), Luna Lage (Desoroxxx)
     */
    @ModifyVariable(method = "setBlockState", at = @At(value = "STORE", ordinal = 1), name = "flag")
    private boolean preventGenerateSkylightMap(final boolean original) {
        return false;
    }

    /**
     * Prevent propagateSkylightOcclusion from being called.
     *
     * @author embeddedt
     */
    @Redirect(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;propagateSkylightOcclusion(II)V"))
    private void doPropagateSkylight(final Chunk chunk, final int x, final int z) {
        /* No-op, we don't want skylight propagated */
    }

    /**
     * Prevent getLightFor from being called.
     *
     * @author embeddedt
     */
    @Redirect(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;getLightFor(Lnet/minecraft/world/EnumSkyBlock;Lnet/minecraft/util/math/BlockPos;)I"))
    private int fakeGetLightFor(final Chunk chunk, final EnumSkyBlock lightType, final BlockPos blockPos) {
        return 0;
    }

    @Override
    public short[] alfheim$getNeighborLightChecks() {
        return alfheim$neighborLightChecks;
    }

    @Override
    public void alfheim$setNeighborLightChecks(final short[] data) {
        alfheim$neighborLightChecks = data;
    }

    @Override
    public LightingEngine alfheim$getLightingEngine() {
        return alfheim$lightingEngine;
    }

    @Override
    public boolean alfheim$isLightInitialized() {
        return alfheim$isLightInitialized;
    }

    @Override
    public void alfheim$setLightInitialized(final boolean lightInitialized) {
        alfheim$isLightInitialized = lightInitialized;
    }

    @Override
    public void alfheim$setSkylightUpdatedPublic() {
        setSkylightUpdated();
    }

    @Override
    public byte alfheim$getCachedLightFor(final EnumSkyBlock lightType, final BlockPos blockPos) {
        final int x = blockPos.getX() & 15;
        final int y = blockPos.getY();
        final int z = blockPos.getZ() & 15;

        final ExtendedBlockStorage extendedblockstorage = storageArrays[y >> 4];

        if (extendedblockstorage == Chunk.NULL_BLOCK_STORAGE)
            return canSeeSky(blockPos) ? (byte) lightType.defaultLightValue : 0;
        else if (lightType == EnumSkyBlock.SKY)
            return world.provider.hasSkyLight() ? (byte) extendedblockstorage.getSkyLight(x, y & 15, z) : 0;
        else
            return lightType == EnumSkyBlock.BLOCK ? (byte) extendedblockstorage.getBlockLight(x, y & 15, z) : (byte) lightType.defaultLightValue;
    }
}

