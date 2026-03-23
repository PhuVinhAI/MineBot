package com.omnicraft.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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
    private final String displayTitle;
    private final ListTag goalsTag;
    private final List<TodoGoal> goals = new ArrayList<>();

    // Trạng thái UI
    private int currentGoalIndex = -1; // -1: Xem danh sách Goal, >=0: Xem chi tiết Goal đó
    private int page = 0;
    private static final int ITEMS_PER_PAGE = 6;

    private final int panelWidth = 280;
    private final int panelHeight = 200;

    public TodoBookScreen(String title, ListTag goalsTag) {
        super(Component.literal(title));
        this.displayTitle = title;
        this.goalsTag = goalsTag;
    }

    @Override
    protected void init() {
        super.init();
        parseData();
    }

    private void parseData() {
        goals.clear();
        for (int i = 0; i < goalsTag.size(); i++) {
            CompoundTag gTag = goalsTag.getCompound(i);
            TodoGoal goal = new TodoGoal(gTag.getString("name"));
            String reqs = gTag.getString("reqs");

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
                            rawId = rawId.startsWith("#") ? "#minecraft:" + rawId.substring(1) : "minecraft:" + rawId;
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
                        goal.items.add(new TodoItem(rawId, isTag, dName, count));
                    } catch (Exception ignored) {}
                }
            }
            goals.add(goal);
        }
    }

    private void updateCurrentCounts(Inventory inv) {
        for (TodoGoal goal : goals) {
            for (TodoItem item : goal.items) {
                item.currentCount = 0;
            }
        }
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty()) {
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                for (TodoGoal goal : goals) {
                    for (TodoItem item : goal.items) {
                        if (item.isTag) {
                            try {
                                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, ResourceLocation.parse(item.itemId.substring(1)));
                                if (stack.is(tagKey)) item.currentCount += stack.getCount();
                            } catch (Exception ignored) {}
                        } else if (item.itemId.equals(itemId)) {
                            item.currentCount += stack.getCount();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        if (Minecraft.getInstance().player != null) updateCurrentCounts(Minecraft.getInstance().player.getInventory());

        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;

        // Vẽ viền và nền
        guiGraphics.fill(x - 2, y - 2, x + panelWidth + 2, y + panelHeight + 2, 0xFFDDAA00);
        guiGraphics.fill(x, y, x + panelWidth, y + panelHeight, 0xFF1E1E1E);

        // Header: Nút X và Tiêu đề
        boolean hoverX = isHovering(mouseX, mouseY, x + panelWidth - 20, y + 5, 15, 15);
        guiGraphics.drawString(this.font, "§lX", x + panelWidth - 15, y + 10, hoverX ? 0xFFFF55 : 0xAAAAAA, true);

        String headerText = (currentGoalIndex == -1) ? "§l" + displayTitle : "§l" + goals.get(currentGoalIndex).name;
        guiGraphics.drawCenteredString(this.font, headerText, this.width / 2, y + 15, 0xFFFFFF);
        guiGraphics.fill(x + 10, y + 30, x + panelWidth - 10, y + 31, 0xFF555555);

        // Nút Back nếu đang xem chi tiết
        if (currentGoalIndex != -1) {
            boolean hoverBack = isHovering(mouseX, mouseY, x + 10, y + 10, 40, 15);
            guiGraphics.drawString(this.font, "§l< Quay lại", x + 10, y + 10, hoverBack ? 0xFFDD00 : 0xAAAAAA, true);
        }

        // Vẽ Danh sách
        int listSize = (currentGoalIndex == -1) ? goals.size() : goals.get(currentGoalIndex).items.size();
        int maxPages = (int) Math.ceil((double) listSize / ITEMS_PER_PAGE);
        if (page >= maxPages) page = Math.max(0, maxPages - 1);

        int startIdx = page * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, listSize);
        int currentY = y + 45;

        for (int i = startIdx; i < endIdx; i++) {
            if (currentGoalIndex == -1) {
                // Đang ở Menu Nhóm (Goals)
                TodoGoal goal = goals.get(i);
                boolean isDone = goal.isComplete();
                boolean hover = isHovering(mouseX, mouseY, x + 10, currentY - 5, panelWidth - 20, 20);

                int color = isDone ? 0x55FF55 : (hover ? 0xFFDD00 : 0xDDDDDD);
                String prefix = isDone ? "✔ " : "▶ ";
                if (hover) guiGraphics.fill(x + 10, currentY - 5, x + panelWidth - 10, currentY + 15, 0x22FFFFFF);
                guiGraphics.drawString(this.font, prefix + goal.name, x + 20, currentY, color, true);

            } else {
                // Đang ở Chi tiết Món (Items)
                TodoItem item = goals.get(currentGoalIndex).items.get(i);
                boolean isDone = item.currentCount >= item.requiredCount;
                int color = isDone ? 0x55FF55 : 0xAAAAAA;
                String prefix = isDone ? "✔ " : "☐ ";

                guiGraphics.drawString(this.font, prefix + item.displayName, x + 20, currentY, color, true);

                String status = item.currentCount + " / " + item.requiredCount;
                int statusWidth = this.font.width(status);
                guiGraphics.drawString(this.font, status, x + panelWidth - 20 - statusWidth, currentY, color, true);
            }
            currentY += 22;
        }

        // Điều hướng trang (Pagination)
        if (maxPages > 1) {
            boolean hoverPrev = isHovering(mouseX, mouseY, x + panelWidth / 2 - 40, y + panelHeight - 20, 20, 15);
            boolean hoverNext = isHovering(mouseX, mouseY, x + panelWidth / 2 + 20, y + panelHeight - 20, 20, 15);

            if (page > 0) guiGraphics.drawString(this.font, "§l<<<", x + panelWidth / 2 - 40, y + panelHeight - 15, hoverPrev ? 0xFFFFFF : 0x888888, true);
            guiGraphics.drawCenteredString(this.font, (page + 1) + " / " + maxPages, x + panelWidth / 2, y + panelHeight - 15, 0xAAAAAA);
            if (page < maxPages - 1) guiGraphics.drawString(this.font, "§l>>>", x + panelWidth / 2 + 20, y + panelHeight - 15, hoverNext ? 0xFFFFFF : 0x888888, true);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (this.width - panelWidth) / 2;
        int y = (this.height - panelHeight) / 2;

        // Click X
        if (isHovering(mouseX, mouseY, x + panelWidth - 20, y + 5, 15, 15)) {
            this.onClose();
            return true;
        }

        // Click Back
        if (currentGoalIndex != -1 && isHovering(mouseX, mouseY, x + 10, y + 10, 40, 15)) {
            currentGoalIndex = -1;
            page = 0;
            return true;
        }

        // Click Phân trang
        int listSize = (currentGoalIndex == -1) ? goals.size() : goals.get(currentGoalIndex).items.size();
        int maxPages = (int) Math.ceil((double) listSize / ITEMS_PER_PAGE);
        if (maxPages > 1) {
            if (page > 0 && isHovering(mouseX, mouseY, x + panelWidth / 2 - 40, y + panelHeight - 20, 20, 15)) {
                page--; return true;
            }
            if (page < maxPages - 1 && isHovering(mouseX, mouseY, x + panelWidth / 2 + 20, y + panelHeight - 20, 20, 15)) {
                page++; return true;
            }
        }

        // Click chọn Goal
        if (currentGoalIndex == -1) {
            int startIdx = page * ITEMS_PER_PAGE;
            int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, listSize);
            int currentY = y + 45;
            for (int i = startIdx; i < endIdx; i++) {
                if (isHovering(mouseX, mouseY, x + 10, currentY - 5, panelWidth - 20, 20)) {
                    currentGoalIndex = i;
                    page = 0;
                    return true;
                }
                currentY += 22;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHovering(double mouseX, double mouseY, int rx, int ry, int rw, int rh) {
        return mouseX >= rx && mouseX <= rx + rw && mouseY >= ry && mouseY <= ry + rh;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}