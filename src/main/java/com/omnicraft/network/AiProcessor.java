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
            "2. Tra công thức: <call_tool name=\"get_recipe\"><item>minecraft:id_vật_phẩm</item></call_tool>\n" +
            "3. Tạo Sách Tiến Trình: <call_tool name=\"set_todo_hud\"><item>Tên Nhiệm Vụ (Hiển thị đẹp)</item><req>minecraft:id_vật_phẩm:số_lượng</req></call_tool>\n" +
            "(Lưu ý quan trọng: thẻ <req> phải đúng chuẩn định dạng ID:SốLượng, ví dụ: minecraft:oak_planks:4. Nếu yêu cầu nhóm vật liệu chung như gỗ, hãy dùng TAG bắt đầu bằng '#', ví dụ: #minecraft:planks:4. Phân cách nhiều món bằng dấu phẩy).\n" +
            "Khi API trả về túi đồ hoặc công thức, tên thực tế đã dịch của vật phẩm trong game sẽ nằm trong dấu ngoặc vuông [...]. HÃY ƯU TIÊN SỬ DỤNG TÊN NÀY khi giao tiếp với người chơi để họ dễ hiểu.\n" +
            "Tool set_todo_hud sẽ đưa cho người chơi 1 cuốn Sách Nhiệm Vụ (Custom UI) để họ tự theo dõi. Hãy thông báo điều này cho họ. Khi gọi tool, CHỈ in XML, tuyệt đối không giải thích thêm cho đến khi có <tool_result>. Luôn nói tiếng Việt.";

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