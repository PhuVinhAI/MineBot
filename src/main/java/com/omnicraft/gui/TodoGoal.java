package com.omnicraft.gui;

import java.util.ArrayList;
import java.util.List;

public class TodoGoal {
    public String name;
    public List<TodoItem> items = new ArrayList<>();

    public TodoGoal(String name) {
        this.name = name;
    }

    public boolean isComplete() {
        if (items.isEmpty()) return false;
        for (TodoItem item : items) {
            if (item.currentCount < item.requiredCount) return false;
        }
        return true;
    }
}
