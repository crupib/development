About the Sqoop Demo 
-----------------------

While Sqoop supports the migration of data across a large number of databases
(relational and NoSQL), this demo focuses on how data is imported into and
exported from a Cassandra CQL table. It makes use of a MySQL database 
(www.mysql.com) as the source, but creating and populating the source table
can be performed just as easily on virtually any other RDBMS.

In order for Sqoop to access the source database, a suitable JDBC driver must be
available and placed in a directory that is part of the Sqoop classpath. The
suggested directory is the resources/sqoop subdirectory of your DataStax
Enterprise (DSE) installation. The JDBC driver for MySQL can be obtained from
   
   http://dev.mysql.com/downloads/connector/j/ 

(i.e. mysql-connector-java-5.1.30-bin.jar). 

This demo uses data from the North America Numbering Plan, which provides the 
area-code (NPA) and phone number prefix (Nxx) combinations for The United
States and Canada. 

Setting up the source data
-----------------------------

The first step of this demo is to get the data into the MySQL table 'npa_nxx' of
the database 'npa_nxx_demo'. This table will be the source of the data that will
be imported into Cassandra. Note that you will need sufficient database
privileges in order to create the database objects. The commands below can be
entered from a MySQL command line or a visual tool such as MySQL Workbench.

Create the database with the following command:

   CREATE DATABASE npa_nxx_demo;

Then connect to the database and create the table:

   CONNECT npa_nxx_demo;

   CREATE TABLE npa_nxx (
      npa_nxx_key int NOT NULL,
      npa         int DEFAULT NULL,
      nxx         int DEFAULT NULL,
      lat         float DEFAULT NULL,
      lon         float DEFAULT NULL,
      linetype    char DEFAULT NULL,
      state       varchar(2)  DEFAULT NULL,
      city        varchar(36) DEFAULT NULL,
      PRIMARY KEY (npa_nxx_key)
   ) ENGINE=InnoDB DEFAULT CHARSET=latin1;

