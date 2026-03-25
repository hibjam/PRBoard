CREATE TABLE disciplines (
    id            SMALLSERIAL PRIMARY KEY,
    code          VARCHAR(20)  NOT NULL UNIQUE,
    display_name  VARCHAR(50)  NOT NULL,
    is_distance_based BOOLEAN  NOT NULL DEFAULT true,
    is_active     BOOLEAN      NOT NULL DEFAULT true
);

INSERT INTO disciplines (code, display_name, is_distance_based) VALUES
    ('SWIM',       'Swimming',      true),
    ('BIKE',       'Cycling',       true),
    ('RUN',        'Running',       true),
    ('TRAIL_RUN',  'Trail running', true),
    ('ROW',        'Rowing',        true),
    ('HIKE',       'Hiking',        true),
    ('WEIGHTLIFT', 'Weightlifting', false),
    ('POWERLIFTING',  'Powerlifting',    false),
    ('OLYMPIC_LIFT',  'Olympic lifting', false);