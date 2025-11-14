package com.MediConnect.socialmedia.repository;

import com.MediConnect.socialmedia.entity.MedicalPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalPostCommentRepository extends JpaRepository<MedicalPostComment, Long> {
    List<MedicalPostComment> findByPostIdOrderByCreatedAtDesc(Long postId);
    
    /**
     * Batch fetch: Gets comment counts for multiple posts in a single query.
     * Returns a list of Object arrays where [0] = postId (Long), [1] = count (Long).
     * 
     * @param postIds List of post IDs to get comment counts for
     * @return List of Object arrays [postId, count]
     */
    @Query("SELECT c.post.id, COUNT(c) FROM MedicalPostComment c WHERE c.post.id IN :postIds GROUP BY c.post.id")
    List<Object[]> countCommentsByPostIds(@Param("postIds") List<Long> postIds);
}
