package com.omnicraft.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
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

    public static void setTasks(String title, String reqs) {
        currentTitle = title;
        tasks.clear();

        String[] parts = reqs.split("[,\\n]+");
        for (String p : parts) {
            String[] kv = p.trim().split(":");
            if (kv.length >= 2) {
                try {
                    String id = kv.length >= 3 ? kv[0] + ":" + kv[1] : kv[0];
                    int count = Integer.parseInt(kv[kv.length - 1].trim());
                    tasks.add(new TodoTask(id, count));
                } catch (Exception ignored) {}
            }
        }
        active = !tasks.isEmpty();
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
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                for (TodoTask task : tasks) {
                    if (task.itemId.equals(id)) {
                        task.currentCount += stack.getCount();
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
                String[] idParts = task.itemId.split(":");
                String shortName = idParts.length > 1 ? idParts[1] : task.itemId;
                String text = shortName + ": " + task.currentCount + " / " + task.requiredCount;
                int color = (task.currentCount >= task.requiredCount) ? 0x55FF55 : 0xFFFFFF;
                guiGraphics.drawString(Minecraft.getInstance().font, text, screenWidth - 150, y, color);
                y += 12;
            }
        });
    }
}