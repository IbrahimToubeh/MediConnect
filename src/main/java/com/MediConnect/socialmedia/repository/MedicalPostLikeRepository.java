package com.MediConnect.socialmedia.repository;


import com.MediConnect.socialmedia.entity.MedicalPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalPostLikeRepository extends JpaRepository<MedicalPostLike, Long> {
    
    @Query("SELECT l FROM MedicalPostLike l WHERE l.post.id = :postId AND l.likeGiverId = :userId")
    List<MedicalPostLike> findByPostIdAndLikeGiverId(@Param("postId") Long postId, @Param("userId") Long userId);
    
    @Query("SELECT l FROM MedicalPostLike l WHERE l.post.id = :postId AND l.likeGiverId = :userId")
    Optional<MedicalPostLike> findOneByPostIdAndLikeGiverId(@Param("postId") Long postId, @Param("userId") Long userId);
    
    @Query("SELECT l FROM MedicalPostLike l WHERE l.post.id = :postId")
    List<MedicalPostLike> findByPostId(@Param("postId") Long postId);
    
    @Query("SELECT COUNT(l) FROM MedicalPostLike l WHERE l.post.id = :postId")
    long countByPostId(@Param("postId") Long postId);
    
    @Modifying
    @Query("DELETE FROM MedicalPostLike l WHERE l.post.id = :postId AND l.likeGiverId = :userId")
    int deleteByPostIdAndLikeGiverId(@Param("postId") Long postId, @Param("userId") Long userId);
}
