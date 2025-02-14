package dev.redstudio.alfheim.lighting;

import dev.redstudio.alfheim.api.IChunkLightingData;
import dev.redstudio.alfheim.utils.DeduplicatedLongQueue;
import dev.redstudio.redcore.math.ClampUtil;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.profiler.Profiler;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.concurrent.locks.ReentrantLock;

import static dev.redstudio.alfheim.ProjectConstants.LOGGER;
import static dev.redstudio.alfheim.ProjectConstants.NAME;

/// @author Luna Lage (Desoroxxx)
/// @author kappa-maintainer
/// @author embeddedt
/// @author Angeline (@jellysquid)
/// @version 2024-11-14
/// @since 1.0
public final class LightingEngine {

    private static final byte MAX_LIGHT_LEVEL = 15;

    private final Thread ownerThread = Thread.currentThread();

    private final World world;

    private final Profiler profiler;

    // Layout of longs: [padding(4)] [y(8)] [x(26)] [z(26)]
    private final DeduplicatedLongQueue[] lightUpdateQueues = new DeduplicatedLongQueue[EnumSkyBlock.values().length];

    // Layout of longs: see above
    private final DeduplicatedLongQueue[] darkeningQueues = new DeduplicatedLongQueue[MAX_LIGHT_LEVEL + 1];
    private final DeduplicatedLongQueue[] brighteningQueues = new DeduplicatedLongQueue[MAX_LIGHT_LEVEL + 1];

    // Layout of longs: [newLight(4)] [pos(60)]
    private final DeduplicatedLongQueue initialBrightenings;
    // Layout of longs: [padding(4)] [pos(60)]
    private final DeduplicatedLongQueue initialDarkenings;

    private boolean updating = false;

    // Layout parameters
    // Length of bit segments
    private static final int
            L_X = 26,
            L_Y = 8,
            L_Z = 26,
            L_L = 4;

    // Bit segment shifts/positions
    private static final int
            S_Z = 0,
            S_X = S_Z + L_Z,
            S_Y = S_X + L_X,
            S_L = S_Y + L_Y;

    // Bit segment masks
    private static final long
            M_X = (1L << L_X) - 1,
            M_Y = (1L << L_Y) - 1,
            M_Z = (1L << L_Z) - 1,
            M_L = (1L << L_L) - 1,
            M_POS = (M_Y << S_Y) | (M_X << S_X) | (M_Z << S_Z);

    // Bit to check whether y had overflow
    private static final long Y_CHECK = 1L << (S_Y + L_Y);

    private static final long[] neighborShifts = new long[6];

    static {
        for (byte i = 0; i < 6; ++i) {
            final Vec3i offset = EnumFacing.VALUES[i].getDirectionVec();
            neighborShifts[i] = ((long) offset.getY() << S_Y) | ((long) offset.getX() << S_X) | ((long) offset.getZ() << S_Z);
        }
    }

    // Mask to extract chunk identifier
    private static final long M_CHUNK = ((M_X >> 4) << (4 + S_X)) | ((M_Z >> 4) << (4 + S_Z));

    // Iteration state data
    // Cache position to avoid allocation of new object each time
    private final MutableBlockPos currentPos = new MutableBlockPos();
    private Chunk currentChunk;
    private long currentChunkIdentifier;
    private long currentData;

    // Cached data about neighboring blocks (of tempPos)
    private boolean isNeighborDataValid = false;

    private final NeighborInfo[] neighborInfos = new NeighborInfo[6];
    private DeduplicatedLongQueue currentQueue;

    private final ReentrantLock lock = new ReentrantLock();

    public LightingEngine(final World world) {
        this.world = world;
        profiler = world.profiler;

        initialBrightenings = new DeduplicatedLongQueue(16384);
        initialDarkenings = new DeduplicatedLongQueue(16384);

        for (int i = 0; i < EnumSkyBlock.values().length; ++i)
            lightUpdateQueues[i] = new DeduplicatedLongQueue(16384);

        for (int i = 0; i < darkeningQueues.length; ++i)
            darkeningQueues[i] = new DeduplicatedLongQueue(16384);

        for (int i = 0; i < brighteningQueues.length; ++i)
            brighteningQueues[i] = new DeduplicatedLongQueue(16384);

        for (int i = 0; i < neighborInfos.length; ++i)
            neighborInfos[i] = new NeighborInfo();
    }

