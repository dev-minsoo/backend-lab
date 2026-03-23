#!/bin/bash
set -euo pipefail

until mysql -hmysql-primary -uroot -proot -e "SELECT 1" >/dev/null 2>&1; do
  echo "waiting for mysql-primary..."
  sleep 3
done

mysql -uroot -proot <<'SQL'
STOP REPLICA;
RESET REPLICA ALL;
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='mysql-primary',
  SOURCE_PORT=3306,
  SOURCE_USER='repl_user',
  SOURCE_PASSWORD='repl_password',
  SOURCE_AUTO_POSITION=1,
  GET_SOURCE_PUBLIC_KEY=1;
START REPLICA;
SET GLOBAL read_only = ON;
SET GLOBAL super_read_only = ON;
SQL
