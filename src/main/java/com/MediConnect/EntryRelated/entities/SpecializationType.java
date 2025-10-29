package com.MediConnect.EntryRelated.entities;

import lombok.Getter;

@Getter
public enum SpecializationType {
    INTERNAL_MEDICINE("Internal Medicine and its subspecialties"),
    GENERAL_SURGERY("Surgical treatment of a wide range of conditions"),
    PEDIATRICS("Healthcare for infants, children, and adolescents"),
    OBSTETRICS_GYNECOLOGY("Women's reproductive health"),
    FAMILY_MEDICINE("Comprehensive healthcare for all ages"),
    PSYCHIATRY("Mental health and behavioral conditions"),
    EMERGENCY_MEDICINE("Immediate decision-making and action for acute illnesses/injuries"),
    ANESTHESIOLOGY("Pain relief and anesthesia for surgical procedures"),
    RADIOLOGY("Medical imaging for diagnosis"),
    PATHOLOGY("Study and diagnosis of disease"),
    ORTHOPEDIC_SURGERY("Surgery of the bones and joints"),
    NEUROSURGERY("Surgery of the brain and nervous system"),
    CARDIOLOGY("Heart and cardiovascular care"),
    DERMATOLOGY("Skin conditions"),
    NEUROLOGY("Nervous system disorders"),
    UROLOGY("Urinary tract and male reproductive organs"),
    PLASTIC_SURGERY("Reconstructive and cosmetic surgery"),
    OPHTHALMOLOGY("Eye and vision care");

    private final String description;

    SpecializationType(String description) {
        this.description = description;
    }
}
