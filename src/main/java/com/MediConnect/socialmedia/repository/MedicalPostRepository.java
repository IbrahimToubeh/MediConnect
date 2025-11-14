package com.MediConnect.socialmedia.repository;


import com.MediConnect.socialmedia.entity.MedicalPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalPostRepository extends JpaRepository<MedicalPost, Long>, JpaSpecificationExecutor<MedicalPost> {
    
    /**
     * Finds all posts ordered by creation date (newest first).
     * Uses EntityGraph to eagerly fetch postProvider to avoid N+1 queries.
     * 
     * @return List of posts with postProvider eagerly loaded
     */
    @EntityGraph(attributePaths = {"postProvider"})
    @Query("SELECT p FROM MedicalPost p ORDER BY p.createdAt DESC")
    List<MedicalPost> findAllByOrderByCreatedAtDesc();
    
    /**
     * Finds all posts by a specific doctor ordered by creation date (newest first).
     * Uses EntityGraph to eagerly fetch postProvider to avoid N+1 queries.
     * 
     * @param providerId The ID of the healthcare provider
     * @return List of posts with postProvider eagerly loaded
     */
    @EntityGraph(attributePaths = {"postProvider"})
    @Query("SELECT p FROM MedicalPost p WHERE p.postProvider.id = :providerId ORDER BY p.createdAt DESC")
    List<MedicalPost> findByPostProviderIdOrderByCreatedAtDesc(Long providerId);

    /**
     * Finds all flagged posts ordered by flag date (newest first).
     * Uses EntityGraph to eagerly fetch postProvider to avoid N+1 queries.
     * 
     * @return List of flagged posts with postProvider eagerly loaded
     */
    @EntityGraph(attributePaths = {"postProvider"})
    @Query("SELECT p FROM MedicalPost p WHERE p.adminFlagged = true ORDER BY p.adminFlaggedAt DESC")
    List<MedicalPost> findByAdminFlaggedTrueOrderByAdminFlaggedAtDesc();
    
    /**
     * Finds all posts matching a specification with eager loading of postProvider.
     * This method is used for admin filtering with EntityGraph to avoid N+1 queries.
     * 
     * @param spec The specification to filter posts
     * @param sort The sort order
     * @return List of posts with postProvider eagerly loaded
     */
    @EntityGraph(attributePaths = {"postProvider"})
    List<MedicalPost> findAll(Specification<MedicalPost> spec, Sort sort);
    
    /**
     * Finds a page of posts matching a specification with eager loading of postProvider.
     * This method is used for paginated admin filtering with EntityGraph to avoid N+1 queries.
     * 
     * @param spec The specification to filter posts
     * @param pageable The pagination information
     * @return Page of posts with postProvider eagerly loaded
     */
    @EntityGraph(attributePaths = {"postProvider"})
    Page<MedicalPost> findAll(Specification<MedicalPost> spec, Pageable pageable);
    
    /**
     * Finds a page of posts with eager loading of postProvider.
     * This method is used for paginated post feeds with EntityGraph to avoid N+1 queries.
     * 
     * @param pageable The pagination information
     * @return Page of posts with postProvider eagerly loaded
     */
    @EntityGraph(attributePaths = {"postProvider"})
    Page<MedicalPost> findAll(Pageable pageable);
}
