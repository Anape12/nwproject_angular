ALTER TABLE users_info
    ADD COLUMN birthday_secret_id VARCHAR(36) NULL,
    ADD UNIQUE KEY uq_birthday_secret_id (birthday_secret_id);

CREATE TABLE payloads (
    token VARCHAR(36) PRIMARY KEY,
    nonce VARCHAR(24) NOT NULL,
    ciphertext LONGTEXT NOT NULL
) ENGINE=InnoDB;

INSERT INTO error_message_mst (error_code, message_text) VALUES
('ERR00010140', '生年月日を安全に保存・取得できません。管理者にお問い合わせください。'),
('ERR00010141', 'このユーザーの生年月日を操作する権限がありません。'),
('ERR00010142', '生年月日の移行が完了していません。管理者にお問い合わせください。');

INSERT INTO audit_reason_mst (event_category, reason_code, message_template) VALUES
('PRIVACY', 'BIRTHDAY_VIEWED', '生年月日を閲覧しました。'),
('PRIVACY', 'BIRTHDAY_CHANGED', '生年月日を変更しました。'),
('PRIVACY', 'BIRTHDAY_MIGRATED', '生年月日を暗号化保存へ移行しました。');
