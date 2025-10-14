package com.MediConnect.socialmedia.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Getter
@Setter
public class CommentReplyLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reply_id", nullable = false)
    private CommentReply reply;

    private Long likeGiverId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt = new Date();
}

