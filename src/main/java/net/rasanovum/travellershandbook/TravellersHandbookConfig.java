package net.rasanovum.travellershandbook;

import eu.midnightdust.lib.config.MidnightConfig;

public class TravellersHandbookConfig extends MidnightConfig {
    @Entry(category = "entries")
    public static volatile boolean loadDefaultEntries = true;

    @Entry(category = "accessibility")
    public static volatile boolean reduceRuneMotion = false;

    @Entry(category = "notifications", min = 0)
    public static volatile int entryToastDelayTicks = 100;
}
