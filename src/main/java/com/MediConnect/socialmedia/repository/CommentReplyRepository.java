package com.MediConnect.socialmedia.repository;

import com.MediConnect.socialmedia.entity.CommentReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentReplyRepository extends JpaRepository<CommentReply, Long> {
    
    @Query("SELECT r FROM CommentReply r WHERE r.comment.id = :commentId ORDER BY r.createdAt ASC")
    List<CommentReply> findByCommentIdOrderByCreatedAtAsc(@Param("commentId") Long commentId);
    
    @Query("SELECT COUNT(r) FROM CommentReply r WHERE r.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);
}

