package com.omnicraft.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.omnicraft.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = "omnicraft", value = Dist.CLIENT)
public class ClientCommandHandler {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("ai")
                .then(Commands.literal("config")
                    .then(Commands.literal("set")
                        .then(Commands.literal("api_key")
                            .then(Commands.argument("key", StringArgumentType.string())
                                .executes(context -> {
                                    String key = StringArgumentType.getString(context, "key");
                                    ConfigManager.getConfig().apiKey = key;
                                    ConfigManager.save();
                                    sendMsg("§a[OmniCraft] Đã lưu API Key an toàn.");
                                    return 1;
                                })
                            )
                        )
                        .then(Commands.literal("base_url")
                            .then(Commands.argument("url", StringArgumentType.string())
                                .executes(context -> {
                                    String url = StringArgumentType.getString(context, "url");
                                    ConfigManager.getConfig().baseUrl = url;
                                    ConfigManager.save();
                                    sendMsg("§a[OmniCraft] Đã lưu Base URL: " + url);
                                    return 1;
                                })
                            )
                        )
                        .then(Commands.literal("model")
                            .then(Commands.argument("model_name", StringArgumentType.string())
                                .executes(context -> {
                                    String modelName = StringArgumentType.getString(context, "model_name");
                                    ConfigManager.getConfig().model = modelName;
                                    ConfigManager.save();
                                    sendMsg("§a[OmniCraft] Đã lưu Model: " + modelName);
                                    return 1;
                                })
                            )
                        )
                    )
                )
        );
    }

    private static void sendMsg(String msg) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(Component.literal(msg), false);
        }
    }
}