Now populate the table (the npa_nxx.csv file is found in this demo's directory):

   LOAD DATA LOCAL INFILE 'npa_nxx.csv'
     INTO TABLE npa_nxx_demo.npa_nxx 
     FIELDS TERMINATED BY ','
     ENCLOSED BY '"'
     LINES TERMINATED BY '\n';

When the LOAD completes, a message similar to the following will be displayed:

   Query OK, 105291 rows affected (2.09 sec)
   Records: 105291  Deleted: 0  Skipped: 0  Warnings: 0

Finally, create the table for the data extract performed in section 2:

   CREATE TABLE npa_nxx_extract (
      npa_nxx_key int NOT NULL AUTO_INCREMENT,
      npa         int DEFAULT NULL,
      nxx         int DEFAULT NULL,
      latitude    float DEFAULT NULL,
      longitude   float DEFAULT NULL,
      state       varchar(2)  DEFAULT NULL,
      city        varchar(36) DEFAULT NULL,
      PRIMARY KEY (npa_nxx_key)
   ) ENGINE=InnoDB DEFAULT CHARSET=latin1;

Alternatively, you can run the script included in this demo's directory:

   mysql -u <user> -p --local-infile=1 -e "source mysql_setup.sql"

DataStax Enterprise (DSE)
-------------------------

Sqoop utilizes one or more DSE nodes running the Analytics workload in order to
run the Hadoop job that performs the import. Refer to the DSE documentation
on how to setup and run a single or a multi node configuration.

Running Sqoop
-------------

1 - Importing into Cassandra

The Sqoop integration provided with DSE allows for data import directly into
CQL tables. In order to carry out the import a keyspace and table must be 
created. Using cqlsh, enter the following commands:

   cqlsh> CREATE KEYSPACE npa_nxx WITH replication =
      ... {   'class': 'SimpleStrategy',   'replication_factor': '1' };

   cqlsh> CREATE TABLE npa_nxx.npa_nxx_data (npa int, nxx int, 
      ... latitude float, longitude float, state text, city text, 
      ... PRIMARY KEY(npa, nxx));

   cqlsh> exit

Again, you can run the script included in this demo's directory to do this:

   cqlsh -f import_setup.cql

The following command imports data from the MySql table 'npa_nxx' into the
Cassandra CQL table npa_nxx.npa_nxx_data:

   dse sqoop \ 
      cql-import \
      --connect jdbc:mysql://<mysql_host>/npa_nxx_demo \
      --username <mysql_user> \
      --password <mysql_password> \
      --table npa_nxx \
      --cassandra-keyspace npa_nxx \ 
      --cassandra-table npa_nxx_data \
      --cassandra-column-mapping npa:npa,nxx:nxx,\
      latitude:lat,longitude:lon,state:state,city:city \
      --cassandra-host <dse_host>

These options are provided in the demo folder and can be run as:

   dse sqoop --options-file import.options
   
The command line options are explained as follows:

   cql-import specifies that this is an import into a CQL table
   
   --connect  the jdbc connect URL
   --username the source database username
   --password the source database password
   --table    indicates the database table to import
   
   --cassandra-keyspace       the Cassandra keyspace for the import
   --cassandra-table          the Cassandra table for the import
   --cassandra-column-mapping the column mapping for the import
   --cassandra-host           a comma separated list of cassandra hosts
   
The --cassandra-column-mapping argument allows CQL columns and SQL columns
with different names to be mapped to each other. It also makes it possible
to select which columns to import. The format of the column mapping is:

   [CQL column]:[SQL column],[CQL column]:[SQL column]...
   
In the above cql-import the SQL npa_nxx_key and linetype columns are not
imported into the CQL table and the SQL lat and lon columns are mapped to
the CQL latitude and longitude columns.

Once the Sqoop job has completed the data can be verified in Cassandra 
using cqlsh:

   cqlsh> SELECT * FROM npa_nxx.npa_nxx_data LIMIT 5;

   npa | nxx | city        | latitude | longitude | state
  -----+-----+-------------+----------+-----------+-------
   660 | 200 |     Braymer |    39.59 |      93.8 |    MO
   660 | 202 |     Sedalia |     38.7 |     93.22 |    MO
   660 | 213 |    La Belle |    40.11 |     91.91 |    MO
   660 | 214 | Chillicothe |    39.79 |     93.55 |    MO
   660 | 215 |   Maryville |    40.34 |     94.87 |    MO


2 - Exporting data from Cassandra
   
Now that the Cassandra table is populated with data it is possible to
export data to the SQL extract table created at the beginning of this
demo.

The following command will export the Cassandra data to MySql:

   dse sqoop \ 
      cql-export \
      --connect jdbc:mysql://<mysql_host>/npa_nxx_demo \
      --username <mysql_user> \
      --password <mysql_password> \
      --table npa_nxx_extract \
      --cassandra-keyspace npa_nxx \ 
      --cassandra-table npa_nxx_data \
      --cassandra-host <dse_host>
 
These options are provided in the demo folder and can be run as:

   dse sqoop --options-file export.options
   
In this command the --cassandra-column-mapping option is not specified 
because the column names in the SQL extract table match the column names
in the CQL table.

The command line options that differ from the cql-import are explained
as follows:

   cql-export specifies that this is an export from a CQL table
   
   --cassandra-host   a comma separated list of Cassandra hosts

Note that the --cassandra-column-mapping option is not needed for this
command because the column names in the SQL table match the column names
in the CQL table.

Once the Sqoop export job has completed the data can be verified using the
MySQL client:

   mysql> select * from npa_nxx_extract limit 5;
   +-------------+------+------+----------+-----------+-------+-------------+
   | npa_nxx_key | npa  | nxx  | latitude | longitude | state | city        |
   +-------------+------+------+----------+-----------+-------+-------------+
   |           1 |  660 |  200 |    39.59 |      93.8 | MO    | Braymer     |
   |           2 |  660 |  202 |     38.7 |     93.22 | MO    | Sedalia     |
   |           3 |  660 |  213 |    40.11 |     91.91 | MO    | La Belle    |
   |           4 |  660 |  214 |    39.79 |     93.55 | MO    | Chillicothe |
   |           5 |  660 |  215 |    40.34 |     94.87 | MO    | Maryville   |
   +-------------+------+------+----------+-----------+-------+-------------+
   
Further Information
-------------------

For further information about the DSE specific Sqoop commands please consult
the appropriate release documentation at:

http://www.datastax.com/docs    
