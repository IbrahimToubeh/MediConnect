package com.MediConnect.EntryRelated.dto;

import lombok.Data;
import java.util.Date;

@Data
public class PatientProfileResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String gender;
    private Date registrationDate;
    private String address;
}
