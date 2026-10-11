package jjcet.PragatiX.integrations.neopat.config.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

/**
 * Ensures database schema migration replacing current_assessment with separate
 * physical tables (assessment_2027, assessment_2028, assessment_2029, assessment_2030),
 * backing up existing records, migrating eligible data, safely dropping current_assessment,
 * and seeding batch schedules in neopa_sms.
 */
@Component
@Order(1)
public class NeopatBatchSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NeopatBatchSchemaInitializer.class);
    private static final List<Integer> SUPPORTED_BATCHES = List.of(2027, 2028, 2029, 2030);

    private final DataSource neopatDataSource;

    public NeopatBatchSchemaInitializer(@Qualifier("neopatDataSource") DataSource neopatDataSource) {
        this.neopatDataSource = neopatDataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Running NeopatBatchSchemaInitializer for batch-wise physical tables...");
        try (Connection conn = neopatDataSource.getConnection()) {
            createBatchAssessmentTables(conn);
            migrateAndDropOldCurrentAssessment(conn);
            dropBackupTableIfPresent(conn);
            dropRawPayloadColumnIfPresent(conn);
            ensureAuxiliaryColumns(conn);
            dropOldExecutionConstraintIfPresent(conn);
            seedDefaultBatchSchedules(conn);
            log.info("NeopatBatchSchemaInitializer completed successfully.");
        } catch (Exception e) {
            log.error("Failed to execute NeopatBatchSchemaInitializer: {}", e.getMessage(), e);
        }
    }

    private void createBatchAssessmentTables(Connection conn) {
        for (Integer batch : SUPPORTED_BATCHES) {
            String tableName = "assessment_" + batch;
            String createTableSql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "email VARCHAR(255) NOT NULL, " +
                    "test_id VARCHAR(100) NOT NULL, " +
                    "test_name VARCHAR(255) NULL, " +
                    "marks DECIMAL(7, 2) NOT NULL, " +
                    "total_marks DECIMAL(7, 2) NOT NULL, " +
                    "attempts INT DEFAULT 1, " +
                    "result_analysis_url VARCHAR(1024) NULL, " +
                    "start_time DATETIME NULL, " +
                    "submit_time DATETIME NULL, " +
                    "section_wise_marks JSON NULL, " +
                    "processing_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', " +
                    "received_at DATETIME NOT NULL, " +
                    "created_at DATETIME NOT NULL, " +
                    "CONSTRAINT uk_a" + batch + "_email_test UNIQUE (email, test_id), " +
                    "INDEX idx_a" + batch + "_status (processing_status), " +
                    "INDEX idx_a" + batch + "_email (email), " +
                    "INDEX idx_a" + batch + "_test_id (test_id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
            executeUpdate(conn, createTableSql);
            log.info("Verified table structure for {}", tableName);
        }
    }

    private void migrateAndDropOldCurrentAssessment(Connection conn) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            boolean currentAssessmentExists = false;
            try (ResultSet rs = metaData.getTables(null, null, "current_assessment", new String[]{"TABLE"})) {
                if (rs.next()) {
                    currentAssessmentExists = true;
                }
            }

            if (!currentAssessmentExists) {
                log.info("current_assessment table is absent; no migration needed.");
                return;
            }

            // Migrate eligible records directly into assessment_2027..2030
            for (Integer batch : SUPPORTED_BATCHES) {
                String targetTable = "assessment_" + batch;
                String copySql = "INSERT IGNORE INTO " + targetTable + " " +
                        "(email, test_id, test_name, marks, total_marks, attempts, result_analysis_url, " +
                        "start_time, submit_time, section_wise_marks, raw_payload, processing_status, received_at, created_at) " +
                        "SELECT email, test_id, COALESCE(test_name, test_id), marks, total_marks, attempts, result_analysis_url, " +
                        "start_time, submit_time, section_wise_marks, raw_payload, processing_status, received_at, created_at " +
                        "FROM current_assessment " +
                        "WHERE batch_year = " + batch + " " +
                        "OR (batch_year IS NULL AND test_name LIKE '" + batch + "%') " +
                        "OR (batch_year IS NULL AND test_id LIKE '" + batch + "%')";
                int rows = executeUpdateWithCount(conn, copySql);
                log.info("Migrated {} rows from current_assessment into {}", rows, targetTable);
            }

            // Drop the old table completely
            executeUpdate(conn, "DROP TABLE IF EXISTS current_assessment");
            log.info("Dropped old current_assessment table after migrating to batch tables.");

        } catch (Exception e) {
            log.error("Error during current_assessment migration: {}", e.getMessage(), e);
        }
    }

    private void dropBackupTableIfPresent(Connection conn) {
        try {
            executeUpdate(conn, "DROP TABLE IF EXISTS current_assessment_backup");
            log.info("Ensured current_assessment_backup table is removed.");
        } catch (Exception e) {
            log.debug("Note on dropping current_assessment_backup: {}", e.getMessage());
        }
    }

    private void dropRawPayloadColumnIfPresent(Connection conn) {
        for (Integer batch : SUPPORTED_BATCHES) {
            dropColumnIfExists(conn, "assessment_" + batch, "raw_payload");
        }
        dropColumnIfExists(conn, "assessment_history", "raw_payload");
    }

    private void dropColumnIfExists(Connection conn, String tableName, String columnName) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
                if (rs.next()) {
                    executeUpdate(conn, "ALTER TABLE " + tableName + " DROP COLUMN " + columnName);
                    log.info("Dropped redundant column {} from table {}", columnName, tableName);
                }
            }
        } catch (Exception e) {
            log.debug("Note on dropping column {} from table {}: {}", columnName, tableName, e.getMessage());
        }
    }

    private void ensureAuxiliaryColumns(Connection conn) {
        // assessment_history
        addColumnIfNotExists(conn, "assessment_history", "batch_year", "INT DEFAULT 2027");
        addColumnIfNotExists(conn, "assessment_history", "test_name", "VARCHAR(255) NULL");
        executeUpdate(conn, "UPDATE assessment_history SET batch_year = 2027 WHERE batch_year IS NULL");

        // neopat_sms_schedule
        addColumnIfNotExists(conn, "neopat_sms_schedule", "batch_year", "INT DEFAULT 2027");
        executeUpdate(conn, "UPDATE neopat_sms_schedule SET batch_year = 2027 WHERE batch_year IS NULL");

        // neopat_sms_execution
        addColumnIfNotExists(conn, "neopat_sms_execution", "batch_year", "INT DEFAULT 2027");
        executeUpdate(conn, "UPDATE neopat_sms_execution SET batch_year = 2027 WHERE batch_year IS NULL");
    }

    private void addColumnIfNotExists(Connection conn, String tableName, String columnName, String columnDefinition) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getColumns(null, null, tableName, columnName)) {
                if (!rs.next()) {
                    String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnDefinition;
                    executeUpdate(conn, sql);
                    log.info("Added column {} to table {}", columnName, tableName);
                }
            }
        } catch (Exception e) {
            log.warn("Could not check/add column {} on table {}: {}", columnName, tableName, e.getMessage());
        }
    }

    private void dropOldExecutionConstraintIfPresent(Connection conn) {
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            try (ResultSet rs = metaData.getIndexInfo(null, null, "neopat_sms_execution", false, false)) {
                boolean hasOldConstraint = false;
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if ("uk_scheduled_date_time".equalsIgnoreCase(indexName)) {
                        hasOldConstraint = true;
                        break;
                    }
                }
                if (hasOldConstraint) {
                    executeUpdate(conn, "ALTER TABLE neopat_sms_execution DROP INDEX uk_scheduled_date_time");
                    log.info("Dropped legacy uk_scheduled_date_time index on neopat_sms_execution");
                }
            }
        } catch (Exception e) {
            log.debug("Note on dropping legacy index: {}", e.getMessage());
        }
    }

    private void seedDefaultBatchSchedules(Connection conn) {
        for (Integer batch : SUPPORTED_BATCHES) {
            try {
                String checkSql = "SELECT COUNT(*) FROM neopat_sms_schedule WHERE batch_year = ?";
                try (PreparedStatement ps = conn.prepareStatement(checkSql)) {
                    ps.setInt(1, batch);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next() && rs.getInt(1) == 0) {
                            String insertSql = "INSERT INTO neopat_sms_schedule (batch_year, day_of_week, send_time, enabled, timezone, created_at, updated_at) " +
                                    "VALUES (?, 'SATURDAY', '10:00:00', false, 'Asia/Kolkata', NOW(), NOW())";
                            try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                                ins.setInt(1, batch);
                                ins.executeUpdate();
                                log.info("Seeded default SMS schedule for batch {}", batch);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not seed schedule for batch {}: {}", batch, e.getMessage());
            }
        }
    }

    private void executeUpdate(Connection conn, String sql) {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (Exception e) {
            log.debug("Ignored exception during SQL execution [{}]: {}", sql, e.getMessage());
        }
    }

    private int executeUpdateWithCount(Connection conn, String sql) {
        try (Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        } catch (Exception e) {
            log.debug("Ignored exception during SQL execution [{}]: {}", sql, e.getMessage());
            return 0;
        }
    }
}
