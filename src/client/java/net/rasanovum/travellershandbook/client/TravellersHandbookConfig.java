package net.rasanovum.travellershandbook.client;

import eu.midnightdust.lib.config.MidnightConfig;

public class TravellersHandbookConfig extends MidnightConfig {
    @Entry(category = "accessibility")
    public static volatile boolean reduceRuneMotion = false;
}
