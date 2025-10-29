package com.MediConnect.EntryRelated.entities.enums;

import lombok.Getter;

@Getter
public enum DietaryHabits {

    BALANCED_DIET("A healthy mix of all food groups in appropriate portions."),
    VEGETARIAN("Excludes meat and fish; may include dairy and eggs."),
    VEGAN("Avoids all animal products including meat, dairy, and eggs."),
    PEDESTRIAN("Primarily vegetarian with the inclusion of fish."),
    KETO_LOW_CARB("Low in carbohydrates, high in fats, often used for weight loss."),
    GLUTEN_FREE("Excludes foods containing gluten such as wheat, barley, and rye."),
    HIGH_PROTEIN("Focused on high protein intake, often for muscle gain or fitness."),
    Prefer_Not_To_Say("N/A"),
    Other("N/A");

    private final String label;

    DietaryHabits(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
