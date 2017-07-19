package com.stefano.weaveworks;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ExecutionException;

import com.stefano.weaveworks.signals.PosixSignalsHandling;
import com.stefano.weaveworks.stats.StatsCollector;

/**
 * Created by stefano
 */
public class Main {

    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
        final CommandLineParser commandLine = CommandLineParser.fromAgs(args);
        if (commandLine == null) {
            System.exit(1);
        }
        printInfoToStdErr(commandLine);
        ProxyServer server = new ProxyServer(commandLine.getListenAddress(),
                commandLine.getListenPort(),
                commandLine.getForwardAddress(),
                commandLine.getForwardPort(),
                new StatsCollector()
        );
        PosixSignalsHandling.registerHandler("USR2", (signal) -> server.dumpToOutput());
        server.startAndWait();
    }

    private static void printInfoToStdErr(CommandLineParser commandLine) {
        System.err.println("Starting proxy server "
                + commandLine.getListenAddress()
                + ":"
                + commandLine.getListenPort()
                + " -> "
                + commandLine.getForwardAddress()
                + ":"
                + commandLine.getForwardPort()
                + " send SIGUSR2 signal to pid: " + attemptToGetPid());
    }

    private static String attemptToGetPid() {
        try {
            String id = ManagementFactory.getRuntimeMXBean().getName();
            return id.split("@")[0];
        } catch (RuntimeException e) {
            return "UNKNOWN PID";
        }
    }


}
