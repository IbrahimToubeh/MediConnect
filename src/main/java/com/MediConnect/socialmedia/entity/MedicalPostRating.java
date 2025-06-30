package com.MediConnect.socialmedia.entity;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.socialmedia.entity.enums.ContextRank;
import com.MediConnect.socialmedia.entity.enums.TruthRank;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
public class MedicalPostRating {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private MedicalPost post;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private HealthcareProvider ratingProvider;

    @Enumerated(EnumType.STRING)
    private TruthRank truthRank;

    @Enumerated(EnumType.STRING)
    private ContextRank contextRank;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
}
