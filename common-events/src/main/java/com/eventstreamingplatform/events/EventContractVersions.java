package com.eventstreamingplatform.events;

/**
 * Version information for the shared order-event contract.
 */
public final class EventContractVersions {

    public static final int CURRENT = 1;

    private EventContractVersions() {
    }

    /**
     * Checks whether the supplied event version is supported by this module.
     *
     * @param version event contract version
     * @return {@code true} when the version is supported
     */
    public static boolean isSupported(int version) {
        return version == CURRENT;
    }
}
