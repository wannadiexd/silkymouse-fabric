package com.wannadiexd.tapemouse;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
// import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
// import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TapeMouse implements ClientModInitializer {
    public static final String MOD_ID = "tapemouse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding activeBinding = null;
    private static int delay = 0;
    private static int counter = 0;
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Tapemouse initialized");
        
        ClientCommandRegistrationCallback.EVENT.register(this::registerCommands);
    
        ClientTickEvents.START_CLIENT_TICK.register(minecraft -> {
            if (minecraft.currentScreen != null) return;
            if (activeBinding == null) return;
            if (counter++ < delay) return;
            
            counter = 0;
            
            if (delay == 0) {
                activeBinding.setPressed(true);
            } else {
                KeyBinding.onKeyPressed(activeBinding.getDefaultKey());
                KeyBinding.updatePressedStates();
            }
        });
        
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (activeBinding != null && delay > 0) {
                activeBinding.setPressed(false);
            }
        });
        
        
        LOGGER.info("Tapemouse setup completed");
    }
    
    private void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
            ClientCommandManager.literal("tapemouse")
                .executes(this::showHelp)
                .then(ClientCommandManager.literal("list")
                    .executes(this::listKeybinds))
                .then(ClientCommandManager.literal("off")
                    .executes(this::disableAll))
                .then(ClientCommandManager.argument("binding", StringArgumentType.word())
                    .then(ClientCommandManager.argument("delay", IntegerArgumentType.integer(0))
                        .executes(context -> enableKeybind(
                            context, 
                            StringArgumentType.getString(context, "binding"),
                            IntegerArgumentType.getInteger(context, "delay")
                        ))
                    )
                )
        );
    }
    
    private int showHelp(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        
        source.sendFeedback(Text.literal("Tapemouse help: ").setStyle(Style.EMPTY.withColor(Formatting.AQUA)));
        source.sendFeedback(Text.literal("Run '/tapemouse list' to get a list of keybindings."));
        source.sendFeedback(Text.literal("Run '/tapemouse off' to stop Tapemouse."));
        source.sendFeedback(Text.literal("Run '/tapemouse <binding> <delay>' to start Tapemouse."));
        source.sendFeedback(Text.literal("  delay is the number of ticks between every keypress. Set to 0 to hold down the key."));
        
        return 1;
    }
    
    private int listKeybinds(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();
        
        List<String> keys = new ArrayList<>();
        for (KeyBinding binding : mc.options.allKeys) {
            keys.add(binding.getTranslationKey().replaceFirst("^key\\.", ""));
        }
        keys = keys.stream().sorted().collect(Collectors.toList());
        
        source.sendFeedback(Text.literal(String.join(", ", keys)));
        
        return 1;
    }
    
    private int disableAll(CommandContext<FabricClientCommandSource> context) {
        if (activeBinding != null) {
            if (delay == 0) {
                activeBinding.setPressed(false);
            }
            
            context.getSource().sendFeedback(Text.literal("Tapemouse disabled").setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
            LOGGER.info("Tapemouse disabled via command");
            
            activeBinding = null;
            counter = 0;
        } else {
            context.getSource().sendFeedback(Text.literal("Tapemouse was not active").setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
        }
        
        return 1;
    }
    
    private int enableKeybind(CommandContext<FabricClientCommandSource> context, String bindingName, int newDelay) {
        FabricClientCommandSource source = context.getSource();
        KeyBinding targetBinding = null;
        
        String fullName = bindingName.startsWith("key.") ? bindingName : "key." + bindingName;
        
        for (KeyBinding keyBinding : mc.options.allKeys) {
            if (keyBinding.getTranslationKey().equals(fullName)) {
                targetBinding = keyBinding;
                break;
            }
        }
        
        if (targetBinding == null) {
            for (KeyBinding keyBinding : mc.options.allKeys) {
                if (keyBinding.getTranslationKey().equals(bindingName)) {
                    targetBinding = keyBinding;
                    break;
                }
            }
        }
        
        if (targetBinding == null) {
            if (bindingName.equalsIgnoreCase("attack")) {
                targetBinding = mc.options.attackKey;
            } else if (bindingName.equalsIgnoreCase("use")) {
                targetBinding = mc.options.useKey;
            } else if (bindingName.equalsIgnoreCase("forward")) {
                targetBinding = mc.options.forwardKey;
            } else if (bindingName.equalsIgnoreCase("jump")) {
                targetBinding = mc.options.jumpKey;
            }
        }
        
        if (targetBinding != null) {
            if (activeBinding != null && delay == 0) {
                activeBinding.setPressed(false);
            }
            
            activeBinding = targetBinding;
            delay = newDelay;
            counter = 0;
            
            String message = "Tapemouse enabled for " + targetBinding.getTranslationKey().replaceFirst("^key\\.", "") + 
                    " (" + targetBinding.getBoundKeyLocalizedText().getString() + ")";
            message += " with delay " + delay;
            
            source.sendFeedback(Text.literal(message).setStyle(Style.EMPTY.withColor(Formatting.GREEN)));
            LOGGER.info(message);
            
            return 1;
        } else {
            source.sendFeedback(Text.literal(bindingName + " is not a valid keybinding.").setStyle(Style.EMPTY.withColor(Formatting.RED)));
            return 0;
        }
    }
}