    /// Schedules a light update for the specified light type and position to be processed later by [#processLightUpdatesForType(EnumSkyBlock)]
    public void scheduleLightUpdate(final EnumSkyBlock lightType, final BlockPos pos) {
        lock();

        try {
            scheduleLightUpdate(lightType, encodeWorldCoord(pos));
        } finally {
            lock.unlock();
        }
    }

    /// Schedules a light update for the specified light type and position to be processed later by [#processLightUpdates()]
    private void scheduleLightUpdate(final EnumSkyBlock lightType, final long blockPos) {
        lightUpdateQueues[lightType.ordinal()].enqueue(blockPos);
    }

    /// Calls [#processLightUpdatesForType(EnumSkyBlock)] for both light types
    public void processLightUpdates() {
        profiler.startSection("processSky");

        processLightUpdatesForType(EnumSkyBlock.SKY);

        profiler.endStartSection("processBlock");

        processLightUpdatesForType(EnumSkyBlock.BLOCK);

        profiler.endSection();
    }

    /// Processes light updates of the given light type
    public void processLightUpdatesForType(final EnumSkyBlock lightType) {
        // We only want to perform updates if we're being called from a tick event on the client.
        // There are many locations in the client code that will end up making calls to this method, usually from other threads.
        if (world.isRemote && !isCallingFromMainThread())
            return;

        final DeduplicatedLongQueue queue = lightUpdateQueues[lightType.ordinal()];

        // Quickly check if the queue is empty before we acquire a more expensive lock.
        if (queue.isEmpty())
            return;

        profiler.startSection("process");

        lock();

        try {
            processLightUpdatesForTypeInner(lightType, queue);
        } finally {
            lock.unlock();
        }

        profiler.endSection();
    }

    @SideOnly(Side.CLIENT)
    private boolean isCallingFromMainThread() {
        return Minecraft.getMinecraft().isCallingFromMinecraftThread();
    }

    private void lock() {
        if (lock.tryLock())
            return;

        // If we cannot lock, something has gone wrong... Only one thread should ever acquire the lock.
        // Validate that we're on the right thread immediately, so we can gather information.
        // It is NEVER valid to call World methods from a thread other than the owning thread of the world instance.
        final Thread current = Thread.currentThread();

        if (current != ownerThread) {
            final IllegalAccessException illegalAccessException = new IllegalAccessException(String.format("World is owned by '%s' (ID: %s)," + " but was accessed from thread '%s' (ID: %s)", ownerThread.getName(), ownerThread.getId(), current.getName(), current.getId()));

            LOGGER.warn(
                    "Something (likely another mod) has attempted to modify the world's state from the wrong thread!\n" +
                            "This is *bad practice* and can cause severe issues in your game.\n" +
                            NAME + " has done as best as it can to mitigate this violation, but it may negatively impact performance or introduce stalls.\n" +
                            "In a future release, this violation may result in a hard crash instead of the current soft warning.\n"
                    , illegalAccessException);

        }

        // Wait for the lock to be released. This will likely introduce unwanted stalls, but will mitigate the issue.
        lock.lock();
    }

