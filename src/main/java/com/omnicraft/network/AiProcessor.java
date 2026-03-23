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

    private static final String SYSTEM_PROMPT = "Bạn là OmniCraft AI, trợ lý Minecraft. Bạn BẮT BUỘC dùng các công cụ dưới đây bằng thẻ XML khi cần thiết:\n" +
            "1. Lấy túi đồ: <call_tool name=\"get_inventory\"></call_tool>\n" +
            "2. Tra công thức: <call_tool name=\"get_recipe\"><item>minecraft:tên_vật_phẩm</item></call_tool>\n" +
            "3. Bật UI theo dõi: <call_tool name=\"set_todo_hud\"><item>Tên Nhiệm Vụ</item><req>minecraft:tên_vật_phẩm:số_lượng</req></call_tool>\n" +
            "(Lưu ý quan trọng: thẻ <req> phải đúng chuẩn định dạng ID:SốLượng, ví dụ: minecraft:oak_planks:4. Nếu yêu cầu nhóm vật liệu chung như bất kỳ loại gỗ nào, hãy dùng thẻ TAG bắt đầu bằng '#', ví dụ: #minecraft:planks:4, #minecraft:logs:2. Phân cách bằng dấu phẩy nếu có nhiều món).\n" +
            "KHÔNG được bịa rằng đã bật HUD nếu chưa xuất thẻ XML. Nếu xuất XML, CHỈ in XML, tuyệt đối không giải thích thêm cho đến khi có <tool_result>. Luôn nói tiếng Việt.";

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
                String[] lines = response.split("\n");
                for (String line : lines) {
                    if (!line.trim().isEmpty()) {
                        Minecraft.getInstance().player.displayClientMessage(Component.literal("§e[OmniCraft]: §f" + line.trim().replace("**", "")), false);
                    }
                }
            }
        }
    }
}