package com.MediConnect.socialmedia.entity;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.socialmedia.entity.enums.PostPrivacy;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Entity
@Getter
@Setter
public class MedicalPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private HealthcareProvider postProvider;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String mediaUrl;  // image or video URL

    @Enumerated(EnumType.STRING)
    private PostPrivacy privacy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalPostRating> ratings = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalPostLike> likes = new java.util.ArrayList<>();

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicalPostComment> comments = new java.util.ArrayList<>();
}
