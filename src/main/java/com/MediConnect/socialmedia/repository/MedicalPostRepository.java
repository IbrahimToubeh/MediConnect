package com.MediConnect.socialmedia.repository;


import com.MediConnect.socialmedia.entity.MedicalPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalPostRepository extends JpaRepository<MedicalPost, Long> {
    List<MedicalPost> findAllByOrderByCreatedAtDesc();
}
