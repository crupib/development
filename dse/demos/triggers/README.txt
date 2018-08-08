This is a simple demo of Cassandra Triggers.  Triggers allow you to
run custom Java code any time a ColumnFamily is modified; for more
details you can read Aleksey Yeschenko's blog post:
http://www.datastax.com/dev/blog/whats-new-in-cassandra-2-0-prototype-triggers-support

The demo uses the InvertedIndex trigger to synchronize an inverted
index table: for every first->last name in the original table, the
inverted table will contain last->first.  

Important files:
  * InvertedIndex.java contains the source code for the trigger
  * InvertedIndex.properties contains options for the trigger
  * create_table.cql contains the CQL code for the demo

To run the demo:
  * ./gradlew build
    This step is optional because the demo is installed with the
    InvertedIndex-1.0.jar file already built in the root of the
    demo
  * Copy the ./InvertedIndex-1.0.jar file to the cassandra triggers
    folder. The location of this folder will depend on the type of
    installation:
    * Package installs (deb,rpm): /etc/dse/cassandra/triggers
    * Tarball installs: ../../resources/cassandra/conf/triggers
  * Copy the ./conf/InvertedIndex.properties file the the cassandra
    configuration folder. This will be either:
    * Package installs (deb, rpm): /etc/dse/cassandra
    * Tarball installs: ../../resources/cassandra/conf
  * Restart the dse service
  * ./triggers-demo.sh
  * ./verify-triggers-demo.sh
