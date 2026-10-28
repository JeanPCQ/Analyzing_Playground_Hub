-- One-time database setup for StockPilot WMS.
-- Run it as the MySQL root user:
--     mysql -u root -p < docs/setup-mysql.sql
--
-- Before running: change 'ChangeMe-123' below to your own password,
-- and put the same password in config/local.yml.

CREATE DATABASE IF NOT EXISTS wms
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

CREATE DATABASE IF NOT EXISTS wms_sandbox
  CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;

-- The app logs in as this user, never as root.
CREATE USER IF NOT EXISTS 'wms_app'@'localhost' IDENTIFIED BY 'ChangeMe-123';

GRANT ALL PRIVILEGES ON wms.*         TO 'wms_app'@'localhost';
GRANT ALL PRIVILEGES ON wms_sandbox.* TO 'wms_app'@'localhost';
FLUSH PRIVILEGES;

SELECT 'Databases wms and wms_sandbox are ready.' AS result;
