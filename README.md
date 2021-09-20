# Vertx-4.1.0-PgConnection-Issue
This is the reproducer project for the following issue. Vertx-pg-client 4.1.0 creates as many connections as mentioned for max pool size. It is not reusing the existing connections and creates new connection until it reaches max pool size. But vertx-pg-client 3.9.3 used to create only single connection with the same connection pool configuration and application code. 
## To run Reproducer.java 
Configure port, host, database, user and password of Postgres database.
