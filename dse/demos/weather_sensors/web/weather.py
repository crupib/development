import json
import time

from datetime import datetime
from flask import Flask, request
from flask import render_template

from cassandra.cluster import Cluster
from cassandra.query import dict_factory
import pyhs2
from thrift.transport.TTransport import TTransportException


app = Flask(__name__)

app.secret_key='A0Zr98j/3yX R~XHH!jmN]LWX/,?RT'

def json_serial(obj):
    if isinstance(obj, datetime):
        serial = obj.isoformat()
    return serial

METRICS = ['temperature', 'humidity', 'dewpoint', 'winddirection', 'windspeed', 'barometricpressure', 'precipitation']
STATS = ['min', 'mean', 'max', 'percentile1', 'percentile5', 'median', 'percentile95', 'percentile99', 'total']




class CQL3Weather():
    def __init__(self):
        # Set up CQL3 python-driver connections and dictionary factory for results
        self.cluster = Cluster()
        self.session = self.cluster.connect()
        self.session.row_factory = dict_factory

        # Create prepared statements to simplify requests and increase throughput
        self.prepared_get_monthly_report = self.session.prepare("SELECT * FROM weathercql.monthly WHERE stationid = ? AND metric = ?")
        self.prepared_get_daily_report = self.session.prepare("SELECT * FROM weathercql.daily WHERE stationid = ? AND metric = ?")
        self.prepared_get_location =  self.session.prepare("SELECT location from weathercql.station WHERE stationid = ?")

    def get_stations(self, stationid=None):
        '''
        Returns either a full list of stations' full names, or the full name of a specific station.
        # TODO: Implement paging for large datasets.
        '''

        if stationid:
            return self.session.execute("SELECT stationid, location FROM weathercql.station WHERE stationid = %s", (stationid,))
        else:
            return self.session.execute("SELECT stationid, location FROM weathercql.station")

    def get_monthly_report(self, stationid, metric):
        '''
        Use a prepared statement to request a full monthly report.
        # TODO: Implement paging for large datasets.
        '''

        return self.session.execute(self.prepared_get_monthly_report, [stationid, metric])

    def get_daily_report(self, stationid, metric):
        '''
        Use a prepared statement to request a full daily report.
        # TODO: Implement paging for large datasets.
        '''

        return self.session.execute(self.prepared_get_daily_report, [stationid, metric])

    def get_series(self, stationid, metric, monthly=True):
        '''
        Format results into json-ready results for Rickshaw.js.
        '''

        allResults = {}
        if monthly:
            rs = self.get_monthly_report(stationid, metric)
        else:
            rs = self.get_daily_report(stationid, metric)
        for field in STATS:
            series = self.format_series(rs, field)
            allResults[field] = series
        return json.dumps(allResults,  default=json_serial)

    def format_series(self, records, field):
        '''
        JSON formatting helper.
        '''

        data = []
        for record in records:
            data.append({'x' : time.mktime(record['date'].timetuple()), 'y' : record[field]})
        return data




def query(server, port, query_string):
    '''
    Helper method for running Hive queries.
    '''

    app.logger.debug("attempting to connect to: %s:%s" % (server, port))
    conn = pyhs2.connect(
        host=server,
        port=port,
        authMechanism="PLAIN",
        user='test',
        password='test',
        database='default'
    )
    cur = conn.cursor()

    app.logger.debug("sending query: %s" % (query_string))
    cur.execute(query_string)
    result = {'rows': []}
    try:
        for row in cur.fetch():
            result['rows'].append(row)
    except Exception as e:
        result['error_message'] = e.message

    return result


@app.route('/')
def weather():
    '''
    Displays index.html.
    '''

    app.logger.debug("visiting index")
    weather = CQL3Weather()
    stations = weather.get_stations()
    return render_template('index.html', stations=stations)

@app.route('/report/<rollup>/<stationid>/')
@app.route('/report/<rollup>/<stationid>/<metric>/')
def report(rollup=None, stationid=None, metric='temperature'):
    '''
    Displays report.html, not the charts.
    '''

    app.logger.debug("running report")
    weather = CQL3Weather()
    stations = weather.get_stations()

    if rollup == 'daily':
        rollup = False
    else:
        rollup = True

    return render_template('report.html',
                           stationid=stationid,
                           stations=stations,
                           metric=metric,
                           metrics=METRICS,
                           stats=STATS,
                           monthly=rollup)

@app.route('/sample_queries/')
def sample_queries():
    '''
    Displays sample_queries.html, not the data.
    '''

    weather = CQL3Weather()
    stations = weather.get_stations()
    return render_template('sample_queries.html',
                           stations=stations)

@app.route('/custom_queries/')
def custom_queries():
    '''
    Displays custom_queries.html, not the data.
    '''

    weather = CQL3Weather()
    stations = weather.get_stations()
    return render_template('custom_queries.html',
                           stations=stations)

@app.route('/cfs_queries/')
def cfs_queries():
    '''
    Displays cfs_queries.html, not the data.
    '''

    weather = CQL3Weather()
    stations = weather.get_stations()
    return render_template('cfs_queries.html',
                           stations=stations)


@app.route('/series/<rollup>/<stationid>/')
@app.route('/series/<rollup>/<stationid>/<metric>/')
def series(rollup=None, stationid=None, metric='temperature'):
    '''
    Returns the JSON formatted series for Rickshaw.js.
    '''

    weather = CQL3Weather()
    if rollup == 'daily':
        result = weather.get_series(stationid, metric, monthly=False)
    else:
        result = weather.get_series(stationid, metric, monthly=True)

    return result

@app.route('/run_query/', methods=['GET', 'POST'])
def run_query():
    # Start timer
    start = time.time()

    # Initialize variables
    error_message = None
    results = []
    query_results = {}

    # Grab POST variables
    query_string = request.form['query_string']
    service = request.form['service']

    # for BYOH code
    server = request.form['server'] if 'server' in request.form and request.form['server'] else '127.0.0.1'
    port = request.form['port'] if 'port' in request.form and request.form['port'] else 5588

    try:
        port = int(port)
    except:
        error_message = 'Port "%s" does not appear to be a valid port.' % (port)
        return json.dumps({
            'error_message': error_message,
            'elapsed': round(time.time() - start, 4),
            'record_count': len(results),
            'results': results,
        })

    # Set Hive variables
    if service == 'spark':
        port = 5588
    elif service == 'hadoop':
        port = 5587

    # Properly format query_string and submit to hive server
    query_string = query_string.strip().strip(';')
    try:
        query_results = query(server, port, query_string)

        # Save any error messages to report back to the front end
        if 'error_message' in query_results:
            error_message = query_results['error_message']
    except TTransportException as e:
        # Report transport exceptions
        error_message = 'No Service Found: ' + e.message

    # Extract Hive results
    if query_results and 'rows' in query_results and query_results['rows']:
        results = query_results['rows']

    return json.dumps({
        'error_message': error_message,
        'elapsed': round(time.time() - start, 4),
        'record_count': len(results),
        'results': results,
    })


if __name__ == "__main__":
    # Allow access from all hosts, to facilitate running on external servers
    # Allow for a threaded run for multiple requests
    # Display errors with formatted stacktraces on the front-end
    app.run(host='0.0.0.0', port=8983, threaded=True, debug=True)
