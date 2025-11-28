package com.MediConnect.socialmedia.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Service for uploading images and videos to Cloudinary
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Test Cloudinary connection by verifying credentials
     * 
     * @return true if connection is successful, false otherwise
     */
    public boolean testConnection() {
        try {
            // Try to ping Cloudinary API to verify credentials
            // This will throw an exception if credentials are invalid
            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) cloudinary.api().ping(ObjectUtils.emptyMap());
            log.info("Cloudinary connection test successful: {}", result);
            return true;
        } catch (Exception e) {
            log.error("Cloudinary connection test failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Upload an image or video file to Cloudinary
     * 
     * @param file The file to upload (image or video)
     * @return The public URL of the uploaded file
     * @throws IOException If upload fails
     */
    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, "mediconnect/posts");
    }

    /**
     * Upload a file to Cloudinary with a specific folder
     * 
     * @param file The file to upload
     * @param folder The folder to upload to
     * @return The public URL of the uploaded file
     * @throws IOException If upload fails
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty");
        }

        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 10MB");
        }

        // Determine resource type based on file content type and filename
        // Use 'auto' to let Cloudinary detect file type. 
        // For PDFs, it will be treated as image-like (public) and extension added automatically.
        String resourceType = "auto"; 
        
        // Generate a unique filename with extension
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.lastIndexOf(".") > 0) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        // For PDFs, we do NOT want to force extension in public_id if using 'auto', 
        // because Cloudinary adds it automatically.
        // But for other files, we might want to keep original extension if needed.
        // Actually, for 'auto', Cloudinary usually handles extensions.
        // Let's just use UUID as public_id and let Cloudinary add extension.
        
        String publicId = java.util.UUID.randomUUID().toString();

        // Upload options
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadOptions = (Map<String, Object>) ObjectUtils.asMap(
            "resource_type", resourceType,
            "type", "upload", 
            "folder", folder, 
            "public_id", publicId,
            "format", "pdf", // Force format to PDF
            "overwrite", false
        );

        try {
            // Upload file to Cloudinary
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                file.getBytes(),
                uploadOptions
            );

            // Extract the secure URL (HTTPS)
            String url = (String) uploadResult.get("secure_url");
            
            log.info("Successfully uploaded file to Cloudinary: {}", url);
            return url;
            
        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary", e);
            throw new IOException("Failed to upload file to Cloudinary: " + e.getMessage(), e);
        }
    }

    /**
     * Delete a file from Cloudinary using its public ID or URL
     * 
     * @param publicIdOrUrl The public ID or URL of the file to delete
     * @throws IOException If deletion fails
     */
    public void deleteFile(String publicIdOrUrl) throws IOException {
        try {
            // Extract public ID from URL if a full URL is provided
            String publicId = publicIdOrUrl;
            if (publicIdOrUrl.contains("/")) {
                // Extract public ID from URL (format: https://res.cloudinary.com/cloud_name/resource_type/upload/v1234567890/folder/filename.jpg)
                String[] parts = publicIdOrUrl.split("/");
                int uploadIndex = -1;
                for (int i = 0; i < parts.length; i++) {
                    if (parts[i].equals("upload")) {
                        uploadIndex = i;
                        break;
                    }
                }
                if (uploadIndex >= 0 && uploadIndex < parts.length - 1) {
                    // Reconstruct public ID from parts after "upload"
                    StringBuilder sb = new StringBuilder();
                    for (int i = uploadIndex + 2; i < parts.length; i++) {
                        if (sb.length() > 0) sb.append("/");
                        // Remove file extension
                        String part = parts[i];
                        if (i == parts.length - 1) {
                            int dotIndex = part.lastIndexOf('.');
                            if (dotIndex > 0) {
                                part = part.substring(0, dotIndex);
                            }
                        }
                        sb.append(part);
                    }
                    publicId = sb.toString();
                }
            }

            // Delete the file
            @SuppressWarnings("unchecked")
            Map<String, Object> deleteResult = (Map<String, Object>) cloudinary.uploader().destroy(
                publicId, 
                (Map<String, Object>) ObjectUtils.emptyMap()
            );
            
            log.info("Deleted file from Cloudinary: {} (result: {})", publicId, deleteResult.get("result"));
            
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", publicIdOrUrl, e);
            throw new IOException("Failed to delete file from Cloudinary: " + e.getMessage(), e);
        }
    }
}

