#!/bin/bash
#Get the GMT timestamp of the last batch
LAST_BATCH=$(expr $(date +%s) - $(date +%s) % 5)000
TOPHIT=$(echo "select * from requests_ks.sorted_urls where time = '$LAST_BATCH' ORDER BY count desc limit 1;" | cqlsh `hostname -f`)
TOPURL=$(echo $TOPHIT | grep -oe "/\S*")

echo "Displayling the Top 5 Url's from the Last Batch and The Top Url's Performance per Method in the Last 3 Batches";
echo "select * from requests_ks.sorted_urls where time = $LAST_BATCH ORDER BY count desc limit 5;
select url, count, time, method from requests_ks.method_agg where url='$TOPURL' and method='GET' ORDER BY time desc limit 3;
select url, count, time, method from requests_ks.method_agg where url='$TOPURL' and method='PUT' ORDER BY time desc limit 3;
select url, count, time, method from requests_ks.method_agg where url='$TOPURL' and method='DELETE' ORDER BY time desc limit 3;
select url, count, time, method from requests_ks.method_agg where url='$TOPURL' and method='POST' ORDER BY time desc limit 3;
" | cqlsh `hostname -f`
echo "End this process (ctrl-c) to end the demo "
