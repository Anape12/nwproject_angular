-- Disposable integration test environment, never deployment credentials.
CREATE DATABASE nw_birthday_test CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE nw_birthday_vault CHARACTER SET utf8mb4 COLLATE utf8mb4_bin;
CREATE USER 'birthday_test_app'@'%' IDENTIFIED BY 'birthday-test-app-only';
CREATE USER 'birthday_test_vault'@'%' IDENTIFIED BY 'birthday-test-vault-only';
GRANT SELECT,INSERT,UPDATE,DELETE ON nw_birthday_test.* TO 'birthday_test_app'@'%';
GRANT SELECT,INSERT,UPDATE,DELETE ON nw_birthday_vault.* TO 'birthday_test_vault'@'%';
