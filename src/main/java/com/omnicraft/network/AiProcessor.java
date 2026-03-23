package com.omnicraft.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.omnicraft.config.ConfigManager;
import com.omnicraft.session.ChatMessage;
import com.omnicraft.session.SessionManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class AiProcessor {
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final Gson GSON = new Gson();

    private static final String SYSTEM_PROMPT = "Bạn là OmniCraft AI, trợ lý Minecraft. Bạn có thể sử dụng các công cụ sau bằng cách xuất ra thẻ XML:\n" +
            "1. <call_tool name=\"get_inventory\"></call_tool>: Lấy túi đồ hiện tại.\n" +
            "2. <call_tool name=\"get_recipe\"><item>id_vật_phẩm</item></call_tool>: Lấy công thức.\n" +
            "3. <call_tool name=\"set_todo_hud\"><item>tên</item><req>id:số_lượng</req></call_tool>: Bật UI theo dõi.\n" +
            "Nếu bạn gọi tool, CHỈ in ra thẻ XML, không giải thích gì thêm cho đến khi nhận được <tool_result>. Hãy luôn dùng tiếng Việt.";

    public static void sendToAI(String userInput) {
        if (userInput != null) {
            SessionManager.addMessage("user", userInput);
        }

        CompletableFuture.supplyAsync(AiProcessor::callLlmApi)
            .thenAcceptAsync(AiProcessor::handleAiResponse, Minecraft.getInstance()); // Chạy kết quả trên Main Thread
    }

    private static String callLlmApi() {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", ConfigManager.getConfig().model);
            requestBody.addProperty("temperature", 1.0);
            requestBody.addProperty("top_p", 0.9);
            requestBody.addProperty("max_tokens", 4000);
            requestBody.addProperty("stream", false);

            JsonArray messages = new JsonArray();
            JsonObject sysMsg = new JsonObject();
            sysMsg.addProperty("role", "system");
            sysMsg.addProperty("content", SYSTEM_PROMPT);
            messages.add(sysMsg);

            for (ChatMessage msg : SessionManager.getCurrentSession().messages) {
                JsonObject m = new JsonObject();
                m.addProperty("role", msg.role);
                m.addProperty("content", msg.content);
                messages.add(m);
            }
            requestBody.add("messages", messages);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ConfigManager.getConfig().baseUrl))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + ConfigManager.getConfig().apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(requestBody)))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();

            try {
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                if (jsonResponse.has("choices")) {
                    return jsonResponse.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
                } else {
                    return "Lỗi từ API (HTTP " + response.statusCode() + "): " + responseBody;
                }
            } catch (Exception parseEx) {
                return "Lỗi phản hồi API (HTTP " + response.statusCode() + "): " + responseBody;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi kết nối mạng: " + e.getMessage();
        }
    }

    private static void handleAiResponse(String response) {
        if (response.contains("<call_tool")) {
            ToolHandler.processToolCall(response);
        } else {
            SessionManager.addMessage("assistant", response);
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(Component.literal("§e[OmniCraft]: §f" + response), false);
            }
        }
    }
}