package io.redstudioragnarok.alfheim.utils;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

/**
 * @author Luna Lage (Desoroxxx)
 * @author Angeline (@jellysquid)
 * @since 0.1
 */
public class WorldChunkSlice {

    private static final int DIAMETER = 5;

    private final int x, z;

    private final Chunk[] chunks;

    public WorldChunkSlice(final World world, final int x, final int z) {
        chunks = new Chunk[DIAMETER * DIAMETER];

        final int radius = DIAMETER / 2;

        for (int xDiff = -radius; xDiff <= radius; xDiff++)
            for (int zDiff = -radius; zDiff <= radius; zDiff++)
                chunks[((xDiff + radius) * DIAMETER) + (zDiff + radius)] = world.getChunkProvider().getLoadedChunk(x + xDiff, z + zDiff);

        this.x = x - radius;
        this.z = z - radius;
    }

    public Chunk getChunk(final int x, final int z) {
        return chunks[(x * DIAMETER) + z];
    }

    public Chunk getChunkFromWorldCoords(final int x, final int z) {
        return getChunk((x >> 4) - this.x, (z >> 4) - this.z);
    }

    public boolean isLoaded(final int x, final int z, final int radius) {
        return isLoaded(x - radius, z - radius, x + radius, z + radius);
    }

    public boolean isLoaded(int xStart, int zStart, int xEnd, int zEnd) {
        xStart = (xStart >> 4) - x;
        zStart = (zStart >> 4) - z;
        xEnd = (xEnd >> 4) - x;
        zEnd = (zEnd >> 4) - z;

        for (int i = xStart; i <= xEnd; ++i) {
            for (int j = zStart; j <= zEnd; ++j) {
                if (getChunk(i, j) == null) {
                    return false;
                }
            }
        }

        return true;
    }
}
