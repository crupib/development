/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package org.apache.cassandra.triggers;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.cassandra.db.Cell;
import org.apache.cassandra.db.ColumnFamily;
import org.apache.cassandra.db.Mutation;
import org.apache.cassandra.io.util.FileUtils;

public class InvertedIndex implements ITrigger
{
    private static final Logger logger = LoggerFactory.getLogger(InvertedIndex.class);
    private Properties properties = loadProperties();

    public Collection<Mutation> augment(ByteBuffer key, ColumnFamily update)
    {
        List<Mutation> mutations = new ArrayList<>();

        for (Cell cell : update)
        {
            // Skip the row marker, since it has no value which leads to an empty key.  See Cassandra-4361.
            if (cell.value().remaining() > 0)
            {
                Mutation mutation = new Mutation(properties.getProperty("keyspace"), cell.value());
                mutation.add(properties.getProperty("columnfamily"), cell.name(), key, System.currentTimeMillis());
                mutations.add(mutation);
            }
        }

        // The trigger is called before the update takes place, and any mutations are simply added into
        // the original list for the base statement.  This is clean and fast, but it can for some
        // confusing error messages as it is not easy to determine which ones were caused by the
        // trigger.
        return mutations;
    }

    private static Properties loadProperties()
    {
        Properties properties = new Properties();
        InputStream stream = InvertedIndex.class.getClassLoader().getResourceAsStream("InvertedIndex.properties");
        try {
            properties.load(stream);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            FileUtils.closeQuietly(stream);
        }
        logger.info("loaded property file, InvertedIndex.properties");
        return properties;
    }
}
