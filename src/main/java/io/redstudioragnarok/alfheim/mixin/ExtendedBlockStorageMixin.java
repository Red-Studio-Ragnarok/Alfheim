package io.redstudioragnarok.alfheim.mixin;

import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * @author Luna Lage (Desoroxxx)
 * @author Angeline (@jellysquid)
 * @since 0.1
 */
@Mixin(ExtendedBlockStorage.class)
public abstract class ExtendedBlockStorageMixin {

    @Shadow private int blockRefCount;

    @Shadow private NibbleArray blockLight;
    @Shadow private NibbleArray skyLight;

    @Unique private int alfheim$lightRefCount = -1;

    /**
     * @reason Reset lightRefCount on call
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    public void setSkyLight(final int x, final int y, final int z, final int value) {
        skyLight.set(x, y, z, value);

        alfheim$lightRefCount = -1;
    }

    /**
     * @reason Reset lightRefCount on call
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    public void setBlockLight(final int x, final int y, final int z, final int value) {
        blockLight.set(x, y, z, value);

        alfheim$lightRefCount = -1;
    }

    /**
     * @reason Reset lightRefCount on call
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    public void setBlockLight(final NibbleArray array) {
        blockLight = array;

        alfheim$lightRefCount = -1;
    }

    /**
     * @reason Reset lightRefCount on call
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    public void setSkyLight(final NibbleArray array) {
        skyLight = array;

        alfheim$lightRefCount = -1;
    }

    /**
     * @reason Send light data to clients when lighting is non-trivial
     * @author Angeline (@jellysquid)
     */
    @Overwrite
    public boolean isEmpty() {
        if (blockRefCount != 0)
            return false;

        // -1 indicates the lightRefCount needs to be re-calculated
        if (alfheim$lightRefCount == -1) {
            if (alfheim$checkLightArrayEqual(skyLight, (byte) 255) && alfheim$checkLightArrayEqual(blockLight, (byte) 0))
                alfheim$lightRefCount = 0; // Lighting is trivial, don't send to clients
            else
                alfheim$lightRefCount = 1; // Lighting is not trivial, send to clients
        }

        return alfheim$lightRefCount == 0;
    }

    /**
     * Check light array equality
     *
     * @author Angeline (@jellysquid)
     * @author Luna Lage (Desoroxxx)
     */
    @Unique
    private boolean alfheim$checkLightArrayEqual(final NibbleArray storage, final byte targetValue) {
        if (storage == null)
            return true;

        for (final byte currentByte : storage.getData())
            if (currentByte != targetValue)
                return false;

        return true;
    }
}
