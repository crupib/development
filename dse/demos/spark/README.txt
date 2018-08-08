This is a standalone Spark application performing the same set of queries
as the set of Hive queries in demos/portfolio_manager/10_day_loss.q file.

Steps:

1. Start a DSE cluster with Analytic DC with Spark enabled

2. Generate stock data for the application:
   $ cd install_location/demos/portfolio_manager
   $ bin/pricer -o INSERT_PRICES
   $ bin/pricer -o UPDATE_PORTFOLIOS
   $ bin/pricer -o INSERT_HISTORICAL_PRICES -n 100

3. Run the Spark Demo application:
   $ cd install_location/demos/spark
   $ ./10-day-loss.sh

4. The same processing steps are available in a Spark Java example:
   $ cd install_location/demos/spark
   $ ./10-day-loss-java.sh

Source code for both demos can be found under install_location/demos/spark/src.

For more information on the Portfolio Manager Demo, go to:
http://www.datastax.com/documentation/getting_started/doc/getting_started/gsDemos.html
