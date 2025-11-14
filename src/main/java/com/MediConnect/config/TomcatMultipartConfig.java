package com.MediConnect.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Field;

/**
 * Configuration to customize Tomcat's multipart file upload limits.
 * 
 * This addresses the FileCountLimitExceededException when uploading multiple files.
 * Uses Unsafe API to modify the final MAX_FILE_COUNT field in Tomcat's FileUploadBase class.
 */
@Configuration
public class TomcatMultipartConfig {

    static {
        // Set system property first (in case it's checked)
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileCountLimit", "200");
        
        // Use Unsafe to modify the final field
        try {
            Class<?> fileUploadBaseClass = Class.forName("org.apache.tomcat.util.http.fileupload.FileUploadBase");
            Field maxFileCountField = fileUploadBaseClass.getDeclaredField("MAX_FILE_COUNT");
            
            // Get Unsafe instance
            Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            
            // Get the static field offset
            Object base = unsafe.staticFieldBase(maxFileCountField);
            long offset = unsafe.staticFieldOffset(maxFileCountField);
            
            // Read current value
            int currentValue = unsafe.getInt(base, offset);
            System.out.println("Current Tomcat file count limit: " + currentValue);
            
            // Modify the final field using Unsafe
            unsafe.putInt(base, offset, 200);
            
            // Verify the change
            int newValue = unsafe.getInt(base, offset);
            System.out.println("✓ Successfully updated Tomcat file count limit to: " + newValue);
            
        } catch (Exception e) {
            System.err.println("Warning: Could not modify Tomcat file count limit: " + e.getMessage());
            System.err.println("Attempting alternative method...");
            
            // Fallback: Try reflection with modifiers field (works in older Java versions)
            try {
                Class<?> fileUploadBaseClass = Class.forName("org.apache.tomcat.util.http.fileupload.FileUploadBase");
                Field maxFileCountField = fileUploadBaseClass.getDeclaredField("MAX_FILE_COUNT");
                maxFileCountField.setAccessible(true);
                
                // Try to remove final modifier
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                int modifiers = maxFileCountField.getModifiers();
                modifiersField.setInt(maxFileCountField, modifiers & ~java.lang.reflect.Modifier.FINAL);
                
                maxFileCountField.setInt(null, 200);
                System.out.println("✓ Updated Tomcat file count limit to: 200 (via reflection fallback)");
            } catch (Exception e2) {
                System.err.println("Error: Both methods failed. Please use JVM argument: -Dorg.apache.tomcat.util.http.fileupload.FileCountLimit=200");
            }
        }
    }

    /**
     * Customizes the embedded Tomcat server to increase the file count limit
     * for multipart requests.
     * 
     * @return WebServerFactoryCustomizer that sets connector properties
     */
    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatMultipartCustomizer() {
        return factory -> {
            // Configure connector properties for multipart handling
            factory.addConnectorCustomizers((Connector connector) -> {
                // Maximum POST size (100MB in bytes - matches max-request-size)
                connector.setProperty("maxPostSize", "104857600");
                // Maximum number of parameters (increase to handle multiple files)
                connector.setProperty("maxParameterCount", "10000");
                // Ensure parseBodyMethods includes POST
                connector.setProperty("parseBodyMethods", "POST,PUT,PATCH,DELETE");
            });
            
            // Also try to set it via context
            factory.addContextCustomizers(context -> {
                // Set multipart configuration
                context.setAllowCasualMultipartParsing(true);
            });
        };
    }
}

