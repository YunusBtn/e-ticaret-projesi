ALTER TABLE trendyol.users ADD COLUMN updated_at TIMESTAMP(6);
UPDATE trendyol.users
ALTER TABLE trendyol.users ADD COLUMN updated_at TIMESTAMP(6);
UPDATE trendyol.users SET updated_at = COALESCE(created_at, now());
ALTER TABLE trendyol.users ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE trendyol.products ADD COLUMN updated_at TIMESTAMP(6);
UPDATE trendyol.products SET updated_at = COALESCE(created_at, now());
ALTER TABLE trendyol.products ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE trendyol.orders ADD COLUMN updated_at TIMESTAMP(6);
UPDATE trendyol.orders SET updated_at = COALESCE(created_at, now());
ALTER TABLE trendyol.orders ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE trendyol.notifications ADD COLUMN updated_at TIMESTAMP(6);
UPDATE trendyol.notifications SET updated_at = COALESCE(created_at, now());
ALTER TABLE trendyol.notifications ALTER COLUMN updated_at SET NOT NULL;