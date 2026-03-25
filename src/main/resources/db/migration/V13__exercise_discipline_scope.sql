CREATE TABLE exercise_discipline_scope (
    exercise_id   INTEGER  NOT NULL REFERENCES exercises(id),
    discipline_id SMALLINT NOT NULL REFERENCES disciplines(id),
    PRIMARY KEY (exercise_id, discipline_id)
);

-- General weightlifting: everything without a lift_category, plus shared barbell compounds
INSERT INTO exercise_discipline_scope (exercise_id, discipline_id)
SELECT e.id, d.id FROM exercises e, disciplines d
WHERE d.code = 'WEIGHTLIFT'
  AND (e.lift_category IS NULL OR e.lift_category IN ('squat', 'bench', 'deadlift'));

-- Powerlifting: competition lifts + accessory work (no Olympic-specific movements)
INSERT INTO exercise_discipline_scope (exercise_id, discipline_id)
SELECT e.id, d.id FROM exercises e, disciplines d
WHERE d.code = 'POWERLIFTING'
  AND e.name NOT IN (
      'Snatch', 'Clean and jerk', 'Power snatch', 'Power clean',
      'Hang snatch', 'Hang clean', 'Overhead squat'
  );

-- Olympic lifting: competition movements
INSERT INTO exercise_discipline_scope (exercise_id, discipline_id)
SELECT e.id, d.id FROM exercises e, disciplines d
WHERE d.code = 'OLYMPIC_LIFT'
  AND e.lift_category IN ('snatch', 'clean_and_jerk');

-- Olympic lifting support exercises (pulls, squats used as assistance)
INSERT INTO exercise_discipline_scope (exercise_id, discipline_id)
SELECT e.id, d.id FROM exercises e, disciplines d
WHERE d.code = 'OLYMPIC_LIFT'
  AND e.name IN ('Snatch pull', 'Clean pull', 'Front squat', 'Overhead squat',
                 'Romanian deadlift', 'Barbell back squat');