    private void processLightUpdatesForTypeInner(final EnumSkyBlock lightType, final DeduplicatedLongQueue queue) {
        // Avoid nested calls
        if (updating)
            throw new IllegalStateException("Already processing updates!");

        updating = true;

        currentChunkIdentifier = -1; // Reset chunk cache

        currentQueue = queue;

        if (currentQueue != null)
            currentQueue.newDeduplicationSet();

        profiler.startSection("prepare");

        // Process the queued updates and enqueue them for further processing
        while (nextItem()) {
            if (currentChunk == null)
                continue;

            final byte oldLight = getCursorCachedLight(lightType);
            final byte newLight = calculateNewLightFromCursor(lightType);

            if (oldLight < newLight)
                initialBrightenings.enqueue(((long) newLight << S_L) | currentData); // Don't enqueue directly for brightening to avoid duplicate scheduling
            else if (oldLight > newLight)
                initialDarkenings.enqueue(currentData); // Don't enqueue directly for darkening to avoid duplicate scheduling
        }

        profiler.endStartSection("enqueueBrightening");

        currentQueue = initialBrightenings;

        if (currentQueue != null)
            currentQueue.newDeduplicationSet();

        while (nextItem()) {
            final byte newLight = (byte) (currentData >> S_L & M_L);

            if (newLight > getCursorCachedLight(lightType))
                enqueueBrightening(currentPos, currentData & M_POS, newLight, currentChunk, lightType); // Sets the light to newLight to only schedule once. Clear leading bits of curData for later
        }

        profiler.endStartSection("enqueueDarkening");

        currentQueue = initialDarkenings;

        if (currentQueue != null)
            currentQueue.newDeduplicationSet();

        while (nextItem()) {
            final byte oldLight = getCursorCachedLight(lightType);

            if (oldLight != 0)
                enqueueDarkening(currentPos, currentData, oldLight, currentChunk, lightType); // Sets the light to zero to only schedule once
        }

        profiler.endStartSection("process");

        // Iterate through enqueued updates (brightening and darkening in parallel) from brightest to darkest so that we only need to iterate once
        for (byte currentLight = MAX_LIGHT_LEVEL; currentLight >= 0; --currentLight) {
            currentQueue = darkeningQueues[currentLight];

            if (currentQueue != null)
                currentQueue.newDeduplicationSet();

            while (nextItem()) {
                // Don't darken if we got brighter due to some other change
                if (getCursorCachedLight(lightType) >= currentLight)
                    continue;

                final IBlockState blockState = currentChunk.getBlockState(currentPos);
                final byte luminosity = getCursorLuminosity(blockState, lightType);
                final byte opacity; // If luminosity is high enough, opacity is irrelevant

                if (luminosity >= MAX_LIGHT_LEVEL - 1)
                    opacity = 1;
                else
                    opacity = getPosOpacity(currentPos, blockState);

                // Only darken neighbors if we indeed became darker
                if (calculateNewLightFromCursor(luminosity, opacity, lightType) < currentLight) {
                    // Need to calculate new light value from neighbors IGNORING neighbors which are scheduled for darkening
                    byte newLight = luminosity;

                    fetchNeighborDataFromCursor(lightType);

                    for (final NeighborInfo neighborInfo : neighborInfos) {
                        final Chunk neighborChunk = neighborInfo.chunk;

                        if (neighborChunk == null)
                            continue;

                        final byte neighborLight = neighborInfo.light;

                        if (neighborLight == 0)
                            continue;

                        final MutableBlockPos neighborPos = neighborInfo.mutableBlockPos;

                        if (currentLight - getPosOpacity(neighborPos, neighborChunk.getBlockState(neighborPos)) >= neighborLight) /*Schedule neighbor for darkening if we possibly light it*/ {
                            enqueueDarkening(neighborPos, neighborInfo.key, neighborLight, neighborChunk, lightType);
                        } else /*Only use for new light calculation if not*/ {
                            // If we can't darken the neighbor, no one else can (because of processing order) -> safe to let us be illuminated by it
                            newLight = (byte) Math.max(newLight, neighborLight - opacity);
                        }
                    }

                    // Schedule brightening since light level was set to 0
                    enqueueBrighteningFromCursor(newLight, lightType);
                } else /*We didn't become darker, so we need to re-set our initial light value (was set to zero) and notify neighbors*/ {
                    enqueueBrighteningFromCursor(currentLight, lightType); // Do not spread to neighbors immediately to avoid scheduling multiple times
                }
            }

            currentQueue = brighteningQueues[currentLight];

            if (currentQueue != null)
                currentQueue.newDeduplicationSet();

            while (nextItem()) {
                final byte oldLight = getCursorCachedLight(lightType);

                // Only process this if nothing else has happened at this position since scheduling
                if (oldLight == currentLight) {
                    world.notifyLightSet(currentPos);

                    if (currentLight > 1)
                        spreadLightFromCursor(currentLight, lightType);
                }
            }
        }

        profiler.endSection();

        updating = false;
    }

