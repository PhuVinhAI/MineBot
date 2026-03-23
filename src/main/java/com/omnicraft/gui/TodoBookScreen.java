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
    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
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
        try {
            if (!rawTitle.contains(" ")) {
                String lookupId = rawTitle.contains(":") ? rawTitle : "minecraft:" + rawTitle;
                Item tItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(lookupId));
                displayTitle = tItem != Items.AIR ? tItem.getDescription().getString() : rawTitle.replace("_", " ");
            } else {
                displayTitle = rawTitle;
            }
        } catch (Exception e) {
            displayTitle = rawTitle.replace("_", " ");
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

        int x = (this.width - 192) / 2;
        int y = (this.height - 192) / 2;

        guiGraphics.blit(BOOK_TEXTURE, x, y, 0, 0, 192, 192, 256, 256);

        if (Minecraft.getInstance().player != null) {
            updateCurrentCounts(Minecraft.getInstance().player.getInventory());
        }

        guiGraphics.drawString(this.font, "§l" + displayTitle + "§r", x + 20, y + 20, 0x000000, false);

        int taskY = y + 40;
        for (TodoTask task : tasks) {
            String text = "- " + task.displayName + ": " + task.currentCount + " / " + task.requiredCount;
            int color = (task.currentCount >= task.requiredCount) ? 0x008800 : 0x000000;
            guiGraphics.drawString(this.font, text, x + 20, taskY, color, false);
            taskY += 12;
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}