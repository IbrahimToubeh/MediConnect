package com.MediConnect.socialmedia.repository;

import com.MediConnect.socialmedia.entity.MedicalPostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalPostCommentRepository extends JpaRepository<MedicalPostComment, Long> {
    List<MedicalPostComment> findByPostIdOrderByCreatedAtDesc(Long postId);
}