    /// Gets data for neighbors of [#currentPos] and saves the results into neighbor state data members.
    /// If a neighbor can't be accessed/doesn't exist, the corresponding entry in neighborChunks is null - others are not reset
    private void fetchNeighborDataFromCursor(final EnumSkyBlock lightType) {
        // Only update if curPos was changed
        if (isNeighborDataValid)
            return;

        isNeighborDataValid = true;

        for (int i = 0; i < neighborInfos.length; ++i) {
            final NeighborInfo neighborInfo = neighborInfos[i];
            final long neighborLongPos = neighborInfo.key = currentData + neighborShifts[i];

            if ((neighborLongPos & Y_CHECK) != 0) {
                neighborInfo.chunk = null;
                continue;
            }

            final MutableBlockPos neighborPos = decodeWorldCoord(neighborInfo.mutableBlockPos, neighborLongPos);

            final Chunk neighborChunk;

            if ((neighborLongPos & M_CHUNK) == currentChunkIdentifier)
                neighborChunk = neighborInfo.chunk = currentChunk;
            else
                neighborChunk = neighborInfo.chunk = getChunk(neighborPos);

            if (neighborChunk != null) {
                final ExtendedBlockStorage neighborSection = neighborChunk.getBlockStorageArray()[neighborPos.getY() >> 4];

                neighborInfo.light = getCachedLightFor(neighborChunk, neighborSection, neighborPos, lightType);
            }
        }
    }

    private static byte getCachedLightFor(final Chunk chunk, final ExtendedBlockStorage storage, final BlockPos blockPos, final EnumSkyBlock type) {
        final int x = blockPos.getX() & 15;
        final int y = blockPos.getY();
        final int z = blockPos.getZ() & 15;

        if (storage == Chunk.NULL_BLOCK_STORAGE)
            return type == EnumSkyBlock.SKY && chunk.canSeeSky(blockPos) ? (byte) type.defaultLightValue : 0;
        else if (type == EnumSkyBlock.SKY)
            return chunk.getWorld().provider.hasSkyLight() ? (byte) storage.getSkyLight(x, y & 15, z) : 0;
        else
            return type == EnumSkyBlock.BLOCK ? (byte) storage.getBlockLight(x, y & 15, z) : (byte) type.defaultLightValue;
    }

    private byte calculateNewLightFromCursor(final EnumSkyBlock lightType) {
        final IBlockState blockState = currentChunk.getBlockState(currentPos);

        final byte luminosity = getCursorLuminosity(blockState, lightType);
        final byte opacity;

        if (luminosity >= MAX_LIGHT_LEVEL - 1)
            opacity = 1;
        else
            opacity = getPosOpacity(currentPos, blockState);

        return calculateNewLightFromCursor(luminosity, opacity, lightType);
    }

    private byte calculateNewLightFromCursor(final byte luminosity, final byte opacity, final EnumSkyBlock lightType) {
        if (luminosity >= MAX_LIGHT_LEVEL - opacity)
            return luminosity;

        byte newLight = luminosity;

        fetchNeighborDataFromCursor(lightType);

        for (final NeighborInfo neighborInfo : neighborInfos) {
            if (neighborInfo.chunk == null)
                continue;

            newLight = (byte) Math.max(neighborInfo.light - opacity, newLight);
        }

        return newLight;
    }

