package com.omnicraft.hud;

public class TodoTask {
    public String itemId;
    public int requiredCount;
    public int currentCount;

    public TodoTask(String itemId, int requiredCount) {
        this.itemId = itemId;
        this.requiredCount = requiredCount;
        this.currentCount = 0;
    }
}