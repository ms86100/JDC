package com.avionics_systems.cluster.constants;

/**
 * Shared constant strings for configurable property keys and
 * well-known category values used across all micro-services.
 *
 * <p>Centralising these literals avoids silent drift when one
 * service changes a key while another keeps the old spelling.</p>
 */
public final class MasterDataConstants {

    private MasterDataConstants() {
        // utility class — prevent instantiation
    }

    /** Common prefix for application-level default configuration properties. */
    public static final String CONFIG_PREFIX = "app.defaults";

    /** Status category value representing completed / done work. */
    public static final String STATUS_DONE_CATEGORY = "DONE";

    /** Status category value representing active / in-progress work. */
    public static final String STATUS_IN_PROGRESS_CATEGORY = "IN_PROGRESS";

    /** Status category value representing work not yet started. */
    public static final String STATUS_TODO_CATEGORY = "TODO";
}
