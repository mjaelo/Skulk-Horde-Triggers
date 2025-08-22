package com.sculkhordetriggers;

import com.sculkhordetriggers.config.ModConfig;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = SculkHordeTriggers.MODID, value = Dist.CLIENT)
public class ModEvents {
    private static final Logger LOGGER = LogManager.getLogger();
    private static String lastKnownBiome;

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return; // Only run on the server side
        }
        String toDimension = event.getTo().location().getPath();
        if (shouldTrigger(ModConfig.TriggerType.DIMENSION, toDimension, event.getEntity())) {
            executeTriggerCommand(event.getEntity(), ModConfig.TriggerType.DIMENSION);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return; // Only run on the server side
        }
        ItemStack itemStack = event.getItem().getItem();
        String itemId = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(itemStack.getItem())).toString();
        if (shouldTrigger(ModConfig.TriggerType.ITEM, itemId, event.getEntity())) {
            executeTriggerCommand(event.getEntity(), ModConfig.TriggerType.ITEM);
        }
    }

    @SubscribeEvent
    public static void onMobKilled(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            if (player.level().isClientSide()) {
                return; // Only run on the server side
            }
            LivingEntity killedEntity = event.getEntity();
            String mobId = EntityType.getKey(killedEntity.getType()).toString();
            if (shouldTrigger(ModConfig.TriggerType.MOB, mobId, player)) {
                executeTriggerCommand(player,ModConfig.TriggerType.MOB);
            }
        }
    }

    @SubscribeEvent
    public static void onAdvancementEarned(AdvancementEvent event) {
        if (event.getEntity().level().isClientSide()) {
            return; // Only run on the server side
        }
        Advancement advancement = event.getAdvancement();
        if (advancement == null) return;
        String advancementId = advancement.getId().toString();
        if (shouldTrigger(ModConfig.TriggerType.ADVANCEMENT, advancementId, event.getEntity())) {
            executeTriggerCommand(event.getEntity(),ModConfig.TriggerType.ADVANCEMENT);
        }
    }

    @SubscribeEvent
    public static void onEffectApplied(MobEffectEvent.Added event) {
        if (event.getEntity() instanceof Player player) {
            if (player.level().isClientSide()) {
                return; // Only run on the server side
            }
            MobEffectInstance effect = event.getEffectInstance();
            String effectId = Objects.requireNonNull(ForgeRegistries.MOB_EFFECTS.getKey(effect.getEffect())).toString();
            if (shouldTrigger(ModConfig.TriggerType.EFFECT, effectId, player)) {
                executeTriggerCommand(player,ModConfig.TriggerType.EFFECT);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Player player = event.player;
            if (player.level().isClientSide()) {
                return; // Only run on server side
            }
            String currentBiome = player.level().getBiome(player.blockPosition()).unwrapKey().get().location().getPath();
            if (!currentBiome.equals(lastKnownBiome)) {
                ModConfig.get().getTriggers(ModConfig.TriggerType.BIOME).stream()
                        .filter(trigger -> trigger.keywords().stream()
                                .filter(Objects::nonNull)
                                .filter(keyword -> !keyword.trim().isEmpty())
                                .anyMatch(keyword -> currentBiome.toLowerCase().contains(keyword.toLowerCase())))
                        .findFirst()
                        .ifPresent(trigger -> executeTriggerCommand(player,ModConfig.TriggerType.BIOME));
                lastKnownBiome = currentBiome;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerInteract(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide()) {
            return; // Only run on server side
        }
        Player player = event.getEntity();
        ItemStack itemStack = event.getItemStack();
        if (!itemStack.isEmpty()) {
            String itemId = Objects.requireNonNull(ForgeRegistries.ITEMS.getKey(itemStack.getItem())).toString();
            if (shouldTrigger(ModConfig.TriggerType.ITEM, itemId, player)) {
                executeTriggerCommand(player,ModConfig.TriggerType.ITEM);
            }
        }
    }

    private static boolean shouldTrigger(ModConfig.TriggerType triggerType, String triggerValue, Player player) {
        if (triggerValue == null || triggerValue.trim().isEmpty() || player == null) {
            return false;
        }

        String checkValue = triggerValue.toLowerCase();
        // Check if any trigger in the config matches the current event
        return ModConfig.get().getTriggers(triggerType).stream()
                .filter(trigger -> {
                    // Check if any keyword matches
                    return trigger.keywords().stream()
                            .filter(Objects::nonNull)
                            .filter(keyword -> !keyword.trim().isEmpty())
                            .anyMatch(keyword -> checkValue.contains(keyword.toLowerCase()));
                })
                .anyMatch(trigger -> player.getRandom().nextFloat() <= trigger.probability());
    }

    private static void executeTriggerCommand(Player player, ModConfig.TriggerType source) {
        if (player == null || player.level().isClientSide) {
            return; // Only run on server side
        }

        String command = ModConfig.get().getCommand()
                .replace("@p", player.getName().getString());
        executeConsoleCommand(command,source.name());
    }

    private static void executeConsoleCommand(String command,String sourceStr) {
        if (command == null || command.trim().isEmpty()) {
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            LOGGER.warn("Cannot execute command - server is not available");
            return;
        }

        try {
            CommandSourceStack source = server.createCommandSourceStack()
                    .withPermission(4) // OP level 4 (highest)
                    .withSuppressedOutput();

            LOGGER.info("Executing command: {}", command);
            server.getCommands().performPrefixedCommand(source, command);
            
            // Send confirmation message to all players
            server.getPlayerList().broadcastSystemMessage(
                Component.literal("§a[Trigger] §r Something felt the changes from §7" + sourceStr +"\n"+command),
                false
            );
        } catch (Exception e) {
            LOGGER.error("Failed to execute command: " + command, e);
        }
    }

}
