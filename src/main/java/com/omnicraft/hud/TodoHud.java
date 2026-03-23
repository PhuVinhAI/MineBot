package com.omnicraft.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = "omnicraft", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class TodoHud {
    public static final ResourceLocation TODO_HUD = ResourceLocation.fromNamespaceAndPath("omnicraft", "todo_hud");

    private static String currentTitle = "";
    private static final List<TodoTask> tasks = new ArrayList<>();
    private static boolean active = false;

    public static String setTasks(String title, String reqs) {
        try {
            if (!title.contains(" ")) {
                String lookupId = title.contains(":") ? title : "minecraft:" + title;
                Item tItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(lookupId));
                currentTitle = tItem != Items.AIR ? tItem.getDescription().getString() : title.replace("_", " ");
            } else {
                currentTitle = title;
            }
        } catch (Exception e) {
            currentTitle = title.replace("_", " ");
        }

        tasks.clear();
        String[] parts = reqs.split("[,\\n]+");
        for (String p : parts) {
            String cleanPart = p.trim();
            if (cleanPart.isEmpty()) continue;

            String[] kv = cleanPart.split(":");
            if (kv.length >= 2) {
                try {
                    int count = Integer.parseInt(kv[kv.length - 1].trim());
                    String rawId = cleanPart.substring(0, cleanPart.lastIndexOf(':')).trim();

                    if (!rawId.contains(":")) {
                        if (rawId.startsWith("#")) {
                            rawId = "#minecraft:" + rawId.substring(1);
                        } else {
                            rawId = "minecraft:" + rawId;
                        }
                    }

                    boolean isTag = rawId.startsWith("#");
                    String dName;

                    if (isTag) {
                        String path = rawId.substring(rawId.indexOf(':') + 1);
                        dName = "Nhóm " + path.replace("_", " ");
                    } else {
                        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(rawId));
                        dName = item != Items.AIR ? item.getDescription().getString() : rawId;
                    }

                    tasks.add(new TodoTask(rawId, isTag, dName, count));
                } catch (Exception ignored) {}
            }
        }
        active = !tasks.isEmpty();
        if (active) {
            return "HUD To-do list đã được cập nhật với " + tasks.size() + " mục.";
        } else {
            return "Lỗi Parser: Yêu cầu của bạn (" + reqs + ") sai định dạng. Hãy dùng dạng ID:Count (VD: minecraft:oak_planks:4)";
        }
    }

    public static void show() {
        if (!tasks.isEmpty()) {
            active = true;
        }
    }

    public static void clear() {
        active = false;
        tasks.clear();
    }

    public static boolean isActive() {
        return active;
    }

    public static void updateCurrentCounts(Inventory inv) {
        for (TodoTask task : tasks) {
            task.currentCount = 0;
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                for (TodoTask task : tasks) {
                    if (task.isTag) {
                        try {
                            String tagId = task.itemId.substring(1);
                            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(tagId));
                            if (stack.is(tagKey)) {
                                task.currentCount += stack.getCount();
                            }
                        } catch (Exception ignored) {}
                    } else {
                        if (task.itemId.equals(itemId)) {
                            task.currentCount += stack.getCount();
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(TODO_HUD, (guiGraphics, partialTick) -> {
            if (!active || Minecraft.getInstance().player == null) return;

            updateCurrentCounts(Minecraft.getInstance().player.getInventory());

            int screenWidth = guiGraphics.guiWidth();
            int y = 20;

            guiGraphics.drawString(Minecraft.getInstance().font, "§l[Mục tiêu: " + currentTitle + "]§r", screenWidth - 150, y, 0xFFDD00);
            y += 12;

            for (TodoTask task : tasks) {
                String text = task.displayName + ": " + task.currentCount + " / " + task.requiredCount;
                int color = (task.currentCount >= task.requiredCount) ? 0x55FF55 : 0xFFFFFF;
                guiGraphics.drawString(Minecraft.getInstance().font, text, screenWidth - 150, y, color);
                y += 12;
            }
        });
    }
}