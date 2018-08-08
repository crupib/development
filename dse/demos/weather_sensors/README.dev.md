# Weather Sensors Demo (Developer Guide)

This README includes additional information to customize and generate weather data manually.

## Build

The `create-and-load` utility and required jars have been pre-compiled and included in the `lib` directory.

To rebuild it, you'll first need to install Gradle:

    sudo apt-get install gradle

Then build the project with Gradle:

    cd demos/weather_sensors
    gradle build -x test

## Create and Load

Run the following script on a node that is running Cassandra:

    ./bin/create-and-generate [-y/--years <numberOfYears>] [-s/--stations <numberOfStations>] [-d/--node <nodeIPAddress>]

After running this script, you can then run the Hive script that will run analytics over the generated weather data.
Do be warned that each command takes more than 30 minutes on a laptop. From the dse directory this command would be:

    dse hive -hiveconf METRIC='temperature' -f demos/weather_sensors/resources/aggregates.q
    dse hive -hiveconf METRIC='humidity' -f demos/weather_sensors/resources/aggregates.q
    dse hive -hiveconf METRIC='dewpoint' -f demos/weather_sensors/resources/aggregates.q
    dse hive -hiveconf METRIC='winddirection' -f demos/weather_sensors/resources/aggregates.q
    dse hive -hiveconf METRIC='windspeed' -f demos/weather_sensors/resources/aggregates.q
    dse hive -hiveconf METRIC='barometricpressure' -f demos/weather_sensors/resources/aggregates.q
    dse hive -hiveconf METRIC='precipitation' -f demos/weather_sensors/resources/aggregates.q

## Copy aggregate data to flat files

For the packaging of this demo we then ran the following commands to facilitate faster load times:

    echo "COPY weathercql.monthly TO 'resources/monthly.csv' WITH HEADER='true';" | cqlsh
    echo "COPY weathercql.daily TO 'resources/daily.csv' WITH HEADER='true';" | cqlsh
    echo "COPY weathercql.station TO 'resources/station.csv' WITH HEADER='true';" | cqlsh
