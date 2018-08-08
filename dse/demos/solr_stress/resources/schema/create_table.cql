CREATE KEYSPACE IF NOT EXISTS demo WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE demo;

create table if not exists solr (
  "id" VARCHAR PRIMARY KEY,
  "type" VARCHAR,
  "text" TEXT);
