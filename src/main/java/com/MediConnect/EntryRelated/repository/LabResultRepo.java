package com.MediConnect.EntryRelated.repository;

import com.MediConnect.EntryRelated.entities.LaboratoryResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LabResultRepo extends JpaRepository<LaboratoryResult, Long> {

    List<LaboratoryResult> findByPatientId(Long patientId);
}
