package com.MediConnect.EntryRelated.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;


@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String medicationName;

    @Column(nullable = false)
    private String medicationDosage;

    @Column(nullable = false)
    private String medicationFrequency;

    @Temporal(TemporalType.DATE)
    private Date medicationStartDate;

    @Temporal(TemporalType.DATE)
    private Date medicationEndDate;

    private boolean inUse;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;
}