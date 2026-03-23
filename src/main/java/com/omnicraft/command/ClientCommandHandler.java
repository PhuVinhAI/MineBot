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
                .then(Commands.literal("session")
                    .then(Commands.literal("new")
                        .executes(context -> {
                            com.omnicraft.session.SessionManager.createNewSession(null);
                            sendMsg("§a[OmniCraft] Đã tạo phiên mới: " + com.omnicraft.session.SessionManager.getCurrentSession().name);
                            return 1;
                        })
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes(context -> {
                                String name = StringArgumentType.getString(context, "name");
                                com.omnicraft.session.SessionManager.createNewSession(name);
                                sendMsg("§a[OmniCraft] Đã tạo phiên mới: " + name);
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("list")
                        .executes(context -> {
                            java.util.List<com.omnicraft.session.ChatSession> sessions = com.omnicraft.session.SessionManager.getSessions();
                            sendMsg("§b[OmniCraft] Danh sách phiên chat:");
                            for (com.omnicraft.session.ChatSession s : sessions) {
                                String active = (com.omnicraft.session.SessionManager.getCurrentSession() != null && s.id.equals(com.omnicraft.session.SessionManager.getCurrentSession().id)) ? " §e(Đang chọn)" : "";
                                sendMsg("§f- ID: §a" + s.id + " §f| Tên: " + s.name + active);
                            }
                            return 1;
                        })
                    )
                    .then(Commands.literal("switch")
                        .then(Commands.argument("id", StringArgumentType.string())
                            .executes(context -> {
                                String id = StringArgumentType.getString(context, "id");
                                if (com.omnicraft.session.SessionManager.switchSession(id)) {
                                    sendMsg("§a[OmniCraft] Đã chuyển sang phiên: " + com.omnicraft.session.SessionManager.getCurrentSession().name);
                                } else {
                                    sendMsg("§c[OmniCraft] Không tìm thấy phiên với ID: " + id);
                                }
                                return 1;
                            })
                        )
                    )
                    .then(Commands.literal("delete")
                        .then(Commands.argument("id", StringArgumentType.string())
                            .executes(context -> {
                                String id = StringArgumentType.getString(context, "id");
                                if (com.omnicraft.session.SessionManager.deleteSession(id)) {
                                    sendMsg("§a[OmniCraft] Đã xóa phiên: " + id);
                                    if (com.omnicraft.session.SessionManager.getCurrentSession() != null && com.omnicraft.session.SessionManager.getCurrentSession().id.equals(id)) {
                                        com.omnicraft.session.SessionManager.init();
                                    }
                                } else {
                                    sendMsg("§c[OmniCraft] Xóa thất bại hoặc không tìm thấy ID: " + id);
                                }
                                return 1;
                            })
                        )
                    )
                )
                .then(Commands.literal("hud")
                    .then(Commands.literal("clear")
                        .executes(context -> {
                            com.omnicraft.hud.TodoHud.clear();
                            sendMsg("§a[OmniCraft] Đã ẩn HUD To-do list.");
                            return 1;
                        })
                    )
                    .then(Commands.literal("show")
                        .executes(context -> {
                            com.omnicraft.hud.TodoHud.show();
                            sendMsg("§a[OmniCraft] Đã hiển thị HUD To-do list (nếu có dữ liệu).");
                            return 1;
                        })
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