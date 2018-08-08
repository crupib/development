This is a standalone Spark application that use MLlib to solve iris classification problem
It is updated to Spark 1.x version of the following tutorial http://www.datastax.com/dev/blog/interactive-advanced-analytic-with-dse-and-spark-mllib 

Steps:

1. Start a DSE cluster with Analytic DC with Spark enabled
    $ dse cassandra -k -f

2. build the application:
   $ gradle 

3. Run the application:
   $  dse spark-submit NaiveBayesDemo.jar

  Or for build and run just
   $ gradle -q run

Source code for the demo can be found under <install_location>/demos/spark-mllib/src.

Note: The demo reads iris.csv data file from a  local directory. The file must be accessible at the same path on all worker/dse nodes. Thus, if you move the demo directory to other location, please verify that the file is located in shared folder and all DSE spark worker has read access to the file.
The custom file location could be passed as parameter:
dse spark-submit NaiveBayesDemo.jar  /mnt/shared/iris.csv
