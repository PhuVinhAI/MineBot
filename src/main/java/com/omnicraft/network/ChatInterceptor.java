package com.omnicraft.network;

import com.omnicraft.OmniCraftAI;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientChatEvent;

@EventBusSubscriber(modid = OmniCraftAI.MOD_ID, value = Dist.CLIENT)
public class ChatInterceptor {

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        String msg = event.getMessage();
        if (msg.startsWith("@bot ")) {
            event.setCanceled(true); // Ngăn không gửi chat lên server
            String userText = msg.substring(5);

            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§bBạn: §f" + userText), false);
            }

            AiProcessor.sendToAI(userText);
        }
    }
}