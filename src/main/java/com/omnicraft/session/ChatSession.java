package com.omnicraft.session;

import java.util.ArrayList;
import java.util.List;

public class ChatSession {
    public String id;
    public String name;
    public long lastModified;
    public List<ChatMessage> messages = new ArrayList<>();
}