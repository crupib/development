About the Pig CQL Demo 
------------------------ 

This Pig CQL demo consists of a data population script (library-populate-cql.txt) and a Pig script to show how to 
generate the total square feet per state for the library buildings.


How to run the example
----------------------

This example uses library data from the Institute of Library and Museum Services, encoded in UTF-8 format. 
Download the formatted data from http://www.datastax.com/documentation/tutorials/libdata.csv.zip.

1.  Unzip libdata.csv.zip and give yourself permission to access the downloaded file. On the Linux command line,
    for example:

        chmod 777 libdata.csv

2.  Create and use a keyspace called libdata.

        cqlsh> CREATE KEYSPACE libdata WITH replication =
                     {'class': 'SimpleStrategy', 'replication_factor': 1 };
        cqlsh> USE libdata;

3.  Create a table for the library data that you downloaded.

        cqlsh:libdata> CREATE TABLE libout ("STABR" TEXT, "FSCSKEY" TEXT, "FSCS_SEQ" TEXT,
                 "LIBID" TEXT, "LIBNAME" TEXT, "ADDRESS" TEXT, "CITY" TEXT,
                 "ZIP" TEXT, "ZIP4" TEXT, "CNTY" TEXT, "PHONE" TEXT, "C_OUT_TY" TEXT,
                 "C_MSA" TEXT, "SQ_FEET" INT, "F_SQ_FT" TEXT, "L_NUM_BM" INT,
                 "F_BKMOB" TEXT, "HOURS" INT, "F_HOURS" TEXT, "WKS_OPEN" INT,
                 "F_WKSOPN" TEXT, "YR_SUB" INT, "STATSTRU" INT, "STATNAME" INT,
                 "STATADDR" INT, "LONGITUD" FLOAT, "LATITUDE" FLOAT, "FIPSST" INT,
                 "FIPSCO" INT, "FIPSPLAC" INT, "CNTYPOP" INT, "LOCALE" TEXT,
                 "CENTRACT" FLOAT, "CENBLOCK" INT, "CDCODE" TEXT, "MAT_CENT" TEXT,
                 "MAT_TYPE" INT, "CBSA" INT, "MICROF" TEXT,
                 PRIMARY KEY ("FSCSKEY", "FSCS_SEQ"));

4.  Import data into the libout table from the libdata.csv file that you downloaded.

        cqlsh:libdata> COPY libout ("STABR","FSCSKEY","FSCS_SEQ","LIBID","LIBNAME",
                 "ADDRESS","CITY","ZIP","ZIP4","CNTY","PHONE","C_OUT_TY",
                 "C_MSA","SQ_FEET","F_SQ_FT","L_NUM_BM","F_BKMOB","HOURS",
                 "F_HOURS","WKS_OPEN","F_WKSOPN","YR_SUB","STATSTRU","STATNAME",
                 "STATADDR","LONGITUD","LATITUDE","FIPSST","FIPSCO","FIPSPLAC",
                 "CNTYPOP","LOCALE","CENTRACT","CENBLOCK","CDCODE","MAT_CENT",
                 "MAT_TYPE","CBSA","MICROF") FROM 'libdata.csv' WITH HEADER=TRUE;

    In the FROM clause of the COPY command, use the path to libdata.csv in your environment.

5.  Check that the libout table contains the data you copied from the downloaded file.

        cqlsh:libdata> SELECT count(*) FROM libdata.libout LIMIT 20000;

        count
        -------
        17598

6.   Create a table to hold results of Pig relations.

         cqlsh:libdata> CREATE TABLE libsqft (
                 year INT,
                 state TEXT,
                 sqft BIGINT,
                 PRIMARY KEY (year, state)
               );

7.   Using Pig, add a plan to load the data from the Cassandra libout table to a Pig relation.

         grunt> libdata = LOAD 'cql://libdata/libout' USING CqlNativeStorage();

8.   Add logic to remove data about outlet types other than books-by-mail (BM). The C_OUT_TY column uses BM 
     and other abbreviations to identify these library outlet types:

     CE-Central Library
     BR-Branch Library
     BS-Bookmobile(s)
     BM-Books-by-Mail Only

         grunt> book_by_mail = FILTER libdata BY C_OUT_TY == 'BM';
         grunt> DUMP book_by_mail;

9.   Add logic to filter the data based on library buildings and sum square footage of the buildings, then
     group data by state. The STABR column contains the state codes.

         grunt> libdata_buildings = FILTER libdata BY SQ_FEET > 0;
         grunt> state_flat = FOREACH libdata_buildings GENERATE
                          STABR AS State,SQ_FEET AS SquareFeet;
         grunt> state_grouped = GROUP state_flat BY State;
         grunt> state_footage = FOREACH state_grouped GENERATE
                             group as State,SUM(state_flat.SquareFeet)
                             AS TotalFeet:int;
         grunt> DUMP state_footage;

     The MapReduce job completes successfully and the output shows the square footage of the buildings.

10.  Add logic to filter the data by year, state, and building size, and save the relation to Cassandra using 
     the cql:// URL (which includes a prepared statement).

         grunt> insert_format= FOREACH state_footage GENERATE
                   TOTUPLE(TOTUPLE('year',2011),TOTUPLE('state',State)),TOTUPLE(TotalFeet);
         grunt> STORE insert_format INTO 'cql://libdata/libsqft?output_query=UPDATE%20libdata.libsqft%20SET%20sqft%20%3D%20%3F' USING CqlNativeStorage;

11.  When the MapReduce job completes, a message appears that the records were written successfully.

     In CQL, query the libsqft table to see the Pig results now stored in Cassandra.

         SELECT * FROM libdata.libsqft;

         year | state | sqft
         -----+-------+----------
         2011 |    AK |   570178
         2011 |    AL |  2792246
         ...
         2011 |    WV |  1075356
         2011 |    WY |   724821
