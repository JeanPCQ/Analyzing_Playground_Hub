-- Wipes the main database so the app rebuilds it with fresh sample data on next start.
-- Use when you want to start over. Run as root:
--     mysql -u root -p < docs/reset-database.sql
DROP DATABASE IF EXISTS wms;
CREATE DATABASE wms CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
GRANT ALL PRIVILEGES ON wms.* TO 'wms_app'@'localhost';
FLUSH PRIVILEGES;
SELECT 'wms was reset. Start the app to rebuild it.' AS result;
