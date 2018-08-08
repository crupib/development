/*
 * Copyright DataStax, Inc.
 *
 * Please see the included license file for details.
 */
package com.datastax.dse.demos.solr.commands;

import com.datastax.dse.demos.solr.CommandLine;

import java.util.List;

public class CmdRunnerFactory
{
    public static CmdRunner getInstance(List<String> cmd2Run, String fullUrl, CommandLine.Params params) throws Exception
    {
        String commandType = getCmdType(cmd2Run);

        if (commandType.equals(HttpReadCmdRunner.class.getName()))
        {
            return new HttpReadCmdRunner(fullUrl, params);
        }
        else if (commandType.equals(HttpWriteCmdRunner.class.getName()))
        {
            return new HttpWriteCmdRunner(fullUrl, params);
        }
        else if (commandType.equals(JavaDriverCmdRunner.class.getName()))
        {
            return new JavaDriverCmdRunner(fullUrl, params);
        }
        else if (commandType.equals(LucenePerfCmdRunner.class.getName()))
        {
            return new LucenePerfCmdRunner(fullUrl);
        }
        else if (commandType.equals(HttpUpdateCmdRunner.class.getName()))
        {
            return new HttpUpdateCmdRunner(fullUrl, params);
        }
        else if (GenerateQueriesCmdRunner.class.getName().equals(commandType))
        {
            return new GenerateQueriesCmdRunner(params);
        }
        else if (IndexLatencyCmdRunner.class.getName().equals(commandType))
        {
            return new IndexLatencyCmdRunner(params);
        }
        else if(GenerateIndexLatencyTestFileCmdRunner.class.getName().equals(commandType)) {
            return new GenerateIndexLatencyTestFileCmdRunner();
        }
        else
        {
            throw new Exception("Sorry I could not understand the command: " + cmd2Run.get(0));
        }
    }

    public static String getCmdType(List<String> cmd2Run) throws Exception
    {
        if (cmd2Run.get(0).toUpperCase().startsWith(HttpReadCmdRunner.CMD_STRING_ID))
        {
            return HttpReadCmdRunner.class.getName();
        }
        else if (cmd2Run.get(0).toUpperCase().startsWith(HttpWriteCmdRunner.CMD_STRING_ID))
        {
            return HttpWriteCmdRunner.class.getName();
        }
        else if (cmd2Run.get(0).toUpperCase().startsWith(JavaDriverCmdRunner.CMD_STRING_ID))
        {
            return JavaDriverCmdRunner.class.getName();
        }
        else if (cmd2Run.get(0).toUpperCase().startsWith(LucenePerfCmdRunner.CMD_STRING_ID))
        {
            return LucenePerfCmdRunner.class.getName();
        }
        else if (cmd2Run.get(0).toUpperCase().startsWith(HttpUpdateCmdRunner.CMD_STRING_ID))
        {
            return HttpUpdateCmdRunner.class.getName();
        }
        else if (cmd2Run.get(0).toUpperCase().startsWith(GenerateQueriesCmdRunner.CMD_STRING_ID))
        {
            return GenerateQueriesCmdRunner.class.getName();
        }
        else if (cmd2Run.get(0).toUpperCase().startsWith(IndexLatencyCmdRunner.CMD_STRING_ID))
        {
            return IndexLatencyCmdRunner.class.getName();
        }
        else if (cmd2Run.get(0).toUpperCase().startsWith(GenerateIndexLatencyTestFileCmdRunner.CMD_STRING_ID))
        {
            return GenerateIndexLatencyTestFileCmdRunner.class.getName();
        }
        else
        {
            throw new Exception("Sorry I could not understand the command: " + cmd2Run.get(0));
        }
    }
}
