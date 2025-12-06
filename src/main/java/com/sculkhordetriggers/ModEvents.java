package com.sculkhordetriggers;

import com.sculkhordetriggers.data.TriggerType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = SculkHordeTriggers.MODID)
public class ModEvents {
    private static MinecraftServer serverInstance;
    private static final TriggerHandler TRIGGER_HANDLER = TriggerHandler.get();
    private static String lastKnownBiome;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START && serverInstance == null) {
            serverInstance = event.getServer();
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        // Check for biome changes
        String currentBiome = player.level().getBiome(player.blockPosition()).unwrapKey().get().location().getPath();
        if (!currentBiome.equals(lastKnownBiome)) {
            TRIGGER_HANDLER.handleTrigger(TriggerType.BIOME, currentBiome, player, serverInstance);
            lastKnownBiome = currentBiome;
        }
    }

    @SubscribeEvent
    public static void onEntityKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            String mobName = ForgeRegistries.ENTITY_TYPES.getKey(event.getEntity().getType()).toString();
            TRIGGER_HANDLER.handleTrigger(
                TriggerType.MOB,
                    mobName,
                player,
                serverInstance
            );
        }
    }

    @SubscribeEvent
    public static void onAdvancement(AdvancementEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            String advancementId = event.getAdvancement().getId().toString();
            TRIGGER_HANDLER.handleTrigger(
                TriggerType.ADVANCEMENT,
                advancementId,
                player,
                serverInstance
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerInteractRH(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return; // Only run on server side with ServerPlayer
        }
        ItemStack itemStack = event.getItemStack();
        handleItemStack(player, itemStack);
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack itemStack = event.getItem().getItem();
            handleItemStack(player, itemStack);
        }
    }

    private static void handleItemStack(ServerPlayer player, ItemStack itemStack) {
        if (!itemStack.isEmpty()) {
            String itemName = ForgeRegistries.ITEMS.getKey(itemStack.getItem()).toString();
            TRIGGER_HANDLER.handleTrigger(
                TriggerType.ITEM,
                itemName,
                    player,
                serverInstance
            );
        }
    }


}
