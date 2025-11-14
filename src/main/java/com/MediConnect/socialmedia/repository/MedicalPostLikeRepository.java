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
    
    /**
     * Batch fetch: Gets like counts for multiple posts in a single query.
     * Returns a list of Object arrays where [0] = postId (Long), [1] = count (Long).
     * 
     * @param postIds List of post IDs to get like counts for
     * @return List of Object arrays [postId, count]
     */
    @Query("SELECT l.post.id, COUNT(l) FROM MedicalPostLike l WHERE l.post.id IN :postIds GROUP BY l.post.id")
    List<Object[]> countLikesByPostIds(@Param("postIds") List<Long> postIds);
    
    /**
     * Batch fetch: Gets all likes for a specific user across multiple posts in a single query.
     * Returns list of post IDs that the user has liked.
     * 
     * @param postIds List of post IDs to check
     * @param userId The user ID to check likes for
     * @return List of post IDs that the user has liked
     */
    @Query("SELECT DISTINCT l.post.id FROM MedicalPostLike l WHERE l.post.id IN :postIds AND l.likeGiverId = :userId")
    List<Long> findPostIdsLikedByUser(@Param("postIds") List<Long> postIds, @Param("userId") Long userId);
}
