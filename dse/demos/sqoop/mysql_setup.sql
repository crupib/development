CREATE DATABASE IF NOT EXISTS npa_nxx_demo;

CONNECT npa_nxx_demo;

DROP TABLE IF EXISTS npa_nxx;

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

LOAD DATA LOCAL INFILE 'npa_nxx.csv'
   INTO TABLE npa_nxx_demo.npa_nxx 
   FIELDS TERMINATED BY ',' ENCLOSED BY '"' 
   LINES TERMINATED BY '\n'; 

DROP TABLE IF EXISTS npa_nxx_extract;

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
