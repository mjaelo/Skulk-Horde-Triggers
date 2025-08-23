package com.sculkhordetriggers;

import com.mojang.logging.LogUtils;
import com.sculkhordetriggers.config.ModConfig;
import com.sculkhordetriggers.persistence.ActionPersistence;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(SculkHordeTriggers.MODID)
public class SculkHordeTriggers {
    public static final String MODID = "sculkhordetriggers";
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private static ActionPersistence actionPersistence;
    private static SculkHordeTriggers instance;

    public static ActionPersistence getActionPersistence() {
        return actionPersistence;
    }

    public static SculkHordeTriggers getInstance() {
        return instance;
    }

    public SculkHordeTriggers() {
        ModConfig.get();
        instance = this;
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Sculk Horde Triggers mod initialized");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        actionPersistence = new ActionPersistence(event.getServer());
    }
    
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        actionPersistence = null;
    }
}
