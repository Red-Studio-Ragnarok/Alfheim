package io.redstudioragnarok.alfheim.lighting;

import io.redstudioragnarok.alfheim.api.IChunkLightingData;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

import static io.redstudioragnarok.alfheim.utils.ModReference.LOG;

/**
 * @author Luna Lage (Desoroxxx)
 * @author kappa-maintainer
 * @author embeddedt
 * @author Angeline (@jellysquid)
 * @since 0.1
 */
public final class LightingHooks {
    private static final EnumSkyBlock[] ENUM_SKY_BLOCK_VALUES = EnumSkyBlock.values();

    private static final EnumFacing.AxisDirection[] ENUM_AXIS_DIRECTION_VALUES = EnumFacing.AxisDirection.values();

    private static final int FLAG_COUNT = 32; //2 light types * 4 directions * 2 halves * (inwards + outwards)

    public static void relightSkylightColumn(final World world, final Chunk chunk, final int x, final int z, final int height1, final int height2) {
        final int yMin = Math.min(height1, height2);
        final int yMax = Math.max(height1, height2) - 1;

        final ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();

        final int xBase = (chunk.x << 4) + x;
        final int zBase = (chunk.z << 4) + z;

        scheduleRelightChecksForColumn(world, EnumSkyBlock.SKY, xBase, zBase, yMin, yMax);

        if (sections[yMin >> 4] == Chunk.NULL_BLOCK_STORAGE && yMin > 0) {
            world.checkLightFor(EnumSkyBlock.SKY, new BlockPos(xBase, yMin - 1, zBase));
        }

        short emptySections = 0;

        for (int sec = yMax >> 4; sec >= yMin >> 4; --sec) {
            if (sections[sec] == Chunk.NULL_BLOCK_STORAGE) {
                emptySections |= 1 << sec;
            }
        }

        if (emptySections != 0) {
            for (final EnumFacing dir : EnumFacing.HORIZONTALS) {
                final int xOffset = dir.getXOffset();
                final int zOffset = dir.getZOffset();

                final boolean neighborColumnExists =
                        (((x + xOffset) | (z + zOffset)) & 16) == 0
                                //Checks whether the position is at the specified border (the 16 bit is set for both 15+1 and 0-1)
                                || world.getChunkProvider().getLoadedChunk(chunk.x + xOffset, chunk.z + zOffset) != null;

                if (neighborColumnExists) {
                    for (int sec = yMax >> 4; sec >= yMin >> 4; --sec) {
                        if ((emptySections & (1 << sec)) != 0) {
                            scheduleRelightChecksForColumn(world, EnumSkyBlock.SKY, xBase + xOffset, zBase + zOffset, sec << 4, (sec << 4) + 15);
                        }
                    }
                } else {
                    flagChunkBoundaryForUpdate(chunk, emptySections, EnumSkyBlock.SKY, dir, getAxisDirection(dir, x, z), EnumBoundaryFacing.OUT);
                }
            }
        }
    }

    public static void scheduleRelightChecksForArea(final World world, final EnumSkyBlock lightType, final int xMin, final int yMin, final int zMin,
                                                    final int xMax, final int yMax, final int zMax) {
        for (int x = xMin; x <= xMax; ++x) {
            for (int z = zMin; z <= zMax; ++z) {
                scheduleRelightChecksForColumn(world, lightType, x, z, yMin, yMax);
            }
        }
    }

