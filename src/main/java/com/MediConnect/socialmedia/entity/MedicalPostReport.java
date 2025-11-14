package com.MediConnect.socialmedia.entity;

import com.MediConnect.socialmedia.entity.enums.PostReportReason;
import com.MediConnect.socialmedia.entity.enums.PostReporterType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
public class MedicalPostReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private MedicalPost post;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PostReportReason reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostReporterType reporterType;

    @Column(nullable = false)
    private Long reporterId;

    @Column(columnDefinition = "TEXT")
    private String otherReason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date createdAt = new Date();

    @Column(nullable = false)
    private boolean reviewed = false;
}

