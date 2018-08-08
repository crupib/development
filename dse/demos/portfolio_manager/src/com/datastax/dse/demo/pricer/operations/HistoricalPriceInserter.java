/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demo.pricer.operations;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

import com.datastax.dse.demo.pricer.Pricer;
import com.datastax.dse.demo.pricer.util.Operation;

import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.thrift.KeySlice;
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.cassandra.thrift.Cassandra.Client;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.thrift.TException;
import org.joda.time.LocalDate;

public class HistoricalPriceInserter extends Operation
{
    private static final LocalDate today = LocalDate.fromDateFields(new Date(System.currentTimeMillis()));
       
    public HistoricalPriceInserter(int idx)
    {
        super(idx);
    }

    public void run(Cassandra.Iface client) throws IOException
    {

        //Create a stock price per day
        Map<ByteBuffer, Map<String, List<Mutation>>> record = new HashMap<ByteBuffer, Map<String, List<Mutation>>>(tickers.length);

        LocalDate histDate = today.minusDays(index);
        ByteBuffer histDateBuf = ByteBufferUtil.bytes(histDate.toString("yyyy-MM-dd"));
        
        for(String stock : loadStocks(client))
        {
            record.put(ByteBufferUtil.bytes(stock), genDaysPrices(histDateBuf));
        }
        

        long start = System.currentTimeMillis();

        boolean success = false;
        String exceptionMessage = null;

        for (int t = 0; t < session.getRetryTimes(); t++)
        {
            if (success)
                break;

            try
            {
                client.batch_mutate(record, session.getConsistencyLevel());
                success = true;
            }
            catch (Exception e)
            {
                exceptionMessage = getExceptionMessage(e);
                success = false;
            }
        }

        if (!success)
        {
            error(String.format("Operation [%d] retried %d times - error inserting key %s %s%n",
                                index,
                                session.getRetryTimes(),
                                histDate,
                                (exceptionMessage == null) ? "" : "(" + exceptionMessage + ")"));
        }

        session.operations.getAndIncrement();
        session.keys.addAndGet(tickers.length);
        session.latency.getAndAdd(System.currentTimeMillis() - start);
    }

    private Map<String,List<Mutation>> genDaysPrices(ByteBuffer date)
    {
        Map<String, List<Mutation>> prices = new HashMap<String,List<Mutation>>();
             
        Mutation m = new Mutation();
        m.setColumn_or_supercolumn(new ColumnOrSuperColumn().setColumn(
                new Column()
                .setName(date)
                .setValue(ByteBufferUtil.bytes(Pricer.randomizer.nextDouble()*1000))
                .setTimestamp(System.currentTimeMillis()) 
                ));
        
        prices.put("StockHist", Arrays.asList(m));
        
        return prices;       
    }
    

}
