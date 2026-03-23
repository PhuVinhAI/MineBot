package com.omnicraft.gui;

public class TodoItem {
    public String itemId;
    public boolean isTag;
    public String displayName;
    public int requiredCount;
    public int currentCount;

    public TodoItem(String itemId, boolean isTag, String displayName, int requiredCount) {
        this.itemId = itemId;
        this.isTag = isTag;
        this.displayName = displayName;
        this.requiredCount = requiredCount;
        this.currentCount = 0;
    }
}
