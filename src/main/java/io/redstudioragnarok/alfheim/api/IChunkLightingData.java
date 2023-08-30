package io.redstudioragnarok.alfheim.api;

/**
 * @author Luna Lage (Desoroxxx)
 * @author Angeline (@jellysquid)
 * @since 0.1
 */
public interface IChunkLightingData {

    short[] getNeighborLightChecks();

    void setNeighborLightChecks(short[] data);

    boolean isLightInitialized();

    void setLightInitialized(boolean val);

    void setSkylightUpdatedPublic();
}
