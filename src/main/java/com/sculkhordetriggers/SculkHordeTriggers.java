package com.sculkhordetriggers;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(SculkHordeTriggers.MODID)
public class SculkHordeTriggers {
    public static final String MODID = "sculkhordetriggers";
    private static final Logger LOGGER = LogManager.getLogger();

    public SculkHordeTriggers() {
        // Register the ModEvents class to handle all events
        MinecraftForge.EVENT_BUS.register(new ModEvents());
    }

}
