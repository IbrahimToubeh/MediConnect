package com.MediConnect.socialmedia.repository;


import com.MediConnect.socialmedia.entity.MedicalPostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicalPostLikeRepository extends JpaRepository<MedicalPostLike, Long> {
}
