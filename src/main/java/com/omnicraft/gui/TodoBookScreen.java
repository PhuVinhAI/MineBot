package com.omnicraft.gui;

import com.omnicraft.hud.TodoTask;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class TodoBookScreen extends Screen {
    private final String rawTitle;
    private final String reqs;
    private String displayTitle;
    private final List<TodoTask> tasks = new ArrayList<>();

    public TodoBookScreen(String title, String reqs) {
        super(Component.literal(title));
        this.rawTitle = title;
        this.reqs = reqs;
    }

    @Override
    protected void init() {
        super.init();
        parseData();
    }

    private void parseData() {
        displayTitle = rawTitle;

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
    }

    private void updateCurrentCounts(Inventory inv) {
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

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        if (Minecraft.getInstance().player != null) {
            updateCurrentCounts(Minecraft.getInstance().player.getInventory());
        }

        int panelWidth = 260;
        int panelHeight = 60 + tasks.size() * 20;
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;

        // Vẽ viền và nền
        guiGraphics.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, 0xFFDDAA00); // Viền vàng
        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, 0xFF181818); // Nền tối

        // Tiêu đề
        guiGraphics.drawCenteredString(this.font, "§l" + displayTitle + "§r", this.width / 2, y + 15, 0xFFFFFF);

        // Đường gạch ngang
        guiGraphics.fill(x + 20, y + 30, x + panelWidth - 20, y + 31, 0xFF555555);

        int taskY = y + 45;
        for (TodoTask task : tasks) {
            String status = task.currentCount + " / " + task.requiredCount;
            boolean done = task.currentCount >= task.requiredCount;

            int color = done ? 0x55FF55 : 0xAAAAAA;
            String prefix = done ? "✔ " : "☐ ";

            guiGraphics.drawString(this.font, prefix + task.displayName, x + 20, taskY, color, false);

            int statusWidth = this.font.width(status);
            guiGraphics.drawString(this.font, status, x + panelWidth - 20 - statusWidth, taskY, color, false);

            taskY += 20;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}