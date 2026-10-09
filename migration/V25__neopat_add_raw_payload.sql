-- ==============================================================================
-- DATABASE 2 (neopa_sms): ADD raw_payload TO PRESERVE ENTIRE RAW REQUEST
-- ==============================================================================

USE neopa_sms;

ALTER TABLE current_assessment
    ADD COLUMN IF NOT EXISTS raw_payload JSON NULL AFTER section_wise_marks;

ALTER TABLE assessment_history
    ADD COLUMN IF NOT EXISTS raw_payload JSON NULL AFTER section_wise_marks;

ALTER TABLE failed_assessment
    ADD COLUMN IF NOT EXISTS raw_payload LONGTEXT NULL AFTER section_wise_marks;
