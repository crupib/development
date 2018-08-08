/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */

package com.datastax.bdp.spark.demo.java;

import java.io.Serializable;

import com.google.common.base.Objects;

public class Portfolios implements Serializable
{
    private Long key;
    private String symbol;
    private Double value;

    public Portfolios()
    {
    }

    public Portfolios(Long key, String symbol, Double value)
    {
        this.key = key;
        this.symbol = symbol;
        this.value = value;
    }

    public Long getKey()
    {
        return key;
    }

    public void setKey(Long key)
    {
        this.key = key;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public Double getValue()
    {
        return value;
    }

    public void setValue(Double value)
    {
        this.value = value;
    }


    @Override
    public String toString()
    {
        return Objects.toStringHelper(this)
                .add("key", key)
                .add("symbol", symbol)
                .add("value", value)
                .toString();
    }
}
