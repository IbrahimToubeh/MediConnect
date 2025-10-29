package com.MediConnect.EntryRelated.entities.enums;

public enum PhysicalActivity {
    Sedentary("Little to no exercise"), Lightly_Active("light exercise 1-3 days / week"),
    Moderately_Active("moderate exercise 3-5 days/week"), Very_Active("intense exercise 6-7 days / week"), Athlete("High performance professional training");
    private String label;

    PhysicalActivity(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
