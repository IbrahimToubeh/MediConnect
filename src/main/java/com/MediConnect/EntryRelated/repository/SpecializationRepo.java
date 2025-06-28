package com.MediConnect.EntryRelated.repository;

import com.MediConnect.Entities.Specialization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecializationRepo extends JpaRepository<Specialization, Integer> {
}
