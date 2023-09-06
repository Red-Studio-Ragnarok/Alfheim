package dev.redstudio.alfheim.mixin;

import dev.redstudio.alfheim.api.IChunkLightingData;
import dev.redstudio.alfheim.api.ILightingEngineProvider;
import dev.redstudio.alfheim.utils.EnumBoundaryFacing;
import dev.redstudio.alfheim.lighting.LightingEngine;
import dev.redstudio.alfheim.lighting.LightUtil;
import dev.redstudio.alfheim.utils.WorldChunkSlice;
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

import static dev.redstudio.alfheim.Alfheim.FLAG_COUNT;

/**
 * @author Luna Lage (Desoroxxx)
 * @author kappa-maintainer
 * @author embeddedt
 * @author Angeline (@jellysquid)
 * @since 1.0
 */
@Mixin(Chunk.class)
public abstract class ChunkMixin implements IChunkLightingData, ILightingEngineProvider {

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

    @Unique private static final EnumFacing[] ENUM_FACING_HORIZONTAL = EnumFacing.Plane.HORIZONTAL.facings();

    @Unique private static final EnumSkyBlock[] ENUM_SKY_BLOCK_VALUES = EnumSkyBlock.values();

    @Unique private static final EnumFacing.AxisDirection[] ENUM_AXIS_DIRECTION_VALUES = EnumFacing.AxisDirection.values();

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
        final Chunk chunk = (Chunk) (Object) this;

