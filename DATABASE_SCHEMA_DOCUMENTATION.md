# MediConnect Database Schema Documentation

**Date:** 2025-11-14  
**Version:** 1.0  
**Purpose:** Complete documentation of all database tables, their fields, and relationships

---

## Table of Contents

1. [Overview](#overview)
2. [Entity Inheritance Structure](#entity-inheritance-structure)
3. [Core Identity & User Management](#core-identity--user-management)
4. [Social Media Module](#social-media-module)
5. [Appointment & Medical Records](#appointment--medical-records)
6. [Chat & Communication](#chat--communication)
7. [Notifications](#notifications)
8. [Supporting Tables](#supporting-tables)
9. [Relationship Diagrams](#relationship-diagrams)
10. [Important Design Notes](#important-design-notes)

---

## Overview

The MediConnect database uses **PostgreSQL** and follows a relational model with:
- **Inheritance Strategy**: `JOINED` inheritance for Users → Patient/HealthcareProvider
- **Cascade Operations**: Configured for data integrity
- **Lazy Loading**: Used for most relationships to optimize performance
- **Element Collections**: Used for multi-valued attributes (specializations, insurance, availability)

**Total Tables:** ~26 main tables + helper tables for element collections

---

## Entity Inheritance Structure

```
users (base table)
├── patient (extends users)
└── healthcare_provider (extends users)
```

**Inheritance Type:** `JOINED` (Table Per Subclass)
- Base table: `users` (contains common fields)
- Subclass tables: `patient`, `healthcare_provider` (contain subtype-specific fields)
- Primary Key: `id` (shared across all tables via JOIN)

---

## Core Identity & User Management

### 1. `users` (Base Table)

**Location:** `EntryRelated/entities/Users.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Unique user identifier |
| `username` | VARCHAR | UNIQUE, NOT NULL | Username for login |
| `email` | VARCHAR | UNIQUE, NOT NULL | Email address |
| `password` | VARCHAR | NOT NULL | Hashed password |
| `role` | VARCHAR | NOT NULL | User role: `PATIENT`, `HEALTHPROVIDER`, `ADMIN` |
| `firstName` | VARCHAR | NOT NULL | First name |
| `lastName` | VARCHAR | NOT NULL | Last name |
| `gender` | VARCHAR | NULLABLE | Gender |
| `dateOfBirth` | DATE | NULLABLE | Date of birth |
| `registrationDate` | TIMESTAMP | NULLABLE | Account creation date |
| `phoneNumber` | VARCHAR | NULLABLE | Phone number |
| `address` | VARCHAR | NULLABLE | Street address |
| `city` | VARCHAR | NULLABLE | City |
| `state` | VARCHAR | NULLABLE | State/Province |
| `country` | VARCHAR | NULLABLE | Country |
| `zipcode` | VARCHAR | NULLABLE | ZIP/Postal code |
| `profilePicture` | TEXT | NULLABLE | Profile picture URL |
| `bannerPicture` | TEXT | NULLABLE | Banner picture URL |
| `twoFactorEnabled` | BOOLEAN | DEFAULT FALSE | 2FA enabled flag |
| `accountStatus` | VARCHAR | NOT NULL | Account status enum |

**Relationships:**
- One-to-One → `user_notification_preferences`
- One-to-One → `user_privacy_settings`
- One-to-Many → `login_sessions`
- One-to-Many → `account_activity`
- One-to-Many → `notifications` (as recipient)
- One-to-Many → `notifications` (as actor)
- One-to-Many → `chat_messages` (as sender)

**Notes:**
- Base table for all user types
- Uses JOINED inheritance strategy
- Administrators are users with `role = 'ADMIN'`

---

### 2. `patient` (Extends `users`)

**Location:** `EntryRelated/entities/Patient.java`

**Primary Key/Foreign Key:** `id` → `users.id`

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK/FK → users.id | Inherited from users |
| `bloodType` | VARCHAR | NULLABLE | Blood type enum |
| `height` | DOUBLE | NULLABLE | Height in cm |
| `weight` | DOUBLE | NULLABLE | Weight in kg |
| `allergies` | TEXT | NULLABLE | Allergies information |
| `medicalConditions` | TEXT | NULLABLE | Medical conditions |
| `previousSurgeries` | TEXT | NULLABLE | Previous surgeries |
| `familyMedicalHistory` | TEXT | NULLABLE | Family medical history |
| `dietaryHabits` | VARCHAR | NULLABLE | Dietary habits enum |
| `alcoholConsumption` | VARCHAR | NULLABLE | Alcohol consumption enum |
| `physicalActivity` | VARCHAR | NULLABLE | Physical activity enum |
| `smokingStatus` | VARCHAR | NULLABLE | Smoking status enum |
| `mentalHealthCondition` | VARCHAR | NULLABLE | Mental health condition enum |
| `emergencyContactName` | VARCHAR | NULLABLE | Emergency contact name |
| `emergencyContactPhone` | VARCHAR | NULLABLE | Emergency contact phone |
| `emergencyContactRelation` | VARCHAR | NULLABLE | Relationship to emergency contact |
| `insuranceProvider` | VARCHAR | NULLABLE | Insurance provider name |
| `insuranceNumber` | VARCHAR | NULLABLE | Insurance policy number |
| `adminFlagged` | BOOLEAN | DEFAULT FALSE | Admin flag status |
| `adminFlagReason` | TEXT | NULLABLE | Reason for admin flag |
| `adminFlaggedAt` | TIMESTAMP | NULLABLE | When flagged by admin |
| `listOfFollowedPeople` | List<Long> | NULLABLE | List of followed user IDs |

**Relationships:**
- **One-to-Many:**
  - → `medication` (cascade: ALL, orphanRemoval: true)
  - → `mental_health_medication` (cascade: ALL, orphanRemoval: true)
  - → `laboratory_result` (cascade: ALL, fetch: LAZY)
  - → `medical_record` (cascade: ALL, fetch: LAZY)
  - → `appointment_entity` (cascade: ALL, fetch: LAZY)
  - → `doctor_review` (cascade: ALL, fetch: LAZY)
  - → `chat_channel` (as patient)

**Notes:**
- Inherits all fields from `users` table
- Medical data is stored here
- Lifestyle information tracked for health insights

---

### 3. `healthcare_provider` (Extends `users`)

**Location:** `EntryRelated/entities/HealthcareProvider.java`

**Primary Key/Foreign Key:** `id` → `users.id`

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK/FK → users.id | Inherited from users |
| `licenseNumber` | VARCHAR | NULLABLE | Medical license number |
| `clinicName` | VARCHAR | NULLABLE | Clinic/hospital name |
| `bio` | TEXT | NULLABLE | Professional biography |
| `consultationFee` | DOUBLE | NULLABLE | Consultation fee |
| `availableTimeStart` | VARCHAR | NULLABLE | Available time start |
| `availableTimeEnd` | VARCHAR | NULLABLE | Available time end |
| `adminFlagged` | BOOLEAN | DEFAULT FALSE | Admin flag status |
| `adminFlagReason` | TEXT | NULLABLE | Reason for admin flag |
| `adminFlaggedAt` | TIMESTAMP | NULLABLE | When flagged by admin |
| `followList` | List<Long> | NULLABLE | List of followed user IDs |

**Element Collections** (stored in helper tables):
- `specializations` → `provider_specialization` table (provider_id, specialization)
- `insuranceAccepted` → `provider_insurance` table (provider_id, insurance_value)
- `availableDays` → `provider_availability` table (provider_id, available_day)

**Relationships:**
- **One-to-Many:**
  - → `education_history` (cascade: ALL, orphanRemoval: true)
  - → `work_experience` (cascade: ALL, orphanRemoval: true)
  - → `appointment_entity` (cascade: ALL, fetch: LAZY)
  - → `medical_post` (as postProvider)
  - → `medical_record` (cascade: ALL, fetch: LAZY)
  - → `doctor_review` (cascade: ALL, fetch: LAZY)
  - → `medical_post_rating` (as ratingProvider)
  - → `chat_channel` (as doctor)

**Notes:**
- Inherits all fields from `users` table
- Professional credentials and availability stored here
- Element collections allow multiple specializations, insurance types, and available days

---

### 4. `education_history`

**Location:** `EntryRelated/entities/EducationHistory.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `provider_id` → `healthcare_provider.id`

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `provider_id` | BIGINT | FK → healthcare_provider.id |
| `institutionName` | VARCHAR | Name of educational institution |
| `startDate` | DATE | Education start date |
| `endDate` | DATE | Education end date |
| `stillEnrolled` | BOOLEAN | Currently enrolled flag |

**Relationships:**
- Many-to-One → `healthcare_provider` (fetch: LAZY)

---

### 5. `work_experience`

**Location:** `EntryRelated/entities/WorkExperience.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `provider_id` → `healthcare_provider.id`

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `provider_id` | BIGINT | FK → healthcare_provider.id |
| `organizationName` | VARCHAR | Name of organization |
| `roleTitle` | VARCHAR | Job title/role |
| `startDate` | DATE | Employment start date |
| `endDate` | DATE | Employment end date |
| `stillWorking` | BOOLEAN | Currently working flag |

**Relationships:**
- Many-to-One → `healthcare_provider` (fetch: LAZY)

---

### 6. `user_notification_preferences`

**Location:** `EntryRelated/entities/UserNotificationPreferences.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `user_id` → `users.id` (UNIQUE, One-to-One)

**Fields:**
| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `id` | BIGINT | - | Primary key |
| `user_id` | BIGINT | - | FK → users.id (UNIQUE) |
| `emailNotifications` | BOOLEAN | TRUE | Email notifications enabled |
| `pushNotifications` | BOOLEAN | TRUE | Push notifications enabled |
| `postLikes` | BOOLEAN | TRUE | Post like notifications |
| `postComments` | BOOLEAN | TRUE | Post comment notifications |
| `commentReplies` | BOOLEAN | TRUE | Comment reply notifications |
| `appointmentReminders` | BOOLEAN | TRUE | Appointment reminder notifications |
| `prescriptionUpdates` | BOOLEAN | TRUE | Prescription update notifications |
| `labResults` | BOOLEAN | TRUE | Lab result notifications |
| `medicationReminders` | BOOLEAN | TRUE | Medication reminder notifications |
| `securityAlerts` | BOOLEAN | TRUE | Security alert notifications |
| `loginAlerts` | BOOLEAN | TRUE | Login alert notifications |
| `passwordChangeAlerts` | BOOLEAN | TRUE | Password change alerts |
| `systemUpdates` | BOOLEAN | TRUE | System update notifications |
| `maintenanceAlerts` | BOOLEAN | TRUE | Maintenance alert notifications |
| `createdAt` | TIMESTAMP | NOW | Creation timestamp |
| `updatedAt` | TIMESTAMP | NOW | Last update timestamp |

**Relationships:**
- One-to-One → `users`

**Notes:**
- One preference record per user
- Contains method `isNotificationEnabled(String notificationType)` for checking preferences

---

### 7. `user_privacy_settings`

**Location:** `EntryRelated/entities/UserPrivacySettings.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `user_id` → `users.id` (UNIQUE, One-to-One)

**Fields:**
| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `id` | BIGINT | - | Primary key |
| `user_id` | BIGINT | - | FK → users.id (UNIQUE) |
| `profileVisibility` | VARCHAR | "public" | Profile visibility: "public" or "private" |
| `showEmail` | BOOLEAN | FALSE | Show email in profile |
| `showPhone` | BOOLEAN | FALSE | Show phone in profile |
| `showAddress` | BOOLEAN | FALSE | Show address in profile |
| `showMedicalHistory` | BOOLEAN | FALSE | Show medical history |
| `createdAt` | TIMESTAMP | NOW | Creation timestamp |
| `updatedAt` | TIMESTAMP | NOW | Last update timestamp |

**Relationships:**
- One-to-One → `users`

---

### 8. `login_sessions`

**Location:** `EntryRelated/entities/LoginSession.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `user_id` → `users.id`

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `user_id` | BIGINT | FK → users.id |
| `sessionToken` | VARCHAR | Session token hash |
| `isActive` | BOOLEAN | Active session flag |
| `device` | VARCHAR | Device information |
| `browser` | VARCHAR | Browser information |
| `loginTime` | TIMESTAMP | Login timestamp |
| `logoutTime` | TIMESTAMP | Logout timestamp |

**Relationships:**
- Many-to-One → `users`

---

### 9. `account_activity`

**Location:** `EntryRelated/entities/AccountActivity.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `user_id` → `users.id`

**Fields:**
| Field | Type | Description |
|-------|------|-------------|
| `id` | BIGINT | Primary key |
| `user_id` | BIGINT | FK → users.id |
| `activityType` | VARCHAR | Type of activity |
| `description` | TEXT | Activity description |
| `timestamp` | TIMESTAMP | Activity timestamp |
| `ipAddress` | VARCHAR | IP address |
| `device` | VARCHAR | Device information |

**Relationships:**
- Many-to-One → `users`

---

## Social Media Module

### 10. `medical_post`

**Location:** `socialmedia/entity/MedicalPost.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `provider_id` → `healthcare_provider.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Post identifier |
| `provider_id` | BIGINT | FK, NOT NULL | FK → healthcare_provider.id |
| `content` | TEXT | NULLABLE | Post content/text |
| `mediaUrl` | TEXT | NULLABLE | Single media URL (backward compatibility) |
| `mediaUrls` | TEXT | NULLABLE | JSON array of media URLs (up to 10) |
| `adminFlagged` | BOOLEAN | DEFAULT FALSE | Admin flag status |
| `adminFlagReason` | TEXT | NULLABLE | Reason for admin flag |
| `adminFlaggedAt` | TIMESTAMP | NULLABLE | When flagged by admin |
| `privacy` | VARCHAR | NULLABLE | Post privacy enum |
| `createdAt` | TIMESTAMP | DEFAULT NOW | Post creation timestamp |

**Relationships:**
- **Many-to-One:**
  - → `healthcare_provider` (postProvider, NOT NULL, eagerly loaded via @EntityGraph)
- **One-to-Many** (cascade: ALL, orphanRemoval: true):
  - → `medical_post_like`
  - → `medical_post_comment`
  - → `medical_post_rating`
  - → `medical_post_report` (fetch: LAZY)

**Notes:**
- Only healthcare providers can create posts
- Supports multiple media files (up to 10) via JSON array
- Cascade deletion: deleting post deletes all likes, comments, ratings, and reports

---

### 11. `medical_post_like`

**Location:** `socialmedia/entity/MedicalPostLike.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `post_id` → `medical_post.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Like identifier |
| `post_id` | BIGINT | FK, NOT NULL | FK → medical_post.id |
| `likeGiverId` | BIGINT | NOT NULL | User ID who liked (NOT FK) |
| `createdAt` | TIMESTAMP | DEFAULT NOW | Like timestamp |

**Relationships:**
- Many-to-One → `medical_post`

**Notes:**
- Uses `likeGiverId` (Long) instead of FK to `users` table
- Allows both patients and doctors to like posts
- Cascade deletion: deleted when post is deleted

---

### 12. `medical_post_comment`

**Location:** `socialmedia/entity/MedicalPostComment.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `post_id` → `medical_post.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Comment identifier |
| `post_id` | BIGINT | FK, NOT NULL | FK → medical_post.id |
| `commenterId` | BIGINT | NOT NULL | User ID who commented (NOT FK) |
| `content` | TEXT | NOT NULL | Comment text |
| `createdAt` | TIMESTAMP | DEFAULT NOW | Comment timestamp |

**Relationships:**
- **Many-to-One:**
  - → `medical_post`
- **One-to-Many** (cascade: ALL, orphanRemoval: true):
  - → `comment_like`
  - → `comment_reply`

**Notes:**
- Uses `commenterId` (Long) instead of FK to `users` table
- Cascade deletion: deleted when post is deleted
- Cascade deletion: deleting comment deletes all likes and replies

---

### 13. `comment_like`

**Location:** `socialmedia/entity/CommentLike.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `comment_id` → `medical_post_comment.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Like identifier |
| `comment_id` | BIGINT | FK, NOT NULL | FK → medical_post_comment.id |
| `likeGiverId` | BIGINT | NOT NULL | User ID who liked (NOT FK) |
| `createdAt` | TIMESTAMP | DEFAULT NOW | Like timestamp |

**Relationships:**
- Many-to-One → `medical_post_comment`

**Notes:**
- Uses `likeGiverId` (Long) instead of FK to `users` table
- Cascade deletion: deleted when comment is deleted

---

### 14. `comment_reply`

**Location:** `socialmedia/entity/CommentReply.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `comment_id` → `medical_post_comment.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Reply identifier |
| `comment_id` | BIGINT | FK, NOT NULL | FK → medical_post_comment.id |
| `replierId` | BIGINT | NOT NULL | User ID who replied (NOT FK) |
| `content` | TEXT | NOT NULL | Reply text |
| `createdAt` | TIMESTAMP | DEFAULT NOW | Reply timestamp |

**Relationships:**
- **Many-to-One:**
  - → `medical_post_comment`
- **One-to-Many** (cascade: ALL, orphanRemoval: true):
  - → `comment_reply_like`

**Notes:**
- Uses `replierId` (Long) instead of FK to `users` table
- Cascade deletion: deleted when comment is deleted
- Cascade deletion: deleting reply deletes all reply likes

---

### 15. `comment_reply_like`

**Location:** `socialmedia/entity/CommentReplyLike.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `reply_id` → `comment_reply.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Like identifier |
| `reply_id` | BIGINT | FK, NOT NULL | FK → comment_reply.id |
| `likeGiverId` | BIGINT | NOT NULL | User ID who liked (NOT FK) |
| `createdAt` | TIMESTAMP | DEFAULT NOW | Like timestamp |

**Relationships:**
- Many-to-One → `comment_reply`

**Notes:**
- Uses `likeGiverId` (Long) instead of FK to `users` table
- Cascade deletion: deleted when reply is deleted

---

### 16. `medical_post_rating`

**Location:** `socialmedia/entity/MedicalPostRating.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Keys:**
- `post_id` → `medical_post.id` (NOT NULL)
- `provider_id` → `healthcare_provider.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Rating identifier |
| `post_id` | BIGINT | FK, NOT NULL | FK → medical_post.id |
| `provider_id` | BIGINT | FK, NOT NULL | FK → healthcare_provider.id |
| `truthRank` | VARCHAR | NULLABLE | Truth rank enum |
| `contextRank` | VARCHAR | NULLABLE | Context rank enum |
| `comment` | TEXT | NULLABLE | Rating comment |
| `createdAt` | TIMESTAMP | DEFAULT NOW | Rating timestamp |

**Relationships:**
- Many-to-One → `medical_post`
- Many-to-One → `healthcare_provider` (ratingProvider)

**Notes:**
- Only healthcare providers can rate posts
- Cascade deletion: deleted when post is deleted

---

### 17. `medical_post_report`

**Location:** `socialmedia/entity/MedicalPostReport.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `post_id` → `medical_post.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Report identifier |
| `post_id` | BIGINT | FK, NOT NULL | FK → medical_post.id |
| `reason` | VARCHAR | NOT NULL | Report reason enum |
| `reporterType` | VARCHAR | NOT NULL | Reporter type enum (PATIENT/DOCTOR) |
| `reporterId` | BIGINT | NOT NULL | User ID who reported (NOT FK) |
| `otherReason` | TEXT | NULLABLE | Additional reason details |
| `details` | TEXT | NULLABLE | Report details |
| `createdAt` | TIMESTAMP | DEFAULT NOW | Report timestamp |
| `reviewed` | BOOLEAN | DEFAULT FALSE | Admin review status |

**Relationships:**
- Many-to-One → `medical_post` (cascade: ALL, orphanRemoval: true, fetch: LAZY)

**Notes:**
- Uses `reporterId` (Long) instead of FK to `users` table
- Cascade deletion: deleted when post is deleted
- Tracks whether admin has reviewed the report

---

## Appointment & Medical Records

### 18. `appointment_entity`

**Location:** `Entities/AppointmentEntity.java`

**Primary Key:** `id` (INT, auto-increment)

**Foreign Keys:**
- `patient_id` → `patient.id` (NOT NULL)
- `provider_id` → `healthcare_provider.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | INT | PK, AUTO_INCREMENT | Appointment identifier |
| `patient_id` | BIGINT | FK, NOT NULL | FK → patient.id |
| `provider_id` | BIGINT | FK, NOT NULL | FK → healthcare_provider.id |
| `appointmentDateTime` | TIMESTAMP | NULLABLE | Appointment date and time |
| `status` | VARCHAR | DEFAULT PENDING | Appointment status enum |
| `type` | VARCHAR | NULLABLE | Appointment type enum |
| `reason` | TEXT | NULLABLE | Appointment reason |
| `notes` | TEXT | NULLABLE | Additional notes |
| `durationMinutes` | INTEGER | DEFAULT 30 | Appointment duration |
| `shareMedicalRecords` | BOOLEAN | DEFAULT FALSE | Share medical records flag |
| `isVideoCall` | BOOLEAN | DEFAULT FALSE | Video call appointment flag |
| `isCallActive` | BOOLEAN | DEFAULT FALSE | Active call flag |
| `reminder24hSent` | BOOLEAN | DEFAULT FALSE | 24h reminder sent flag |
| `createdAt` | TIMESTAMP | AUTO | Creation timestamp |
| `updatedAt` | TIMESTAMP | AUTO | Last update timestamp |

**Relationships:**
- **Many-to-One:**
  - → `patient` (fetch: LAZY)
  - → `healthcare_provider` (fetch: LAZY)
- **One-to-Many:**
  - → `doctor_review`
  - → `chat_channel`

**Notes:**
- Primary key is INT (not BIGINT like other tables)
- Tracks video call status and reminders
- Cascade deletion handled by Patient/Provider entities

---

### 19. `doctor_review`

**Location:** `EntryRelated/entities/DoctorReview.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Keys:**
- `appointment_id` → `appointment_entity.id` (NOT NULL)
- `patient_id` → `patient.id` (NOT NULL)
- `doctor_id` → `healthcare_provider.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Review identifier |
| `appointment_id` | INT | FK, NOT NULL | FK → appointment_entity.id |
| `patient_id` | BIGINT | FK, NOT NULL | FK → patient.id |
| `doctor_id` | BIGINT | FK, NOT NULL | FK → healthcare_provider.id |
| `rating` | INTEGER | NOT NULL | Rating (1-5 stars) |
| `notes` | TEXT | NULLABLE | Review notes/feedback |
| `createdAt` | TIMESTAMP | AUTO | Review creation timestamp |

**Relationships:**
- Many-to-One → `appointment_entity` (fetch: LAZY)
- Many-to-One → `patient` (fetch: LAZY)
- Many-to-One → `healthcare_provider` (fetch: LAZY)

**Notes:**
- Only completed appointments can be reviewed
- Rating scale: 1-5 stars
- Links appointment, patient, and doctor

---

### 20. `medical_record`

**Location:** `EntryRelated/entities/MedicalRecord.java`

**Primary Key:** `id` (INT, auto-increment)

**Foreign Keys:**
- `patient_id` → `patient.id` (NOT NULL)
- `provider_id` → `healthcare_provider.id` (NOT NULL)
- `appointment_id` → `appointment_entity.id` (NULLABLE, One-to-One)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | INT | PK, AUTO_INCREMENT | Record identifier |
| `patient_id` | BIGINT | FK, NOT NULL | FK → patient.id |
| `provider_id` | BIGINT | FK, NOT NULL | FK → healthcare_provider.id |
| `appointment_id` | INT | FK, NULLABLE | FK → appointment_entity.id (One-to-One) |
| `visitDate` | TIMESTAMP | NULLABLE | Visit date |
| `diagnosis` | TEXT | NULLABLE | Diagnosis |
| `treatment` | TEXT | NULLABLE | Treatment plan |
| `prescription` | TEXT | NULLABLE | Prescription details |
| `symptoms` | TEXT | NULLABLE | Symptoms |
| `notes` | TEXT | NULLABLE | Additional notes |
| `temperature` | DOUBLE | NULLABLE | Body temperature |
| `bloodPressure` | VARCHAR | NULLABLE | Blood pressure |
| `heartRate` | INTEGER | NULLABLE | Heart rate |
| `weight` | DOUBLE | NULLABLE | Weight at visit |
| `height` | DOUBLE | NULLABLE | Height at visit |
| `createdAt` | TIMESTAMP | AUTO | Record creation timestamp |

**Relationships:**
- Many-to-One → `patient` (fetch: LAZY)
- Many-to-One → `healthcare_provider` (fetch: LAZY)
- One-to-One → `appointment_entity` (optional)

**Notes:**
- Primary key is INT (not BIGINT)
- Can be linked to an appointment (optional)
- Contains vital signs and medical information

---

### 21. `medication`

**Location:** `EntryRelated/entities/Medication.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `patient_id` → `patient.id`

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Medication identifier |
| `patient_id` | BIGINT | FK | FK → patient.id |
| `medicationName` | VARCHAR | NOT NULL | Medication name |
| `medicationDosage` | VARCHAR | NOT NULL | Dosage information |
| `medicationFrequency` | VARCHAR | NOT NULL | Frequency of intake |
| `medicationStartDate` | DATE | NULLABLE | Start date |
| `medicationEndDate` | DATE | NULLABLE | End date |
| `inUse` | BOOLEAN | NULLABLE | Currently in use flag |

**Relationships:**
- Many-to-One → `patient` (cascade: ALL, orphanRemoval: true)

**Notes:**
- Cascade deletion: deleted when patient is deleted
- Tracks medication history for patients

---

### 22. `mental_health_medication`

**Location:** `EntryRelated/entities/MentalHealthMedication.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `patient_id` → `patient.id`

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Medication identifier |
| `patient_id` | BIGINT | FK | FK → patient.id |
| `medicationName` | VARCHAR | NOT NULL | Medication name |
| `medicationDosage` | VARCHAR | NOT NULL | Dosage information |
| `medicationFrequency` | VARCHAR | NOT NULL | Frequency of intake |
| `medicationStartDate` | DATE | NULLABLE | Start date |
| `medicationEndDate` | DATE | NULLABLE | End date |
| `inUse` | BOOLEAN | NULLABLE | Currently in use flag |

**Relationships:**
- Many-to-One → `patient` (cascade: ALL, orphanRemoval: true)

**Notes:**
- Separate table for mental health medications
- Cascade deletion: deleted when patient is deleted

---

### 23. `laboratory_result`

**Location:** `EntryRelated/entities/LaboratoryResult.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Key:** `patient_id` → `patient.id`

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Lab result identifier |
| `patient_id` | BIGINT | FK | FK → patient.id |
| `description` | VARCHAR | NULLABLE | Result description |
| `image` | BYTEA | NULLABLE | Lab result image (binary) |

**Relationships:**
- Many-to-One → `patient` (cascade: ALL, fetch: LAZY)

**Notes:**
- Stores lab result images as binary data (BYTEA in PostgreSQL)
- Cascade deletion: deleted when patient is deleted

---

## Chat & Communication

### 24. `chat_channel`

**Location:** `socialmedia/entity/ChatChannel.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Keys:**
- `patient_id` → `patient.id` (NOT NULL)
- `doctor_id` → `healthcare_provider.id` (NOT NULL)
- `appointment_id` → `appointment_entity.id` (NOT NULL)

**Unique Constraint:** (`patient_id`, `doctor_id`) - Only one channel per patient-doctor pair

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Channel identifier |
| `patient_id` | BIGINT | FK, NOT NULL | FK → patient.id |
| `doctor_id` | BIGINT | FK, NOT NULL | FK → healthcare_provider.id |
| `appointment_id` | INT | FK, NOT NULL | FK → appointment_entity.id |
| `createdAt` | TIMESTAMP | NOT NULL | Channel creation timestamp |
| `lastActivityAt` | TIMESTAMP | NOT NULL | Last message timestamp |
| `isActive` | BOOLEAN | DEFAULT TRUE | Active channel flag |

**Relationships:**
- **Many-to-One:**
  - → `patient` (fetch: LAZY)
  - → `healthcare_provider` (fetch: LAZY)
  - → `appointment_entity` (fetch: LAZY)
- **One-to-Many:**
  - → `chat_message`

**Notes:**
- Only ONE chat channel exists per patient-doctor pair (unique constraint)
- Automatically created when appointment is confirmed
- Channel remains active even after appointments are completed

---

### 25. `chat_message`

**Location:** `socialmedia/entity/ChatMessage.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Keys:**
- `channel_id` → `chat_channel.id` (NOT NULL)
- `sender_id` → `users.id` (NOT NULL)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Message identifier |
| `channel_id` | BIGINT | FK, NOT NULL | FK → chat_channel.id |
| `sender_id` | BIGINT | FK, NOT NULL | FK → users.id |
| `content` | TEXT | NOT NULL | Message content |
| `sentAt` | TIMESTAMP | NOT NULL | Message sent timestamp |
| `isRead` | BOOLEAN | DEFAULT FALSE | Read status |
| `readAt` | TIMESTAMP | NULLABLE | Read timestamp |
| `isDeleted` | BOOLEAN | DEFAULT FALSE | Soft delete flag |

**Relationships:**
- Many-to-One → `chat_channel` (fetch: LAZY)
- Many-to-One → `users` (sender, fetch: LAZY)

**Notes:**
- Uses FK to `users` (not Long ID like social media likes/comments)
- Supports soft deletion
- Tracks read status and read timestamp

---

## Notifications

### 26. `notifications`

**Location:** `socialmedia/entity/Notification.java`

**Primary Key:** `id` (BIGINT, auto-increment)

**Foreign Keys:**
- `recipient_id` → `users.id` (NOT NULL)
- `actor_id` → `users.id` (NOT NULL)
- `post_id` → `medical_post.id` (NULLABLE)
- `comment_id` → `medical_post_comment.id` (NULLABLE)

**Fields:**
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | BIGINT | PK, AUTO_INCREMENT | Notification identifier |
| `recipient_id` | BIGINT | FK, NOT NULL | FK → users.id (who receives) |
| `actor_id` | BIGINT | FK, NOT NULL | FK → users.id (who triggered) |
| `type` | VARCHAR | NOT NULL | Notification type enum |
| `post_id` | BIGINT | FK, NULLABLE | FK → medical_post.id (optional) |
| `comment_id` | BIGINT | FK, NULLABLE | FK → medical_post_comment.id (optional) |
| `message` | TEXT | NULLABLE | Notification message |
| `isRead` | BOOLEAN | DEFAULT FALSE | Read status |
| `createdAt` | TIMESTAMP | DEFAULT NOW | Creation timestamp |
| `relatedEntityId` | BIGINT | NULLABLE | Related entity ID (appointment, etc.) |

**Relationships:**
- Many-to-One → `users` (recipient)
- Many-to-One → `users` (actor)
- Many-to-One → `medical_post` (optional)
- Many-to-One → `medical_post_comment` (optional)

**Notification Types:**
- `POST_LIKE` - Someone liked a post
- `POST_COMMENT` - Someone commented on a post
- `COMMENT_LIKE` - Someone liked a comment
- `COMMENT_REPLY` - Someone replied to a comment
- `APPOINTMENT_REQUESTED` - Patient requested appointment
- `APPOINTMENT_CONFIRMED` - Doctor confirmed appointment
- `APPOINTMENT_CANCELLED` - Appointment cancelled
- `APPOINTMENT_RESCHEDULED` - Appointment rescheduled
- `APPOINTMENT_REMINDER_24H` - 24-hour reminder
- `CHAT_MESSAGE` - New chat message
- `ADMIN_POST_REPORTED` - Post reported to admin

**Notes:**
- Cached: Unread count is cached in Redis
- Async: Notification creation runs asynchronously
- Links to posts/comments when applicable

---

## Supporting Tables

### Element Collection Helper Tables

These tables are automatically created by JPA for `@ElementCollection` fields:

#### 27. `provider_specialization`
- `provider_id` (BIGINT, FK → healthcare_provider.id)
- `specialization` (VARCHAR)
- Stores multiple specializations per provider

#### 28. `provider_insurance`
- `provider_id` (BIGINT, FK → healthcare_provider.id)
- `insurance_value` (VARCHAR)
- Stores multiple insurance types accepted per provider

#### 29. `provider_availability`
- `provider_id` (BIGINT, FK → healthcare_provider.id)
- `available_day` (VARCHAR)
- Stores multiple available days per provider

---

## Relationship Diagrams

### Complete Entity Relationship Overview

```
┌─────────────────┐
│     users       │ (Base table)
│  (id, username, │
│   email, etc.)  │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
┌───▼───┐ ┌───▼──────────────┐
│patient│ │healthcare_provider│
└───┬───┘ └───┬──────────────┘
    │         │
    │         ├──→ medical_post
    │         │      ├──→ medical_post_like
    │         │      ├──→ medical_post_comment
    │         │      │      ├──→ comment_like
    │         │      │      └──→ comment_reply
    │         │      │             └──→ comment_reply_like
    │         │      ├──→ medical_post_rating
    │         │      └──→ medical_post_report
    │         │
    │         ├──→ education_history
    │         ├──→ work_experience
    │         └──→ medical_record
    │
    ├──→ medication
    ├──→ mental_health_medication
    ├──→ laboratory_result
    ├──→ medical_record
    │
    └──→ appointment_entity
           ├──→ doctor_review
           └──→ chat_channel
                  └──→ chat_message

┌─────────────────┐
│  notifications  │
│  (recipient_id) │──→ users
│  (actor_id)     │──→ users
│  (post_id)      │──→ medical_post (optional)
│  (comment_id)   │──→ medical_post_comment (optional)
└─────────────────┘

┌──────────────────────────┐
│user_notification_prefs   │──→ users (One-to-One)
└──────────────────────────┘

┌──────────────────────────┐
│user_privacy_settings     │──→ users (One-to-One)
└──────────────────────────┘
```

### Social Media Module Relationships

```
healthcare_provider
    │
    └──→ medical_post
            │
            ├──→ medical_post_like (likeGiverId: Long, NOT FK)
            │
            ├──→ medical_post_comment (commenterId: Long, NOT FK)
            │       │
            │       ├──→ comment_like (likeGiverId: Long, NOT FK)
            │       │
            │       └──→ comment_reply (replierId: Long, NOT FK)
            │               │
            │               └──→ comment_reply_like (likeGiverId: Long, NOT FK)
            │
            ├──→ medical_post_rating (provider_id: FK)
            │
            └──→ medical_post_report (reporterId: Long, NOT FK)
```

### Appointment & Medical Records Relationships

```
patient ──┐
          ├──→ appointment_entity ──→ doctor_review
healthcare_provider ──┘              │
                                     ├──→ patient
                                     └──→ healthcare_provider

patient ──┐
          ├──→ medical_record ──→ appointment_entity (One-to-One, optional)
healthcare_provider ──┘

patient ──→ medication
patient ──→ mental_health_medication
patient ──→ laboratory_result
```

### Chat Module Relationships

```
patient ──┐
          ├──→ chat_channel (UNIQUE: patient_id + doctor_id)
healthcare_provider ──┘      │
appointment_entity ───────────┘
                              │
                              └──→ chat_message ──→ users (sender)
```

---

## Important Design Notes

### 1. Inheritance Strategy

**JOINED Inheritance:**
- `users` is the base table
- `patient` and `healthcare_provider` extend `users`
- Primary key `id` is shared across all tables
- Queries use JOINs to fetch complete user data

**Benefits:**
- Normalized design
- No data duplication
- Easy to add new user types

### 2. Foreign Key Design Decisions

**Why Some Relationships Use Long IDs Instead of FKs:**

Some tables use `Long` IDs instead of foreign keys to `users`:
- `medical_post_like.likeGiverId` (Long, not FK)
- `medical_post_comment.commenterId` (Long, not FK)
- `medical_post_report.reporterId` (Long, not FK)
- `comment_like.likeGiverId` (Long, not FK)
- `comment_reply.replierId` (Long, not FK)
- `comment_reply_like.likeGiverId` (Long, not FK)

**Reasons:**
- Allows both `Patient` and `HealthcareProvider` to interact (both extend `Users`)
- Avoids complex polymorphic associations
- Simpler queries (no need to join through inheritance hierarchy)
- User lookup done in application layer when needed

**Tables That DO Use FKs:**
- `chat_message.sender_id` → `users.id` (FK)
- `notifications.recipient_id` → `users.id` (FK)
- `notifications.actor_id` → `users.id` (FK)

### 3. Cascade Operations

**Cascade ALL + Orphan Removal:**
- Post deletion → deletes all likes, comments, ratings, reports
- Comment deletion → deletes all comment likes and replies
- Reply deletion → deletes all reply likes
- Patient/Provider deletion → deletes all related entities

**Benefits:**
- Data integrity maintained automatically
- No orphaned records
- Clean deletion operations

### 4. Lazy Loading Strategy

**Most relationships use `FetchType.LAZY`:**
- Prevents loading unnecessary data
- Improves performance
- Reduces memory usage

**Eager Loading (via @EntityGraph):**
- `medical_post.postProvider` - Eagerly loaded to avoid N+1 queries
- `healthcare_provider.specializations` - Eagerly loaded (ElementCollection)

### 5. Element Collections

**Used for Multi-Valued Attributes:**
- `healthcare_provider.specializations` → `provider_specialization` table
- `healthcare_provider.insuranceAccepted` → `provider_insurance` table
- `healthcare_provider.availableDays` → `provider_availability` table

**Benefits:**
- Clean entity model
- Automatic table management by JPA
- Easy to query and update

### 6. Primary Key Types

**Most tables use BIGINT:**
- `users.id`, `patient.id`, `healthcare_provider.id`
- All social media tables
- Most medical records

**Exceptions (use INT):**
- `appointment_entity.id` (INT)
- `medical_record.id` (INT)

**Note:** Consider standardizing to BIGINT for consistency and scalability.

### 7. Timestamp Fields

**Common Patterns:**
- `createdAt` - Set automatically via `@PrePersist` or default value
- `updatedAt` - Set automatically via `@PreUpdate`
- `sentAt`, `readAt` - For chat messages
- `adminFlaggedAt` - For admin actions

### 8. Soft Delete Support

**Tables with Soft Delete:**
- `chat_message.isDeleted` - Soft delete flag
- `medical_post.adminFlagged` - Can be used for soft delete
- `patient.adminFlagged` - Can be used for soft delete
- `healthcare_provider.adminFlagged` - Can be used for soft delete

### 9. Unique Constraints

**Important Unique Constraints:**
- `users.username` - UNIQUE
- `users.email` - UNIQUE
- `chat_channel` - UNIQUE (patient_id, doctor_id)
- `user_notification_preferences.user_id` - UNIQUE (One-to-One)
- `user_privacy_settings.user_id` - UNIQUE (One-to-One)

### 10. Index Recommendations

**Consider Adding Indexes On:**
- `medical_post.provider_id` - Frequently queried
- `medical_post.createdAt` - Used for sorting
- `medical_post_like.post_id` - Used for counting
- `medical_post_comment.post_id` - Used for counting
- `appointment_entity.patient_id` - Frequently queried
- `appointment_entity.provider_id` - Frequently queried
- `appointment_entity.appointmentDateTime` - Used for filtering
- `notifications.recipient_id` - Frequently queried
- `notifications.isRead` - Used for filtering
- `chat_message.channel_id` - Frequently queried
- `chat_message.sentAt` - Used for sorting

---

## Database Query Patterns

### Common Query Patterns

1. **Fetch Posts with Provider:**
   ```sql
   SELECT p.*, hp.* 
   FROM medical_post p
   JOIN healthcare_provider hp ON p.provider_id = hp.id
   ORDER BY p.created_at DESC
   ```

2. **Count Likes for Multiple Posts:**
   ```sql
   SELECT post_id, COUNT(*) 
   FROM medical_post_like 
   WHERE post_id IN (?, ?, ...)
   GROUP BY post_id
   ```

3. **Fetch User Notifications:**
   ```sql
   SELECT n.*, u1.*, u2.*, mp.*, mpc.*
   FROM notifications n
   JOIN users u1 ON n.recipient_id = u1.id
   JOIN users u2 ON n.actor_id = u2.id
   LEFT JOIN medical_post mp ON n.post_id = mp.id
   LEFT JOIN medical_post_comment mpc ON n.comment_id = mpc.id
   WHERE n.recipient_id = ?
   ORDER BY n.created_at DESC
   ```

---

## File Locations Reference

### Entity Files

**Core Identity:**
- `EntryRelated/entities/Users.java`
- `EntryRelated/entities/Patient.java`
- `EntryRelated/entities/HealthcareProvider.java`
- `EntryRelated/entities/EducationHistory.java`
- `EntryRelated/entities/WorkExperience.java`
- `EntryRelated/entities/UserNotificationPreferences.java`
- `EntryRelated/entities/UserPrivacySettings.java`

**Social Media:**
- `socialmedia/entity/MedicalPost.java`
- `socialmedia/entity/MedicalPostLike.java`
- `socialmedia/entity/MedicalPostComment.java`
- `socialmedia/entity/CommentLike.java`
- `socialmedia/entity/CommentReply.java`
- `socialmedia/entity/CommentReplyLike.java`
- `socialmedia/entity/MedicalPostRating.java`
- `socialmedia/entity/MedicalPostReport.java`

**Appointments & Medical:**
- `Entities/AppointmentEntity.java`
- `EntryRelated/entities/DoctorReview.java`
- `EntryRelated/entities/MedicalRecord.java`
- `EntryRelated/entities/Medication.java`
- `EntryRelated/entities/MentalHealthMedication.java`
- `EntryRelated/entities/LaboratoryResult.java`

**Chat:**
- `socialmedia/entity/ChatChannel.java`
- `socialmedia/entity/ChatMessage.java`

**Notifications:**
- `socialmedia/entity/Notification.java`
- `socialmedia/entity/NotificationType.java` (Enum)

---

## Summary

### Total Tables: ~26 Main Tables

**Core Identity:** 9 tables
- users, patient, healthcare_provider
- education_history, work_experience
- user_notification_preferences, user_privacy_settings
- login_sessions, account_activity

**Social Media:** 8 tables
- medical_post, medical_post_like, medical_post_comment
- comment_like, comment_reply, comment_reply_like
- medical_post_rating, medical_post_report

**Appointments & Medical:** 6 tables
- appointment_entity, doctor_review
- medical_record, medication, mental_health_medication, laboratory_result

**Chat:** 2 tables
- chat_channel, chat_message

**Notifications:** 1 table
- notifications

**Element Collections:** 3 helper tables
- provider_specialization, provider_insurance, provider_availability

### Key Relationships

- **Inheritance:** users → patient/healthcare_provider (JOINED)
- **Social Media:** healthcare_provider → medical_post → likes/comments/replies
- **Appointments:** patient + healthcare_provider → appointment_entity → doctor_review
- **Chat:** patient + healthcare_provider + appointment → chat_channel → chat_message
- **Notifications:** users (recipient/actor) + optional post/comment links

---

**Document Version:** 1.0  
**Last Updated:** 2025-11-14  
**For:** Backend Developer Reference

