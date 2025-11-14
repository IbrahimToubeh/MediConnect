package com.MediConnect.EntryRelated.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@RequiredArgsConstructor
@Slf4j
public class DatabaseSchemaPatcher implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        applyPatch("ALTER TABLE users ADD COLUMN IF NOT EXISTS account_status VARCHAR(32) DEFAULT 'ACTIVE'");
        applyPatch("UPDATE users SET account_status = 'ACTIVE' WHERE account_status IS NULL");

        applyPatch("ALTER TABLE healthcare_provider ADD COLUMN IF NOT EXISTS admin_flagged BOOLEAN DEFAULT false");
        applyPatch("ALTER TABLE healthcare_provider ADD COLUMN IF NOT EXISTS admin_flag_reason TEXT");
        applyPatch("ALTER TABLE healthcare_provider ADD COLUMN IF NOT EXISTS admin_flagged_at TIMESTAMP");
        applyPatch("UPDATE healthcare_provider SET admin_flagged = false WHERE admin_flagged IS NULL");

        applyPatch("ALTER TABLE patient ADD COLUMN IF NOT EXISTS admin_flagged BOOLEAN DEFAULT false");
        applyPatch("ALTER TABLE patient ADD COLUMN IF NOT EXISTS admin_flag_reason TEXT");
        applyPatch("ALTER TABLE patient ADD COLUMN IF NOT EXISTS admin_flagged_at TIMESTAMP");
        applyPatch("UPDATE patient SET admin_flagged = false WHERE admin_flagged IS NULL");

        applyPatch("ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check");
        applyPatch(
                "ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN (" +
                        "'POST_LIKE'," +
                        "'POST_COMMENT'," +
                        "'COMMENT_LIKE'," +
                        "'COMMENT_REPLY'," +
                        "'POST_SHARE'," +
                        "'APPOINTMENT_REQUESTED'," +
                        "'APPOINTMENT_CONFIRMED'," +
                        "'APPOINTMENT_CANCELLED'," +
                        "'APPOINTMENT_RESCHEDULED'," +
                        "'APPOINTMENT_RESCHEDULE_CONFIRMED'," +
                        "'APPOINTMENT_RESCHEDULE_CANCELLED'," +
                        "'APPOINTMENT_REMINDER_24H'," +
                        "'CHAT_MESSAGE'," +
                        "'ADMIN_POST_REPORTED'," +
                        "'ADMIN_DOCTOR_REGISTRATION'" +
                        "))");
    }

    private void applyPatch(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ex) {
            log.debug("Skipping patch [{}]: {}", sql, ex.getMessage());
        }
    }
}

