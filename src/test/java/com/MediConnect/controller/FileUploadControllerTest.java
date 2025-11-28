package com.MediConnect.controller;

import com.MediConnect.socialmedia.service.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class FileUploadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CloudinaryService cloudinaryService;

    private FileUploadController fileUploadController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        fileUploadController = new FileUploadController(cloudinaryService);
        mockMvc = MockMvcBuilders.standaloneSetup(fileUploadController).build();
    }

    @Test
    public void testUploadDocument() throws Exception {
        // Mock the Cloudinary service response
        String expectedUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/mediconnect/documents/license.pdf";
        when(cloudinaryService.uploadFile(any(), eq("mediconnect/documents"))).thenReturn(expectedUrl);

        // Create a mock PDF file
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "license.pdf", 
            "application/pdf", 
            "dummy content".getBytes()
        );

        // Perform the POST request
        mockMvc.perform(multipart("/api/upload")
                .file(file)
                .param("type", "DOCUMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.url").value(expectedUrl))
                .andExpect(jsonPath("$.type").value("DOCUMENT"));
    }

    @Test
    public void testUploadProfilePicture() throws Exception {
        // Mock the Cloudinary service response
        String expectedUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/mediconnect/profiles/avatar.jpg";
        when(cloudinaryService.uploadFile(any(), eq("mediconnect/profiles"))).thenReturn(expectedUrl);

        // Create a mock Image file
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "avatar.jpg", 
            "image/jpeg", 
            "dummy content".getBytes()
        );

        // Perform the POST request
        mockMvc.perform(multipart("/api/upload")
                .file(file)
                .param("type", "PROFILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.url").value(expectedUrl));
    }
}
