package com.MediConnect.socialmedia.service.post.impl;

import com.MediConnect.EntryRelated.entities.HealthcareProvider;
import com.MediConnect.EntryRelated.repository.HealthcareProviderRepo;
import com.MediConnect.socialmedia.dto.CreatePostRequestDTO;
import com.MediConnect.socialmedia.entity.MedicalPost;
import com.MediConnect.socialmedia.repository.MedicalPostRepository;
import com.MediConnect.socialmedia.service.post.MedicalPostService;
import com.MediConnect.socialmedia.service.post.mapper.PostMapStructRelated;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MedicalPostServiceImpl implements MedicalPostService {
    private final MedicalPostRepository medicalPostRepository;
    private final PostMapStructRelated postMapStructRelated;
    private final HealthcareProviderRepo healthcareProviderRepo;

    @Override
    public void saveMedicalPost(CreatePostRequestDTO requestDTO) {
        MedicalPost medicalPost = postMapStructRelated.createPostRequestDTOToMedicalPost(requestDTO);
        HealthcareProvider healthcareProvider = healthcareProviderRepo.findById(requestDTO.getProviderId()).get();
        medicalPost.setPostProvider(healthcareProvider);
        medicalPostRepository.save(medicalPost);
    }
}
