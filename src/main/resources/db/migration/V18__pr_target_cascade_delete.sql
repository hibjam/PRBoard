ALTER TABLE personal_records
    DROP CONSTRAINT personal_records_pr_target_id_fkey;

ALTER TABLE personal_records
    ADD CONSTRAINT personal_records_pr_target_id_fkey
        FOREIGN KEY (pr_target_id)
            REFERENCES user_pr_targets(id)
            ON DELETE CASCADE;