CREATE KEYSPACE IF NOT EXISTS demo WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE demo;

create table if not exists geo (
  "id" VARCHAR PRIMARY KEY,
  "name" VARCHAR,
  "latitude" VARCHAR,
  "longitude" VARCHAR,
  "country" VARCHAR,
  "timezone" VARCHAR,
  "published" VARCHAR,
  "text" TEXT);
