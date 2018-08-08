/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */

package com.datastax.bdp.spark.demo.java;

import java.io.Serializable;

import com.google.common.base.Objects;

public class HistLoss implements Serializable
{
    private String key;
    private String column1;
    private String value;

    public HistLoss()
    {
    }

    public HistLoss(String key, String column1, String value)
    {
        this.key = key;
        this.column1 = column1;
        this.value = value;
    }

    public String getKey()
    {
        return key;
    }

    public void setKey(String key)
    {
        this.key = key;
    }

    public String getColumn1()
    {
        return column1;
    }

    public void setColumn1(String column1)
    {
        this.column1 = column1;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }


    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("key", key)
                .add("column1", column1)
                .add("value", value)
                .toString();
    }
}
