package com.MediConnect.socialmedia.repository;

import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.entity.MedicalPostReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalPostReportRepository extends JpaRepository<MedicalPostReport, Long> {
    List<MedicalPostReport> findByPost(MedicalPost post);
    boolean existsByPostIdAndReporterId(Long postId, Long reporterId);
    long countByPostId(Long postId);
    
    /**
     * Batch fetch: Gets report counts for multiple posts in a single query.
     * Returns a list of Object arrays where [0] = postId (Long), [1] = count (Long).
     * 
     * @param postIds List of post IDs to get report counts for
     * @return List of Object arrays [postId, count]
     */
    @Query("SELECT r.post.id, COUNT(r) FROM MedicalPostReport r WHERE r.post.id IN :postIds GROUP BY r.post.id")
    List<Object[]> countReportsByPostIds(@Param("postIds") List<Long> postIds);
}