        for (final EnumFacing facing : EnumFacing.HORIZONTALS) {
            final int xOffset = facing.getXOffset();
            final int zOffset = facing.getZOffset();

            final Chunk nChunk = world.getChunkProvider().getLoadedChunk(chunk.x + xOffset, chunk.z + zOffset);

            if (nChunk == null)
                continue;

            for (final EnumSkyBlock lightType : ENUM_SKY_BLOCK_VALUES) {
                for (final EnumFacing.AxisDirection axisDir : ENUM_AXIS_DIRECTION_VALUES) {
                    // Merge flags upon loading of a chunk. This ensures that all flags are always already on the IN boundary below
                    alfheim$mergeFlags(lightType, chunk, nChunk, facing, axisDir);
                    alfheim$mergeFlags(lightType, nChunk, chunk, facing.getOpposite(), axisDir);

                    // Check everything that might have been canceled due to this chunk not being loaded.
                    // Also, pass in chunks if already known
                    // The boundary to the neighbor chunk (both ways)
                    alfheim$scheduleRelightChecksForBoundary(chunk, nChunk, null, lightType, xOffset, zOffset, axisDir);
                    alfheim$scheduleRelightChecksForBoundary(nChunk, chunk, null, lightType, -xOffset, -zOffset, axisDir);
                    // The boundary to the diagonal neighbor (since the checks in that chunk were aborted if this chunk wasn't loaded, see alfheim$scheduleRelightChecksForBoundary)
                    alfheim$scheduleRelightChecksForBoundary(nChunk, null, chunk, lightType, (zOffset != 0 ? axisDir.getOffset() : 0), (xOffset != 0 ? axisDir.getOffset() : 0), facing.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ? EnumFacing.AxisDirection.NEGATIVE : EnumFacing.AxisDirection.POSITIVE);
                }
            }
        }
    }

    /**
     * Replaces the call in {@link Chunk#setLightFor(EnumSkyBlock, BlockPos, int)} with our hook.
     *
     * @author Angeline (@jellysquid)
     */
    @Redirect(method = "setLightFor", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/chunk/Chunk;generateSkylightMap()V"), expect = 0)
    private void setLightForRedirectGenerateSkylightMap(final Chunk chunk, final EnumSkyBlock lightType, final BlockPos blockPos, final int value) {
        alfheim$initSkylightForSection(storageArrays[blockPos.getY() >> 4]);
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
            alfheim$relightSkylightColumn(x, z, heightMapY, newHeightMapY);

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

        final Chunk chunk = (Chunk) (Object) this;

        if (!((IChunkLightingData) chunk).alfheim$isLightInitialized())
            alfheim$initChunkLighting(chunk, world);

        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                if (x == 0 && z == 0)
                    continue;

                final Chunk nChunk = world.getChunkProvider().getLoadedChunk(chunk.x + x, chunk.z + z);

                if (nChunk == null || !((IChunkLightingData) nChunk).alfheim$isLightInitialized())
                    return;
            }
        }

        chunk.setLightPopulated(true);
    }

    @Unique
    private static void alfheim$initChunkLighting(final Chunk chunk, final World world) {
        final int xBase = chunk.x << 4;
        final int zBase = chunk.z << 4;

        final BlockPos.PooledMutableBlockPos mutableBlockPos = BlockPos.PooledMutableBlockPos.retain(xBase, 0, zBase);

        if (world.isAreaLoaded(mutableBlockPos.add(-16, 0, -16), mutableBlockPos.add(31, 255, 31), false)) {
            final ExtendedBlockStorage[] extendedBlockStorage = chunk.getBlockStorageArray();

            for (int i = 0; i < extendedBlockStorage.length; ++i) {
                final ExtendedBlockStorage storage = extendedBlockStorage[i];

                if (storage == Chunk.NULL_BLOCK_STORAGE)
                    continue;

                int yBase = i * 16;

                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            mutableBlockPos.setPos(xBase + x, yBase + y, zBase + z);

                            if (LightUtil.getLightValueForState(storage.getData().get(x, y, z), world, mutableBlockPos) > 0)
                                world.checkLightFor(EnumSkyBlock.BLOCK, mutableBlockPos);
                        }
                    }
                }
            }

            if (world.provider.hasSkyLight())
                ((IChunkLightingData) chunk).alfheim$setSkylightUpdatedPublic();

            ((IChunkLightingData) chunk).alfheim$setLightInitialized(true);
        }

        mutableBlockPos.release();
    }

    /**
     * @reason Avoids chunk fetches as much as possible.
     * @author Angeline (@jellysquid), Luna Lage (Desoroxxx)
     */
    @Overwrite
    private void recheckGaps(final boolean onlyOne) {
        if (!world.isAreaLoaded(new BlockPos((x << 4) + 8, 0, (z << 4) + 8), 16))
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
    private void alfheim$relightSkylightColumn(final int x, final int z, final int height1, final int height2) {
        final int yMin = Math.min(height1, height2);
        final int yMax = Math.max(height1, height2) - 1;

        final Chunk chunk = ((Chunk) (Object) this);

        final ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();

        final int xBase = (chunk.x << 4) + x;
        final int zBase = (chunk.z << 4) + z;

        alfheim$scheduleRelightChecksForColumn(EnumSkyBlock.SKY, xBase, zBase, yMin, yMax);

        if (sections[yMin >> 4] == Chunk.NULL_BLOCK_STORAGE && yMin > 0) {
            world.checkLightFor(EnumSkyBlock.SKY, new BlockPos(xBase, yMin - 1, zBase));
        }

        short emptySections = 0;

        for (int sec = yMax >> 4; sec >= yMin >> 4; --sec) {
            if (sections[sec] == Chunk.NULL_BLOCK_STORAGE) {
                emptySections |= (short) (1 << sec);
            }
        }

        if (emptySections != 0) {
            for (final EnumFacing facing : EnumFacing.HORIZONTALS) {
                final int xOffset = facing.getXOffset();
                final int zOffset = facing.getZOffset();

                final boolean neighborColumnExists =
                        (((x + xOffset) | (z + zOffset)) & 16) == 0
                                //Checks whether the position is at the specified border (the 16 bit is set for both 15+1 and 0-1)
                                || world.getChunkProvider().getLoadedChunk(chunk.x + xOffset, chunk.z + zOffset) != null;

                if (neighborColumnExists) {
                    for (int sec = yMax >> 4; sec >= yMin >> 4; --sec) {
                        if ((emptySections & (1 << sec)) != 0)
                            alfheim$scheduleRelightChecksForColumn(EnumSkyBlock.SKY, xBase + xOffset, zBase + zOffset, sec << 4, (sec << 4) + 15);
                    }
                } else {
                    ((IChunkLightingData) chunk).alfheim$initNeighborLightChecks();

                    final EnumFacing.AxisDirection axisDirection = ((facing.getAxis() == EnumFacing.Axis.X ? z : x) & 15) < 8 ? EnumFacing.AxisDirection.NEGATIVE : EnumFacing.AxisDirection.POSITIVE;
                    ((IChunkLightingData) chunk).alfheim$getNeighborLightChecks()[alfheim$getFlagIndex(EnumSkyBlock.SKY, facing, axisDirection, EnumBoundaryFacing.OUT)] |= emptySections;

                    chunk.markDirty();
                }
            }
        }
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

        alfheim$initSkylightForSection(extendedBlockStorage);

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

        for (final EnumFacing facing : ENUM_FACING_HORIZONTAL) {
            final Chunk chunk = slice.getChunkFromWorldCoords(x + facing.getXOffset(), z + facing.getZOffset());

            if (chunk != null)
                max = Math.min(max, chunk.getLowestHeight());
        }

        return max;
    }

    @Unique
    private void alfheim$recheckGapsSkylightNeighborHeight(final WorldChunkSlice slice, final int x, final int z, final int height, final int max) {
        alfheim$checkSkylightNeighborHeight(slice, x, z, max);

        for (final EnumFacing facing : ENUM_FACING_HORIZONTAL)
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

    @Unique
    private static void alfheim$mergeFlags(final EnumSkyBlock lightType, final Chunk inChunk, final Chunk outChunk, final EnumFacing dir, final EnumFacing.AxisDirection axisDirection) {
        final IChunkLightingData outChunkLightingData = (IChunkLightingData) outChunk;

        if (outChunkLightingData.alfheim$getNeighborLightChecks() == null)
            return;

        ((IChunkLightingData) inChunk).alfheim$initNeighborLightChecks();

        final int inIndex = alfheim$getFlagIndex(lightType, dir, axisDirection, EnumBoundaryFacing.IN);
        final int outIndex = alfheim$getFlagIndex(lightType, dir.getOpposite(), axisDirection, EnumBoundaryFacing.OUT);

        ((IChunkLightingData) inChunk).alfheim$getNeighborLightChecks()[inIndex] |= outChunkLightingData.alfheim$getNeighborLightChecks()[outIndex];
        // No need to call Chunk.setModified() since checks are not deleted from outChunk
    }

    @Unique
    private void alfheim$scheduleRelightChecksForBoundary(final Chunk chunk, Chunk nChunk, Chunk sChunk, final EnumSkyBlock lightType, final int xOffset, final int zOffset, final EnumFacing.AxisDirection axisDirection) {
        final IChunkLightingData chunkLightingData = (IChunkLightingData) chunk;

        if (chunkLightingData.alfheim$getNeighborLightChecks() == null)
            return;

        final int flagIndex = alfheim$getFlagIndex(lightType, xOffset, zOffset, axisDirection, EnumBoundaryFacing.IN); // OUT checks from neighbor are already merged

        final int flags = chunkLightingData.alfheim$getNeighborLightChecks()[flagIndex];

        if (flags == 0)
            return;

        if (nChunk == null) {
            nChunk = world.getChunkProvider().getLoadedChunk(chunk.x + xOffset, chunk.z + zOffset);

            if (nChunk == null)
                return;
        }

        if (sChunk == null) {
            sChunk = world.getChunkProvider()
                    .getLoadedChunk(chunk.x + (zOffset != 0 ? axisDirection.getOffset() : 0), chunk.z + (xOffset != 0 ? axisDirection.getOffset() : 0));

            if (sChunk == null)
                return; // Cancel, since the checks in the corner columns require the corner column of sChunk
        }

        final int reverseIndex = alfheim$getFlagIndex(lightType, -xOffset, -zOffset, axisDirection, EnumBoundaryFacing.OUT);

        chunkLightingData.alfheim$getNeighborLightChecks()[flagIndex] = 0;

        final IChunkLightingData nChunkLightingData = (IChunkLightingData) nChunk;

        if (nChunkLightingData.alfheim$getNeighborLightChecks() != null)
            nChunkLightingData.alfheim$getNeighborLightChecks()[reverseIndex] = 0; // Clear only now that it's clear that the checks are processed

        chunk.markDirty();
        nChunk.markDirty();

        // Get the area to check
        // Start in the corner...
        int xMin = chunk.x << 4;
        int zMin = chunk.z << 4;

        // Move to other side of chunk if the direction is positive
        if ((xOffset | zOffset) > 0) {
            xMin += 15 * xOffset;
            zMin += 15 * zOffset;
        }

        // Shift to other half if necessary (shift perpendicular to dir)
        if (axisDirection == EnumFacing.AxisDirection.POSITIVE) {
            xMin += 8 * (zOffset & 1); //x & 1 is same as abs(x) for x=-1,0,1
            zMin += 8 * (xOffset & 1);
        }

        // Get maximal values (shift perpendicular to dir)
        final int xMax = xMin + 7 * (zOffset & 1);
        final int zMax = zMin + 7 * (xOffset & 1);

        for (int y = 0; y < 16; ++y)
            if ((flags & (1 << y)) != 0)
                for (int x = xMin; x <= xMax; ++x)
                    for (int z = zMin; z <= zMax; ++z)
                        alfheim$scheduleRelightChecksForColumn(lightType, x, z, y << 4, (y << 4) + 15);
    }

    @Unique
    private void alfheim$initSkylightForSection(final ExtendedBlockStorage extendedBlockStorage) {
        if (!world.provider.hasSkyLight())
            return;

        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                if (((Chunk) (Object) this).getHeightValue(x, z) > extendedBlockStorage.getYLocation())
                    continue;

                for (int y = 0; y < 16; ++y)
                    extendedBlockStorage.setSkyLight(x, y, z, EnumSkyBlock.SKY.defaultLightValue);
            }
        }
    }

    @Unique
    private void alfheim$scheduleRelightChecksForColumn(final EnumSkyBlock lightType, final int x, final int z, final int yMin, final int yMax) {
        final BlockPos.MutableBlockPos mutableBlockPos = new BlockPos.MutableBlockPos();

        for (int y = yMin; y <= yMax; ++y)
            world.checkLightFor(lightType, mutableBlockPos.setPos(x, y, z));
    }

    @Unique
    private static int alfheim$getFlagIndex(final EnumSkyBlock lightType, final int xOffset, final int zOffset, final EnumFacing.AxisDirection axisDirection, final EnumBoundaryFacing boundaryFacing) {
        return (lightType == EnumSkyBlock.BLOCK ? 0 : 16) | ((xOffset + 1) << 2) | ((zOffset + 1) << 1) | (axisDirection.getOffset() + 1) | boundaryFacing.ordinal();
    }

    @Unique
    private static int alfheim$getFlagIndex(final EnumSkyBlock lightType, final EnumFacing facing, final EnumFacing.AxisDirection axisDirection, final EnumBoundaryFacing boundaryFacing) {
        return alfheim$getFlagIndex(lightType, facing.getXOffset(), facing.getZOffset(), axisDirection, boundaryFacing);
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
    public void alfheim$initNeighborLightChecks() {
        if (alfheim$getNeighborLightChecks() == null)
            alfheim$setNeighborLightChecks(new short[FLAG_COUNT]);
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

