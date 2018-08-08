CREATE KEYSPACE triggerdemo WITH REPLICATION = { 'class' : 'SimpleStrategy', 'replication_factor' : 1 };

USE triggerdemo;

CREATE TABLE names (first VARCHAR PRIMARY KEY, last VARCHAR);

CREATE TABLE inames (first VARCHAR PRIMARY KEY, last VARCHAR);

CREATE TRIGGER InvertedNames ON names
  USING 'org.apache.cassandra.triggers.InvertedIndex';

COPY names (first, last) from 'names.csv';