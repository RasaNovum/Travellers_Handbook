package net.rasanovum.travellershandbook;

import eu.midnightdust.lib.config.MidnightConfig;

public class TravellersHandbookConfig extends MidnightConfig {
    @Entry()
    public static volatile boolean loadDefaultEntries = true;

    @Entry()
    public static volatile boolean reduceRuneMotion = false;

    @Entry()
    public static volatile boolean hideLockedCategories = false;

    @Entry(min = 0)
    public static volatile int entryToastDelayTicks = 100;
}
