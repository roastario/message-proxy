package com.stefano.weaveworks;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import sun.misc.Signal;

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
        ProxyServer server = new ProxyServer(commandLine.getListenAddress(),
                commandLine.getListenPort(),
                commandLine.getForwardAddress(),
                commandLine.getForwardPort(), new StatsCollector()
        );
        Signal signal = new Signal("USR2");
        Signal.handle(signal, recievedSignal -> server.dumpToOutput());
        server.startAndWait();
    }


}