    private static void scheduleRelightChecksForColumn(final World world, final EnumSkyBlock lightType, final int x, final int z, final int yMin, final int yMax) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int y = yMin; y <= yMax; ++y) {
            world.checkLightFor(lightType, pos.setPos(x, y, z));
        }
    }

    public enum EnumBoundaryFacing {
        IN,
        OUT
    }

    public static void flagChunkBoundaryForUpdate(final Chunk chunk, final short sectionMask, final EnumSkyBlock lightType, final EnumFacing dir,
                                                  final EnumFacing.AxisDirection axisDirection, final EnumBoundaryFacing boundaryFacing) {
        initNeighborLightChecks(chunk);
        ((IChunkLightingData) chunk).getNeighborLightChecks()[getFlagIndex(lightType, dir, axisDirection, boundaryFacing)] |= sectionMask;
        chunk.markDirty();
    }

    public static int getFlagIndex(final EnumSkyBlock lightType, final int xOffset, final int zOffset, final EnumFacing.AxisDirection axisDirection,
                                   final EnumBoundaryFacing boundaryFacing) {
        return (lightType == EnumSkyBlock.BLOCK ? 0 : 16) | ((xOffset + 1) << 2) | ((zOffset + 1) << 1) | (axisDirection.getOffset() + 1) | boundaryFacing
                .ordinal();
    }

    public static int getFlagIndex(final EnumSkyBlock lightType, final EnumFacing dir, final EnumFacing.AxisDirection axisDirection,
                                   final EnumBoundaryFacing boundaryFacing) {
        return getFlagIndex(lightType, dir.getXOffset(), dir.getZOffset(), axisDirection, boundaryFacing);
    }

    private static EnumFacing.AxisDirection getAxisDirection(final EnumFacing dir, final int x, final int z) {
        return ((dir.getAxis() == EnumFacing.Axis.X ? z : x) & 15) < 8 ? EnumFacing.AxisDirection.NEGATIVE : EnumFacing.AxisDirection.POSITIVE;
    }

    public static void scheduleRelightChecksForChunkBoundaries(final World world, final Chunk chunk) {
        for (final EnumFacing dir : EnumFacing.HORIZONTALS) {
            final int xOffset = dir.getXOffset();
            final int zOffset = dir.getZOffset();

            final Chunk nChunk = world.getChunkProvider().getLoadedChunk(chunk.x + xOffset, chunk.z + zOffset);

            if (nChunk == null) {
                continue;
            }

            for (final EnumSkyBlock lightType : ENUM_SKY_BLOCK_VALUES) {
                for (final EnumFacing.AxisDirection axisDir : ENUM_AXIS_DIRECTION_VALUES) {
                    //Merge flags upon loading of a chunk. This ensures that all flags are always already on the IN boundary below
                    mergeFlags(lightType, chunk, nChunk, dir, axisDir);
                    mergeFlags(lightType, nChunk, chunk, dir.getOpposite(), axisDir);

                    //Check everything that might have been canceled due to this chunk not being loaded.
                    //Also, pass in chunks if already known
                    //The boundary to the neighbor chunk (both ways)
                    scheduleRelightChecksForBoundary(world, chunk, nChunk, null, lightType, xOffset, zOffset, axisDir);
                    scheduleRelightChecksForBoundary(world, nChunk, chunk, null, lightType, -xOffset, -zOffset, axisDir);
                    //The boundary to the diagonal neighbor (since the checks in that chunk were aborted if this chunk wasn't loaded, see scheduleRelightChecksForBoundary)
                    scheduleRelightChecksForBoundary(world, nChunk, null, chunk, lightType, (zOffset != 0 ? axisDir.getOffset() : 0),
                            (xOffset != 0 ? axisDir.getOffset() : 0), dir.getAxisDirection() == EnumFacing.AxisDirection.POSITIVE ?
                                    EnumFacing.AxisDirection.NEGATIVE :
                                    EnumFacing.AxisDirection.POSITIVE);
                }
            }
        }
    }

    private static void mergeFlags(final EnumSkyBlock lightType, final Chunk inChunk, final Chunk outChunk, final EnumFacing dir,
                                   final EnumFacing.AxisDirection axisDir) {
        IChunkLightingData outChunkLightingData = (IChunkLightingData) outChunk;

        if (outChunkLightingData.getNeighborLightChecks() == null) {
            return;
        }

        IChunkLightingData inChunkLightingData = (IChunkLightingData) inChunk;

        initNeighborLightChecks(inChunk);

        final int inIndex = getFlagIndex(lightType, dir, axisDir, EnumBoundaryFacing.IN);
        final int outIndex = getFlagIndex(lightType, dir.getOpposite(), axisDir, EnumBoundaryFacing.OUT);

        inChunkLightingData.getNeighborLightChecks()[inIndex] |= outChunkLightingData.getNeighborLightChecks()[outIndex];
        //no need to call Chunk.setModified() since checks are not deleted from outChunk
    }

    private static void scheduleRelightChecksForBoundary(final World world, final Chunk chunk, Chunk nChunk, Chunk sChunk, final EnumSkyBlock lightType,
                                                         final int xOffset, final int zOffset, final EnumFacing.AxisDirection axisDir) {
        IChunkLightingData chunkLightingData = (IChunkLightingData) chunk;

        if (chunkLightingData.getNeighborLightChecks() == null) {
            return;
        }

        final int flagIndex = getFlagIndex(lightType, xOffset, zOffset, axisDir, EnumBoundaryFacing.IN); //OUT checks from neighbor are already merged

        final int flags = chunkLightingData.getNeighborLightChecks()[flagIndex];

        if (flags == 0) {
            return;
        }

        if (nChunk == null) {
            nChunk = world.getChunkProvider().getLoadedChunk(chunk.x + xOffset, chunk.z + zOffset);

            if (nChunk == null) {
                return;
            }
        }

        if (sChunk == null) {
            sChunk = world.getChunkProvider()
                    .getLoadedChunk(chunk.x + (zOffset != 0 ? axisDir.getOffset() : 0), chunk.z + (xOffset != 0 ? axisDir.getOffset() : 0));

            if (sChunk == null) {
                return; //Cancel, since the checks in the corner columns require the corner column of sChunk
            }
        }

        final int reverseIndex = getFlagIndex(lightType, -xOffset, -zOffset, axisDir, EnumBoundaryFacing.OUT);

        chunkLightingData.getNeighborLightChecks()[flagIndex] = 0;

        IChunkLightingData nChunkLightingData = (IChunkLightingData) nChunk;

        if (nChunkLightingData.getNeighborLightChecks() != null) {
            nChunkLightingData.getNeighborLightChecks()[reverseIndex] = 0; //Clear only now that it's clear that the checks are processed
        }

        chunk.markDirty();
        nChunk.markDirty();

        //Get the area to check
        //Start in the corner...
        int xMin = chunk.x << 4;
        int zMin = chunk.z << 4;

        //move to other side of chunk if the direction is positive
        if ((xOffset | zOffset) > 0) {
            xMin += 15 * xOffset;
            zMin += 15 * zOffset;
        }

        //shift to other half if necessary (shift perpendicular to dir)
        if (axisDir == EnumFacing.AxisDirection.POSITIVE) {
            xMin += 8 * (zOffset & 1); //x & 1 is same as abs(x) for x=-1,0,1
            zMin += 8 * (xOffset & 1);
        }

        //get maximal values (shift perpendicular to dir)
        final int xMax = xMin + 7 * (zOffset & 1);
        final int zMax = zMin + 7 * (xOffset & 1);

        for (int y = 0; y < 16; ++y) {
            if ((flags & (1 << y)) != 0) {
                scheduleRelightChecksForArea(world, lightType, xMin, y << 4, zMin, xMax, (y << 4) + 15, zMax);
            }
        }
    }

    public static void initNeighborLightChecks(final Chunk chunk) {
        IChunkLightingData lightingData = (IChunkLightingData) chunk;

        if (lightingData.getNeighborLightChecks() == null) {
            lightingData.setNeighborLightChecks(new short[FLAG_COUNT]);
        }
    }

    public static final String NEIGHBOR_LIGHT_CHECKS_KEY = "NeighborLightChecks";

    public static void writeNeighborLightChecksToNBT(final Chunk chunk, final NBTTagCompound nbt) {
        short[] neighborLightChecks = ((IChunkLightingData) chunk).getNeighborLightChecks();

        if (neighborLightChecks == null) {
            return;
        }

        boolean empty = true;

        final NBTTagList list = new NBTTagList();

        for (final short flags : neighborLightChecks) {
            list.appendTag(new NBTTagShort(flags));

            if (flags != 0) {
                empty = false;
            }
        }

        if (!empty) {
            nbt.setTag(NEIGHBOR_LIGHT_CHECKS_KEY, list);
        }
    }

    public static void readNeighborLightChecksFromNBT(final Chunk chunk, final NBTTagCompound nbt) {
        if (nbt.hasKey(NEIGHBOR_LIGHT_CHECKS_KEY, 9)) {
            final NBTTagList list = nbt.getTagList(NEIGHBOR_LIGHT_CHECKS_KEY, 2);

            if (list.tagCount() == FLAG_COUNT) {
                initNeighborLightChecks(chunk);

                short[] neighborLightChecks = ((IChunkLightingData) chunk).getNeighborLightChecks();

                for (int i = 0; i < FLAG_COUNT; ++i) {
                    neighborLightChecks[i] = ((NBTTagShort) list.get(i)).getShort();
                }
            } else {
                LOG.warn("Chunk field {} had invalid length, ignoring it (chunk coordinates: {} {})", NEIGHBOR_LIGHT_CHECKS_KEY, chunk.x, chunk.z);
            }
        }
    }

    public static void initChunkLighting(final Chunk chunk, final World world) {
        final int xBase = chunk.x << 4;
        final int zBase = chunk.z << 4;

        final BlockPos.PooledMutableBlockPos mutableBlockPos = BlockPos.PooledMutableBlockPos.retain(xBase, 0, zBase);

        if (world.isAreaLoaded(mutableBlockPos.add(-16, 0, -16), mutableBlockPos.add(31, 255, 31), false)) {
            final ExtendedBlockStorage[] extendedBlockStorage = chunk.getBlockStorageArray();

            for (int j = 0; j < extendedBlockStorage.length; ++j) {
                final ExtendedBlockStorage storage = extendedBlockStorage[j];

                if (storage == Chunk.NULL_BLOCK_STORAGE) {
                    continue;
                }

                int yBase = j * 16;

                for (int y = 0; y < 16; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            mutableBlockPos.setPos(xBase + x, yBase + y, zBase + z);

                            if (LightingEngineHelpers.getLightValueForState(storage.getData().get(x, y, z), world, mutableBlockPos) > 0)
                                world.checkLightFor(EnumSkyBlock.BLOCK, mutableBlockPos);
                        }
                    }
                }
            }

            if (world.provider.hasSkyLight()) {
                ((IChunkLightingData) chunk).setSkylightUpdatedPublic();
            }

            ((IChunkLightingData) chunk).setLightInitialized(true);
        }

        mutableBlockPos.release();
    }

    public static void checkChunkLighting(final Chunk chunk, final World world) {
        if (!((IChunkLightingData) chunk).isLightInitialized()) {
            initChunkLighting(chunk, world);
        }

        for (int x = -1; x <= 1; ++x) {
            for (int z = -1; z <= 1; ++z) {
                if (x != 0 || z != 0) {
                    Chunk nChunk = world.getChunkProvider().getLoadedChunk(chunk.x + x, chunk.z + z);

                    if (nChunk == null || !((IChunkLightingData) nChunk).isLightInitialized()) {
                        return;
                    }
                }
            }
        }

        chunk.setLightPopulated(true);
    }

    public static void initSkylightForSection(final World world, final Chunk chunk, final ExtendedBlockStorage section) {
        if (world.provider.hasSkyLight()) {
            for (int x = 0; x < 16; ++x) {
                for (int z = 0; z < 16; ++z) {
                    if (chunk.getHeightValue(x, z) <= section.getYLocation()) {
                        for (int y = 0; y < 16; ++y) {
                            section.setSkyLight(x, y, z, EnumSkyBlock.SKY.defaultLightValue);
                        }
                    }
                }
            }
        }
    }
}
