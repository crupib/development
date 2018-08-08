CREATE KEYSPACE wiki WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE wiki;

create table solr (
  "id" VARCHAR PRIMARY KEY,
  "name" TEXT,
  "body" TEXT,
  "title" TEXT,
  "date" VARCHAR);
