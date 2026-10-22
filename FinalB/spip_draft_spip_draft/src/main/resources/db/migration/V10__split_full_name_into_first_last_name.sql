ALTER TABLE users
    ADD COLUMN first_name VARCHAR(75) NULL,
    ADD COLUMN last_name VARCHAR(75) NULL;

UPDATE users
SET
    first_name = TRIM(SUBSTRING_INDEX(full_name, ' ', 1)),
    last_name = TRIM(
        CASE
            WHEN full_name LIKE '% %'
                THEN SUBSTRING(full_name, LENGTH(SUBSTRING_INDEX(full_name, ' ', 1)) + 2)
            ELSE ''
        END
    )
WHERE full_name IS NOT NULL;

ALTER TABLE users
    MODIFY COLUMN first_name VARCHAR(75) NOT NULL,
    MODIFY COLUMN last_name VARCHAR(75) NOT NULL;

ALTER TABLE users
    DROP COLUMN full_name;