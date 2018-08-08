/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */

package com.datastax.bdp.spark.demo.java;

import java.io.Serializable;

import com.google.common.base.Objects;
import org.joda.time.DateTime;

public class Return implements Serializable
{
    private String symbol;
    private DateTime date;
    private Double value;

    public Return()
    {
    }

    public Return(String symbol, DateTime date, Double value)
    {
        this.symbol = symbol;
        this.date = date;
        this.value = value;
    }

    public String getSymbol()
    {
        return symbol;
    }

    public void setSymbol(String symbol)
    {
        this.symbol = symbol;
    }

    public DateTime getDate()
    {
        return date;
    }

    public void setDate(DateTime date)
    {
        this.date = date;
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
                .add("symbol", symbol)
                .add("date", date)
                .add("value", value)
                .toString();
    }
}
