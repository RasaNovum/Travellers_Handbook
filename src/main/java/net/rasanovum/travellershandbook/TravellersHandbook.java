package net.rasanovum.travellershandbook;

import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TravellersHandbook {
    public static final String MOD_ID = "travellers_handbook";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private TravellersHandbook() {
    }

    public static ResourceLocation id(String path) {
        //? if <1.21 {
        /*return new ResourceLocation(MOD_ID, path);
        *///?} else {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
        //?}
    }

    public static ResourceLocation parseId(String id) {
        //? if <1.21 {
        /*return new ResourceLocation(id);
        *///?} else {
        return ResourceLocation.parse(id);
        //?}
    }

    public static void initialize() {
        MidnightConfig.init(MOD_ID, TravellersHandbookConfig.class);
        LOGGER.info("I used to be a traveller like you, until I took a handbook to the knee.");
    }
}
