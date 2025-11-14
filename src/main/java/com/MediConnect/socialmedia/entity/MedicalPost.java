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

    @Column(columnDefinition = "TEXT")
    private String mediaUrl;  // Single media URL (for backward compatibility)
    
    @Column(columnDefinition = "TEXT")
    private String mediaUrls;  // JSON array of media URLs (up to 10)

    private Boolean adminFlagged = false;

    @Column(columnDefinition = "TEXT")
    private String adminFlagReason;

    @Temporal(TemporalType.TIMESTAMP)
    private Date adminFlaggedAt;

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

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MedicalPostReport> reports = new java.util.ArrayList<>();
}
