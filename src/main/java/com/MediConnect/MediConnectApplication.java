package com.MediConnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

//comment testing
//todo: after picking time for appointment it becomes earlier by 3 hours for all scheduling
//TODO: VIDEO CALL / upload pdf
//todo: all done
@SpringBootApplication(scanBasePackages = "com.MediConnect")
@EnableCaching
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "com.MediConnect.EntryRelated.repository", 
    "com.MediConnect.socialmedia.repository",
    "com.MediConnect.Repos"
})

public class MediConnectApplication {

    public static void main(String[] args) {
        // Set Tomcat file upload limit before Spring Boot starts
        // This addresses FileCountLimitExceededException when uploading multiple files
        // Default limit varies, we set it to 200 to allow posts with multiple images/videos
        System.setProperty("org.apache.tomcat.util.http.fileupload.FileCountLimit", "200");
        
        SpringApplication.run(MediConnectApplication.class, args);
    }

}
