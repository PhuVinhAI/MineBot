package com.omnicraft.network;

import com.omnicraft.gui.TodoBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = "omnicraft", value = Dist.CLIENT)
public class BookInteractHandler {

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (stack.has(DataComponents.CUSTOM_DATA)) {
            CustomData data = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag tag = data.copyTag();
            if (tag.contains("omnicraft_title")) {
                if (event.getLevel().isClientSide()) {
                    String title = tag.getString("omnicraft_title");
                    String reqs = tag.getString("omnicraft_reqs");
                    Minecraft.getInstance().setScreen(new TodoBookScreen(title, reqs));
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
            }
        }
    }
}