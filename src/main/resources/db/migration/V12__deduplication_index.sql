-- Index to make duplicate detection queries fast
CREATE INDEX idx_activities_dedup
    ON activities(user_id, discipline_id, start_time)
    WHERE canonical_activity_id IS NULL;