CREATE KEYSPACE IF NOT EXISTS npa_nxx 
WITH replication = { 'class': 'SimpleStrategy',
'replication_factor': '1' };

CREATE TABLE IF NOT EXISTS npa_nxx.npa_nxx_data 
(npa int, nxx int, latitude float, longitude float, 
state text, city text, PRIMARY KEY(npa, nxx));