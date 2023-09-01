package io.redstudioragnarok.alfheim.api;

/**
 * @author Luna Lage (Desoroxxx)
 * @author Angeline (@jellysquid)
 * @since 0.1
 */
public interface IChunkLightingData {

    short[] alfheim$getNeighborLightChecks();

    void alfheim$setNeighborLightChecks(final short[] data);

    boolean alfheim$isLightInitialized();

    void alfheim$setLightInitialized(final boolean lightInitialized);

    void alfheim$setSkylightUpdatedPublic();
}
