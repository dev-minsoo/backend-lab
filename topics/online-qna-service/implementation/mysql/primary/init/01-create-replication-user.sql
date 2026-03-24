CREATE USER IF NOT EXISTS 'repl_user'@'%' IDENTIFIED WITH mysql_native_password BY 'repl_password';
CREATE USER IF NOT EXISTS 'qna'@'%' IDENTIFIED WITH mysql_native_password BY 'qna';
GRANT REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'repl_user'@'%';
GRANT ALL PRIVILEGES ON qna_primary.* TO 'qna'@'%';
FLUSH PRIVILEGES;
