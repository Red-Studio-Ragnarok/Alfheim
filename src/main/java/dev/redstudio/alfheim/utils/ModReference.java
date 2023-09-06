package dev.redstudio.alfheim.utils;

import dev.redstudio.alfheim.Tags;
import io.redstudioragnarok.redcore.logging.RedLogger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class defines constants for Alfheim.
 * <p>
 * They are automatically updated by RFG on compile time, except for the name as Gradle would remove spaces.
 */
public final class ModReference {

    public static final String ID = Tags.ID;
    public static final String NAME = "Alfheim";
    public static final String VERSION = Tags.VERSION;
    public static final Logger LOG = LogManager.getLogger(NAME);
    public static final RedLogger RED_LOG = new RedLogger(NAME, "https://linkify.cz/AlfheimBugReport", LOG);
}