    private void spreadLightFromCursor(final byte currentLight, final EnumSkyBlock lightType) {
        fetchNeighborDataFromCursor(lightType);

        for (final NeighborInfo neighborInfo : neighborInfos) {
            final Chunk neighborChunk = neighborInfo.chunk;

            if (neighborChunk == null || currentLight < neighborInfo.light)
                continue;

            final BlockPos neighborBlockPos = neighborInfo.mutableBlockPos;

            final byte newLight = (byte) (currentLight - getPosOpacity(neighborBlockPos, neighborChunk.getBlockState(neighborBlockPos)));

            if (newLight > neighborInfo.light)
                enqueueBrightening(neighborBlockPos, neighborInfo.key, newLight, neighborChunk, lightType);
        }
    }

    private void enqueueBrighteningFromCursor(final byte newLight, final EnumSkyBlock lightType) {
        enqueueBrightening(currentPos, currentData, newLight, currentChunk, lightType);
    }

    /// Enqueues the blockPos for brightening and sets its light value to newLight
    private void enqueueBrightening(final BlockPos blockPos, final long longPos, final byte newLight, final Chunk chunk, final EnumSkyBlock lightType) {
        brighteningQueues[newLight].enqueue(longPos);

        chunk.setLightFor(lightType, blockPos, newLight);
    }

    /// Enqueues the blockPos for darkening and sets its light value to 0
    private void enqueueDarkening(final BlockPos blockPos, final long longPos, final byte oldLight, final Chunk chunk, final EnumSkyBlock lightType) {
        darkeningQueues[oldLight].enqueue(longPos);

        chunk.setLightFor(lightType, blockPos, 0);
    }

    private static MutableBlockPos decodeWorldCoord(final MutableBlockPos mutableBlockPos, final long longPos) {
        return mutableBlockPos.setPos((int) (longPos >> S_X & M_X) - (1 << L_X - 1), (int) (longPos >> S_Y & M_Y), (int) (longPos >> S_Z & M_Z) - (1 << L_Z - 1));
    }

    private static long encodeWorldCoord(final BlockPos pos) {
        return ((long) pos.getY() << S_Y) | ((long) pos.getX() + (1 << L_X - 1) << S_X) | ((long) pos.getZ() + (1 << L_Z - 1) << S_Z);
    }

    /// Polls a new item from [#currentQueue] and fills in state data members
    ///
    /// @return If there was an item to poll
    private boolean nextItem() {
        if (currentQueue.isEmpty()) {
            currentQueue = null;

            return false;
        }

        currentData = currentQueue.dequeue();
        isNeighborDataValid = false;

        decodeWorldCoord(currentPos, currentData);

        final long chunkIdentifier = currentData & M_CHUNK;

        if (currentChunkIdentifier != chunkIdentifier) {
            currentChunk = getChunk(currentPos);
            currentChunkIdentifier = chunkIdentifier;
        }

        return true;
    }

    private byte getCursorCachedLight(final EnumSkyBlock lightType) {
        return ((IChunkLightingData) currentChunk).alfheim$getCachedLightFor(lightType, currentPos);
    }

    /// Calculates the luminosity for [#currentPos], taking into account the light type
    private byte getCursorLuminosity(final IBlockState state, final EnumSkyBlock lightType) {
        if (lightType == EnumSkyBlock.SKY) {
            if (currentChunk.canSeeSky(currentPos))
                return (byte) EnumSkyBlock.SKY.defaultLightValue;
            else
                return 0;
        }

        return (byte) ClampUtil.clampMinFirst(LightUtil.getLightValueForState(state, world, currentPos), 0, MAX_LIGHT_LEVEL);
    }

    private byte getPosOpacity(final BlockPos blockPos, final IBlockState blockState) {
        return (byte) ClampUtil.clampMinFirst(blockState.getLightOpacity(world, blockPos), 1, MAX_LIGHT_LEVEL);
    }

    private Chunk getChunk(final BlockPos blockPos) {
        return world.getChunkProvider().getLoadedChunk(blockPos.getX() >> 4, blockPos.getZ() >> 4);
    }

    private static final class NeighborInfo {

        public final MutableBlockPos mutableBlockPos = new MutableBlockPos();

        public Chunk chunk;

        public byte light;

        public long key;
    }
}

