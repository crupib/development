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

import org.apache.cassandra.db.marshal.LongType;
import org.apache.cassandra.thrift.*;
import org.apache.cassandra.utils.ByteBufferUtil;

public class PortfolioInserter extends Operation
{

    public PortfolioInserter(int index)
    {
        super(index);
    }

    public void run(Cassandra.Iface client) throws IOException
    {
        String[] stocks  = generatePortfolio(session.getColumnsPerKey(), client);
        List<Column> columns = new ArrayList<Column>();
        
        for (int i = 0; i < stocks.length; i++)
        {
            columns.add(new Column().setName(ByteBufferUtil.bytes(stocks[i])).setValue(ByteBufferUtil.bytes((long)Pricer.randomizer.nextInt(50))).setTimestamp(System.currentTimeMillis()));
        }
       
        String rawKey = String.valueOf(index);
        Map<ByteBuffer, Map<String, List<Mutation>>> record = new HashMap<ByteBuffer, Map<String, List<Mutation>>>();

        record.put(LongType.instance.fromString(rawKey), getColumnsMutationMap("Portfolios",columns));

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


    public static Map<String, List<Mutation>> getColumnsMutationMap(String cfName, List<Column> columns)
    {
        List<Mutation> mutations = new ArrayList<Mutation>();
        Map<String, List<Mutation>> mutationMap = new HashMap<String, List<Mutation>>();

        for (Column c : columns)
        {
            ColumnOrSuperColumn column = new ColumnOrSuperColumn().setColumn(c);
            mutations.add(new Mutation().setColumn_or_supercolumn(column));
        }

        mutationMap.put(cfName, mutations);

        return mutationMap;
    }
}
