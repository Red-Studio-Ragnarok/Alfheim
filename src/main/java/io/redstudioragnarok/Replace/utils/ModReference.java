package io.redstudioragnarok.Replace.utils;

import io.redstudioragnarok.redcore.logging.RedLogger;
import io.redstudioragnarok.Replace.Tags;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;

/**
 * This class defines constants for Replace.
 * <p>
 * They are automatically updated by RFG on compile time, except for the name as Gradle would remove spaces.
 */
public class ModReference {

    public static final String ID = Tags.ID;
    public static final String NAME = "Replace";
    public static final String VERSION = Tags.VERSION;
    public static final Logger LOG = LogManager.getLogger(NAME);
    public static final String NEW_ISSUE_URL = "Replace";

    public static RedLogger RED_LOG;

    static {
        try {
            RED_LOG = new RedLogger(NAME, new URI(NEW_ISSUE_URL), LOG);
        } catch (Exception ignored) {
        }
    }
}
