package com.MediConnect.socialmedia.repository;

import com.MediConnect.socialmedia.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    
    @Query("SELECT COUNT(l) FROM CommentLike l WHERE l.comment.id = :commentId")
    long countByCommentId(@Param("commentId") Long commentId);
    
    @Query("SELECT l FROM CommentLike l WHERE l.comment.id = :commentId AND l.likeGiverId = :userId")
    List<CommentLike> findByCommentIdAndLikeGiverId(@Param("commentId") Long commentId, @Param("userId") Long userId);
    
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CommentLike l WHERE l.comment.id = :commentId AND l.likeGiverId = :userId")
    int deleteByCommentIdAndLikeGiverId(@Param("commentId") Long commentId, @Param("userId") Long userId);
    
    @Query("SELECT l FROM CommentLike l WHERE l.comment.id = :commentId")
    List<CommentLike> findByCommentId(@Param("commentId") Long commentId);
}

