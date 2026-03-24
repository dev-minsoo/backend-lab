ALTER TABLE notifications
    RENAME COLUMN `read` TO is_read;

ALTER TABLE notifications
    RENAME INDEX idx_notifications_user_read TO idx_notifications_user_is_read;
