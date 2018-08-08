About the Mahout Demo 
------------------------ 

This mahout demo consists of a data file (synthetic_control.data). The
demo determines with some percentage of certainty which entries in the
input data remained statistically in control and which have not. The
input data is time series historical data. Using the Mahout algorithms,
the demo classifies the data into categories based on whether it
exhibited relatively stable behavior over a period of time. The demo
produces a file of classified results.

To run the demo, you just type:

   ./run_mahout_example.sh

When the demo completes, a message appears on the standard output about
the location of the output file. For example:

   The output is in /tmp/clusteranalyze.txt

You can monitor the progress of the Hadoop job in OpsCenter if you
have access.

