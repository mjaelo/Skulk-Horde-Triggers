package com.sculkhordetriggers;

import com.sculkhordetriggers.config.ModConfig;
import com.sculkhordetriggers.data.ActionData;
import com.sculkhordetriggers.data.EffectData;
import com.sculkhordetriggers.data.TriggerType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class TriggerHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Random RANDOM = new Random();
    private static TriggerHandler INSTANCE;
    private final ModConfig config;

    private TriggerHandler() {
        this.config = ModConfig.get();
    }

    public static TriggerHandler get() {
        if (INSTANCE == null) {
            INSTANCE = new TriggerHandler();
        }
        return INSTANCE;
    }

    /**
     * Handles a trigger event
     *
     * @param type   The type of trigger
     * @param value  The value that triggered the event (e.g., item ID, mob ID, etc.)
     * @param player The player involved in the trigger
     * @param server The Minecraft server instance
     */
    public void handleTrigger(TriggerType type, String value, ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return;

        config.getAllActions().forEach((actionId, actionData) -> {
            actionData.triggers().stream()
                    .filter(trigger -> trigger.type() == type)
                    .filter(trigger -> trigger.keywords().stream()
                            .anyMatch(keyword -> value.toLowerCase().contains(keyword.toLowerCase())))
                    .filter(trigger -> RANDOM.nextFloat() <= trigger.probability())
                    .findFirst()
                    .ifPresent(trigger -> executeEffects(actionData, player, server));
        });
    }

    private void executeEffects(ActionData actionData, ServerPlayer player, MinecraftServer server) {
        for (EffectData effect : actionData.effects()) {
            try {
                switch (effect.type()) {
                    case COMMAND -> executeCommand(effect.value(), player, server);
                    case ITEM -> giveItem(effect.value(), player);
                    case EFFECT -> applyEffect(effect.value(), player);
                }
            } catch (Exception e) {
                LOGGER.error("Failed to execute effect: {}", effect, e);
            }
        }
    }

    private void executeCommand(String command, ServerPlayer player, MinecraftServer server) {
        try {
            CommandSourceStack source = server.createCommandSourceStack()
                    .withPermission(4) // OP level 4 (highest)
                    .withSuppressedOutput();

            LOGGER.info("Executing command: {}", command);
            server.getCommands().performPrefixedCommand(source, command);

            // Send confirmation message to all players
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("§a §r Something has been awoken. §7"),
                    false
            );
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: " + command, e);
        }
    }

    private void giveItem(String itemId, ServerPlayer player) {
        ItemStack itemStack = new ItemStack(Objects.requireNonNull(
                ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId))));
        if (!player.addItem(itemStack)) {
            player.drop(itemStack, false);
        }
    }

    private void applyEffect(String effectString, ServerPlayer player) {
        String[] parts = effectString.split(":");
        if (parts.length < 2) return;
        MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(parts[0]));
        if (effect == null) return;
        int duration = 200; // Default 10 seconds
        int amplifier = 0;
        player.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }
}
