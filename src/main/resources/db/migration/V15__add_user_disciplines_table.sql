CREATE TABLE user_disciplines (
    user_id      UUID     NOT NULL REFERENCES users(user_id) ON DELETE CASCADE,
    discipline_id SMALLINT NOT NULL REFERENCES disciplines(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, discipline_id)
);

CREATE INDEX idx_user_disciplines_user ON user_disciplines(user_id);