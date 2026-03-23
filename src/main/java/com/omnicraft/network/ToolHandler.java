package com.omnicraft.network;

import com.omnicraft.session.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ToolHandler {
    private static final Pattern TOOL_PATTERN = Pattern.compile("<call_tool\\s+name=\"([^\"]+)\">(.*?)</call_tool>", Pattern.DOTALL);
    private static final Pattern ITEM_PATTERN = Pattern.compile("<item>(.*?)</item>", Pattern.DOTALL);

    public static void processToolCall(String xmlResponse) {
        SessionManager.addMessage("assistant", xmlResponse);

        Matcher matcher = TOOL_PATTERN.matcher(xmlResponse);
        if (matcher.find()) {
            String toolName = matcher.group(1);
            String innerXml = matcher.group(2);
            String toolResult = "";

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§7[AI đang sử dụng tool: " + toolName + "...]"), false);
            }

            try {
                if ("get_inventory".equals(toolName)) {
                    toolResult = getInventoryStr();
                } else if ("get_recipe".equals(toolName)) {
                    Matcher itemMatcher = ITEM_PATTERN.matcher(innerXml);
                    if (itemMatcher.find()) {
                        toolResult = getRecipeStr(itemMatcher.group(1).trim());
                    } else {
                        toolResult = "Lỗi: Thiếu thẻ <item>.";
                    }
                } else if ("set_todo_hud".equals(toolName)) {
                    Matcher titleMatcher = Pattern.compile("<title>(.*?)</title>", Pattern.DOTALL).matcher(innerXml);
                    Matcher goalMatcher = Pattern.compile("<goal name=\"([^\"]+)\">(.*?)</goal>", Pattern.DOTALL).matcher(innerXml);

                    if (titleMatcher.find()) {
                        String title = titleMatcher.group(1).trim();
                        net.minecraft.nbt.ListTag goalList = new net.minecraft.nbt.ListTag();

                        while (goalMatcher.find()) {
                            net.minecraft.nbt.CompoundTag gTag = new net.minecraft.nbt.CompoundTag();
                            gTag.putString("name", goalMatcher.group(1).trim());
                            gTag.putString("reqs", goalMatcher.group(2).trim());
                            goalList.add(gTag);
                        }

                        if (goalList.isEmpty()) {
                            toolResult = "Lỗi: Bạn chưa cung cấp bất kỳ thẻ <goal> nào. Hãy làm theo đúng cú pháp hướng dẫn.";
                        } else {
                            if (Minecraft.getInstance().hasSingleplayerServer()) {
                                Minecraft.getInstance().getSingleplayerServer().execute(() -> {
                                    net.minecraft.server.level.ServerPlayer sPlayer = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayer(Minecraft.getInstance().player.getUUID());
                                    if (sPlayer != null) {
                                        ItemStack book = new ItemStack(net.minecraft.world.item.Items.KNOWLEDGE_BOOK);
                                        book.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("§6Tiến trình: " + title));

                                        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
                                        tag.putString("omnicraft_title", title);
                                        tag.put("omnicraft_goals", goalList);
                                        book.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));

                                        sPlayer.getInventory().add(book);
                                    }
                                });
                                toolResult = "Đã đưa sách tiến trình vào túi đồ của người chơi.";
                            } else {
                                Minecraft.getInstance().execute(() -> {
                                    Minecraft.getInstance().setScreen(new com.omnicraft.gui.TodoBookScreen(title, goalList));
                                });
                                toolResult = "Đã hiển thị sách tiến trình cho người chơi trên màn hình.";
                            }
                        }
                    } else {
                        toolResult = "Lỗi: Thiếu thẻ <title>. Vui lòng kiểm tra lại cú pháp XML.";
                    }
                } else {
                    toolResult = "Lỗi: Không tìm thấy tool có tên " + toolName;
                }
            } catch (Exception e) {
                toolResult = "Lỗi khi thực thi tool: " + e.getMessage();
            }

            SessionManager.addMessage("user", "<tool_result name=\"" + toolName + "\">\n" + toolResult + "\n</tool_result>");
            AiProcessor.sendToAI(null); // Đệ quy: Cho AI chạy tiếp vòng lặp ReAct
        } else {
            SessionManager.addMessage("user", "System: Thẻ <call_tool> bị sai định dạng. Hãy tuân thủ đúng XML.");
            AiProcessor.sendToAI(null);
        }
    }

    private static String getInventoryStr() {
        if (Minecraft.getInstance().player == null) return "Không tìm thấy Player.";
        Inventory inv = Minecraft.getInstance().player.getInventory();
        Map<String, Integer> counts = new HashMap<>();
        Map<String, String> names = new HashMap<>();

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                counts.put(id, counts.getOrDefault(id, 0) + stack.getCount());
                names.put(id, stack.getItem().getDescription().getString());
            }
        }

        if (counts.isEmpty()) return "Túi đồ hiện đang trống rỗng.";

        StringBuilder sb = new StringBuilder("Inventory:\n");
        counts.forEach((k, v) -> sb.append("- ").append(k).append(" [").append(names.get(k)).append("]: ").append(v).append("\n"));
        return sb.toString();
    }

    private static String getRecipeStr(String itemId) {
        if (Minecraft.getInstance().level == null) return "Không tìm thấy thế giới (Level).";
        RecipeManager rm = Minecraft.getInstance().level.getRecipeManager();

        for (RecipeHolder<?> holder : rm.getAllRecipesFor(RecipeType.CRAFTING)) {
            ItemStack result = holder.value().getResultItem(Minecraft.getInstance().level.registryAccess());
            if (BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals(itemId)) {
                StringBuilder sb = new StringBuilder("Công thức chế tạo (Recipe) cho ").append(itemId).append(":\n");
                holder.value().getIngredients().forEach(ing -> {
                    if (!ing.isEmpty() && ing.getItems().length > 0) {
                        ItemStack[] items = ing.getItems();
                        if (items.length > 4) {
                            sb.append("- Nhóm nguyên liệu chung (hãy dùng tag bắt đầu bằng dấu #, ví dụ: #minecraft:planks, #minecraft:logs...)\n");
                        } else {
                            sb.append("- ");
                            for (int i = 0; i < items.length; i++) {
                                net.minecraft.world.item.Item item = items[i].getItem();
                                String id = BuiltInRegistries.ITEM.getKey(item).toString();
                                String name = item.getDescription().getString();
                                sb.append(id).append(" [").append(name).append("]");
                                if (i < items.length - 1) sb.append(" HOẶC ");
                            }
                            sb.append("\n");
                        }
                    }
                });
                return sb.toString();
            }
        }
        return "Không tìm thấy công thức chế tạo (crafting recipe) cho " + itemId;
    }
}