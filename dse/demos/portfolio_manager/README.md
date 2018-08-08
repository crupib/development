Portfolio Manager Demo Using DSE Hadoop
---------------------------------------

This demo simulates a financial portfolio manager application. Sample portfolio
data is generated, including a list of stocks, the number of shares purchased,
and the purchase price. The demo's pricer utility simulates real-time stock data
where each portfolio updates based on its overall value and the percentage of
gain or loss compared to the purchase price. This utility also generates 100
days of historical market data (the end-of-day price) for each stock. Finally,
a Hive MapReduce job calculates the greatest historical 10-day loss period for
each portfolio.

For more detailed instructions including screenshots of the sample Portfolio
web application, see the DataStax documentation website:
http://www.datastax.com/documentation/getting_started/doc/getting_started/gsDemos.html


Steps:

1. Start a DSE cluster with Hadoop and Spark enabled as Analytics nodes

2. Generate sample stock data for the application:

        $ cd install_location/demos/portfolio_manager
        $ bin/pricer --help
        $ bin/pricer -o INSERT_PRICES
        $ bin/pricer -o UPDATE_PORTFOLIOS
        $ bin/pricer -o INSERT_HISTORICAL_PRICES -n 100

   The pricer utility may take a couple minutes to run.

3. Start the web service:

        $ cd website
        $ sudo ./start

   Open a browser and go to http://localhost:8983/portfolio.

   The real-time Portfolio Manager demo application is displayed. Note that Historical
   Loss values will show as "?" because they have not yet been calculated.

4. Open another terminal window and run the Hive MapReduce job

        $ cd install_location/demos/portfolio_manager
        $ dse hive -f 10_day_loss.q

   The MapReduce job will take several minutes to run. To watch the progress in the job
   tracker, open the following URL in a browser: http://localhost:50030/jobtracker.jsp

6. After the job completes, refresh the Portfolio Manager web page. The results of
   the Largest Historical 10 day Loss for each portfolio are displayed.
