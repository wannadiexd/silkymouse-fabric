package com.wannadiexd.tapemouse;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TapeMouse implements ClientModInitializer {
    public static final String MOD_ID = "tapemouse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding activeBinding = null;
    private static int delay = 0;
    private static int counter = 0;
    private static final MinecraftClient client = MinecraftClient.getInstance();
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("TapeMouse initialized");
        
        // Register the command
        ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);
        
        // Register tick event for key emulation
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (activeBinding != null) {
                if (delay == 0) {
                    // Continuous press
                    KeyBinding.setKeyPressed(activeBinding.getDefaultKey(), true);
                } else {
                    // Toggle on/off with delay
                    counter = (counter + 1) % (delay * 2);
                    KeyBinding.setKeyPressed(activeBinding.getDefaultKey(), counter < delay);
                }
            }
        });
    }
    
    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
            ClientCommandManager.literal("tapemouse")
                .executes(this::listKeybinds)
                .then(ClientCommandManager.literal("off")
                    .executes(this::disableAll))
                .then(ClientCommandManager.argument("keybinding", StringArgumentType.word())
                    .executes(context -> enableKeybind(context, 20))
                    .then(ClientCommandManager.argument("delay", IntegerArgumentType.integer(0))
                        .executes(context -> enableKeybind(
                            context, 
                            IntegerArgumentType.getInteger(context, "delay")
                        ))
                    )
                )
        );
    }
    
    private int listKeybinds(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        
        // Send header
        source.sendFeedback(Text.literal("Available keybindings:").setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
        
        // Group keybindings by category
        Map<String, List<KeyBinding>> keysByCategory = new HashMap<>();
        for (KeyBinding keyBinding : client.options.allKeys) {
            keysByCategory.computeIfAbsent(keyBinding.getCategory(), k -> new ArrayList<>()).add(keyBinding);
        }
        
        // Display all keybindings grouped by category
        keysByCategory.forEach((category, keys) -> {
            source.sendFeedback(Text.literal(category + ":").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
            
            for (KeyBinding key : keys) {
                String keyName = key.getTranslationKey().replace("key.minecraft.", "");
                source.sendFeedback(Text.literal(" - " + keyName + " (" + key.getBoundKeyLocalizedText().getString() + ")"));
            }
        });
        
        source.sendFeedback(Text.literal("Usage: /tapemouse <off|keybinding> [delay]").setStyle(Style.EMPTY.withColor(Formatting.GRAY)));
        
        return 1;
    }
    
    private int disableAll(CommandContext<FabricClientCommandSource> context) {
        if (activeBinding != null) {
            // Make sure to release the key if it was being held
            KeyBinding.setKeyPressed(activeBinding.getDefaultKey(), false);
            
            context.getSource().sendFeedback(Text.literal("TapeMouse disabled").setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
            LOGGER.info("TapeMouse disabled");
            
            activeBinding = null;
            counter = 0;
        } else {
            context.getSource().sendFeedback(Text.literal("TapeMouse was already disabled").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
        }
        
        return 1;
    }
    
    private int enableKeybind(CommandContext<FabricClientCommandSource> context, int newDelay) {
        String bindingName = StringArgumentType.getString(context, "keybinding");
        KeyBinding targetBinding = null;
        
        // First try to find the exact match
        for (KeyBinding keyBinding : client.options.allKeys) {
            String keyName = keyBinding.getTranslationKey().replace("key.minecraft.", "");
            if (keyName.equalsIgnoreCase(bindingName)) {
                targetBinding = keyBinding;
                break;
            }
        }
        
        // If not found, try partial match
        if (targetBinding == null) {
            for (KeyBinding keyBinding : client.options.allKeys) {
                String keyName = keyBinding.getTranslationKey().replace("key.minecraft.", "");
                if (keyName.contains(bindingName)) {
                    targetBinding = keyBinding;
                    break;
                }
            }
        }
        
        if (targetBinding != null) {
            // Disable previous binding if any
            if (activeBinding != null) {
                KeyBinding.setKeyPressed(activeBinding.getDefaultKey(), false);
            }
            
            activeBinding = targetBinding;
            delay = newDelay;
            counter = 0;
            
            String message = "TapeMouse enabled for " + targetBinding.getTranslationKey().replace("key.minecraft.", "") + 
                    " (" + targetBinding.getBoundKeyLocalizedText().getString() + ")";
            
            if (delay == 0) {
                message += " with continuous press";
            } else {
                message += " with delay " + delay;
            }
            
            context.getSource().sendFeedback(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
            LOGGER.info(message);
            
            return 1;
        } else {
            context.getSource().sendFeedback(Text.literal("Unknown keybinding: " + bindingName)
                    .setStyle(Style.EMPTY.withColor(Formatting.RED)));
            return 0;
        }
    }
}