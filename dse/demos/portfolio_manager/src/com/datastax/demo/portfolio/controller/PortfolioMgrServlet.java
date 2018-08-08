/**
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.demo.portfolio.controller;

import org.apache.thrift.protocol.TJSONProtocol;
import org.apache.thrift.server.TServlet;

import com.datastax.demo.portfolio.PortfolioMgr;

public class PortfolioMgrServlet extends TServlet
{
    public PortfolioMgrServlet()
    {
        super(new PortfolioMgr.Processor(new PortfolioMgrHandler()), new TJSONProtocol.Factory());

    }


}
