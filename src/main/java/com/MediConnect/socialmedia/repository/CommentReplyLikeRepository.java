package com.MediConnect.socialmedia.repository;

import com.MediConnect.socialmedia.entity.CommentReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentReplyLikeRepository extends JpaRepository<CommentReplyLike, Long> {
    
    @Query("SELECT COUNT(l) FROM CommentReplyLike l WHERE l.reply.id = :replyId")
    long countByReplyId(@Param("replyId") Long replyId);
    
    @Query("SELECT l FROM CommentReplyLike l WHERE l.reply.id = :replyId AND l.likeGiverId = :userId")
    List<CommentReplyLike> findByReplyIdAndLikeGiverId(@Param("replyId") Long replyId, @Param("userId") Long userId);
    
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM CommentReplyLike l WHERE l.reply.id = :replyId AND l.likeGiverId = :userId")
    int deleteByReplyIdAndLikeGiverId(@Param("replyId") Long replyId, @Param("userId") Long userId);
    
    @Query("SELECT l FROM CommentReplyLike l WHERE l.reply.id = :replyId")
    List<CommentReplyLike> findByReplyId(@Param("replyId") Long replyId);
}

