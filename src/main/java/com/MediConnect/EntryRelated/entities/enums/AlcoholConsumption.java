package com.MediConnect.EntryRelated.entities.enums;

public enum AlcoholConsumption {
    Never_Drinks, Occasionally_Drinks("Social Drinking"), Regularly_Drinks("Weekly"), Heavy_Drinker, Former_Drinker;

    private final String label;

    AlcoholConsumption(String label) {
        this.label = label;
    }

    AlcoholConsumption() {
        this.label = "N/A";
    }

    @Override
    public String toString() {
        return label;
    }
}
