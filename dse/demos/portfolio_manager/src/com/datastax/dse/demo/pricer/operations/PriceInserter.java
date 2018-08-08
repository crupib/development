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
import org.apache.cassandra.thrift.Mutation;
import org.apache.cassandra.utils.ByteBufferUtil;

public class PriceInserter extends Operation
{

    public PriceInserter(int index)
    {
        super(index);
    }

    public void run(Cassandra.Iface client) throws IOException
    {
        //Pick random stock
        String rawKey = PortfolioInserter.tickers[Pricer.randomizer.nextInt(PortfolioInserter.tickers.length)];

        List<Column> columns = new ArrayList<Column>();
        
        columns.add(new Column().setName(ByteBufferUtil.bytes("price")).setValue(ByteBufferUtil.bytes(Pricer.randomizer.nextDouble()*100)).setTimestamp(System.currentTimeMillis()));
    
        
        Map<ByteBuffer, Map<String, List<Mutation>>> record = new HashMap<ByteBuffer, Map<String, List<Mutation>>>();

        record.put(ByteBufferUtil.bytes(rawKey),  PortfolioInserter.getColumnsMutationMap("Stocks", columns));

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
                                rawKey,
                                (exceptionMessage == null) ? "" : "(" + exceptionMessage + ")"));
        }

        session.operations.getAndIncrement();
        session.keys.getAndIncrement();
        session.latency.getAndAdd(System.currentTimeMillis() - start);
    }
}
