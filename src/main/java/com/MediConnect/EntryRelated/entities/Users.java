package com.MediConnect.EntryRelated.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Date;
// h
@Entity
@Getter
@Setter
@ToString
@Inheritance(strategy = InheritanceType.JOINED)
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(unique = true, nullable = false)
    private String email;

    private String gender;

    @Temporal(TemporalType.DATE)
    private Date dateOfBirth;

    @Temporal(TemporalType.TIMESTAMP)
    private Date registrationDate;

    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String country;
    private String zipcode;
    
    @Column(columnDefinition = "TEXT")
    private String profilePicture;
    
    @Column(columnDefinition = "TEXT")
    private String bannerPicture;
    
    @Column(name = "two_factor_enabled")
    private Boolean twoFactorEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_status", nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;
}
