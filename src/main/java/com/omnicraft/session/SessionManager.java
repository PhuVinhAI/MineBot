package com.omnicraft.session;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SessionManager {
    private static final Path SESSION_DIR = FMLPaths.GAMEDIR.get().resolve("omnicraft_ai/sessions");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ChatSession currentSession;

    public static void init() {
        try {
            Files.createDirectories(SESSION_DIR);
            List<ChatSession> sessions = getSessions();
            if (sessions.isEmpty()) {
                createNewSession("Default");
            } else {
                sessions.sort((a, b) -> Long.compare(b.lastModified, a.lastModified));
                currentSession = sessions.get(0);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createNewSession(String name) {
        currentSession = new ChatSession();
        currentSession.id = UUID.randomUUID().toString().substring(0, 8);
        currentSession.name = name != null ? name : "Session-" + currentSession.id;
        currentSession.lastModified = System.currentTimeMillis();
        saveCurrentSession();
    }

    public static void saveCurrentSession() {
        if (currentSession == null) return;
        currentSession.lastModified = System.currentTimeMillis();
        Path file = SESSION_DIR.resolve(currentSession.id + ".json");
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(currentSession, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<ChatSession> getSessions() {
        List<ChatSession> sessions = new ArrayList<>();
        try {
            if (!Files.exists(SESSION_DIR)) return sessions;
            Files.list(SESSION_DIR)
                .filter(p -> p.toString().endsWith(".json"))
                .forEach(p -> {
                    try (Reader reader = Files.newBufferedReader(p)) {
                        ChatSession session = GSON.fromJson(reader, ChatSession.class);
                        if (session != null && session.id != null) {
                            sessions.add(session);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sessions;
    }

    public static boolean switchSession(String id) {
        Path file = SESSION_DIR.resolve(id + ".json");
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                ChatSession session = GSON.fromJson(reader, ChatSession.class);
                if (session != null) {
                    currentSession = session;
                    return true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean deleteSession(String id) {
        Path file = SESSION_DIR.resolve(id + ".json");
        try {
            return Files.deleteIfExists(file);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void addMessage(String role, String content) {
        if (currentSession == null) createNewSession("Auto-Recovery");
        currentSession.messages.add(new ChatMessage(role, content));
        saveCurrentSession();
    }

    public static ChatSession getCurrentSession() {
        return currentSession;
    }
}