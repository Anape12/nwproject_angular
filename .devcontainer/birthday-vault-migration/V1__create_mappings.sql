CREATE TABLE mappings (
    record_id VARCHAR(36) PRIMARY KEY,
    token VARCHAR(36) UNIQUE NOT NULL,
    key_id VARCHAR(36) NOT NULL,
    key_nonce VARCHAR(24) NOT NULL,
    wrapped_key VARCHAR(128) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;
