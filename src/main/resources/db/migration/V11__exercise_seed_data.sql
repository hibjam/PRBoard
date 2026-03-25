-- General weightlifting exercises
INSERT INTO exercises (name, muscle_group, equipment, lift_category) VALUES
    ('Pull-up',                'back',      'bodyweight', null),
    ('Dip',                    'chest',     'bodyweight', null),
    ('Dumbbell curl',          'arms',      'dumbbell',   null),
    ('Dumbbell lateral raise', 'shoulders', 'dumbbell',   null),
    ('Dumbbell row',           'back',      'dumbbell',   null),
    ('Cable row',              'back',      'cable',      null),
    ('Cable fly',              'chest',     'cable',      null),
    ('Tricep pushdown',        'arms',      'cable',      null),
    ('Face pull',              'shoulders', 'cable',      null),
    ('Leg press',              'legs',      'machine',    null),
    ('Leg curl',               'legs',      'machine',    null),
    ('Leg extension',          'legs',      'machine',    null),
    ('Lat pulldown',           'back',      'cable',      null),
    ('Romanian deadlift',      'legs',      'barbell',    null),
    ('Incline bench press',    'chest',     'barbell',    null),
    ('Hip thrust',             'legs',      'barbell',    null),
    ('Arnold press',           'shoulders', 'dumbbell',   null);

-- Powerlifting big 3 (also scoped to general WEIGHTLIFT)
INSERT INTO exercises (name, muscle_group, equipment, lift_category) VALUES
    ('Barbell back squat',  'legs',  'barbell', 'squat'),
    ('Barbell bench press', 'chest', 'barbell', 'bench'),
    ('Barbell deadlift',    'back',  'barbell', 'deadlift'),
    ('Sumo deadlift',       'back',  'barbell', 'deadlift'),
    ('Low bar squat',       'legs',  'barbell', 'squat'),
    ('Close-grip bench',    'chest', 'barbell', 'bench');

-- Olympic lifts
INSERT INTO exercises (name, muscle_group, equipment, lift_category) VALUES
    ('Snatch',          'full body', 'barbell', 'snatch'),
    ('Clean and jerk',  'full body', 'barbell', 'clean_and_jerk'),
    ('Power snatch',    'full body', 'barbell', 'snatch'),
    ('Power clean',     'full body', 'barbell', 'clean_and_jerk'),
    ('Hang snatch',     'full body', 'barbell', 'snatch'),
    ('Hang clean',      'full body', 'barbell', 'clean_and_jerk'),
    ('Snatch pull',     'back',      'barbell', null),
    ('Clean pull',      'back',      'barbell', null),
    ('Front squat',     'legs',      'barbell', null),
    ('Overhead squat',  'legs',      'barbell', null);