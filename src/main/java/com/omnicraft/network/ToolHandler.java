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
                    Matcher itemMatcher = ITEM_PATTERN.matcher(innerXml);
                    Matcher reqMatcher = Pattern.compile("<req>(.*?)</req>", Pattern.DOTALL).matcher(innerXml);
                    if (itemMatcher.find() && reqMatcher.find()) {
                        toolResult = com.omnicraft.hud.TodoHud.setTasks(itemMatcher.group(1).trim(), reqMatcher.group(1).trim());
                    } else {
                        toolResult = "Lỗi: Thiếu thẻ <item> hoặc <req>. Vui lòng kiểm tra lại cú pháp XML.";
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

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                counts.put(id, counts.getOrDefault(id, 0) + stack.getCount());
            }
        }

        if (counts.isEmpty()) return "Túi đồ hiện đang trống rỗng.";

        StringBuilder sb = new StringBuilder("Inventory:\n");
        counts.forEach((k, v) -> sb.append("- ").append(k).append(": ").append(v).append("\n"));
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
                                sb.append(BuiltInRegistries.ITEM.getKey(items[i].getItem()).toString());
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