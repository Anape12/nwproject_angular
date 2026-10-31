-- Run manually with schema-administration credentials AFTER BirthdayAdmin verify succeeds.
-- Never add this to the automatic Flyway chain before migrating existing rows.
ALTER TABLE users_info ADD CONSTRAINT chk_birthday_no_plaintext CHECK (birthday IS NULL);
