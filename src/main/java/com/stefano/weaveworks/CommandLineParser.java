package com.stefano.weaveworks;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Created by stefano
 */
public class CommandLineParser {


    private final String forwardAddress;
    private final String listenAddress;
    private final int forwardPort;
    private final int listenPort;

    private CommandLineParser(String forwardAddress, String listenAddress, int forwardPort, int listenPort) {
        this.forwardAddress = forwardAddress;
        this.listenAddress = listenAddress;
        this.forwardPort = forwardPort;
        this.listenPort = listenPort;
    }


    public static CommandLineParser fromAgs(String[] args) {
        Options options = new Options();
        options.addOption(Option.builder("listenPort").hasArg()
                .required(true).desc("port to listen on").build());

        options.addOption(Option.builder("listenAddress").hasArg()
                .required(false).desc("address to listen on").build());

        options.addOption(Option.builder("forwardAddress").hasArg()
                .required(true).desc("address to forward connections to").build());

        options.addOption(Option.builder("forwardPort").hasArg()
                .required(true).desc("port to forward connections to").build());

        DefaultParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            String forwardAddress = cmd.getOptionValue("forwardAddress");
            String listenAddress = cmd.getOptionValue("listenAddress", "0.0.0.0");
            Integer listenPort = Integer.parseInt(cmd.getOptionValue("listenPort"));
            Integer forwardPort = (Integer.parseInt(cmd.getOptionValue("forwardPort")));
            return new CommandLineParser(forwardAddress, listenAddress, forwardPort, listenPort);
        } catch (RuntimeException | ParseException e) {
            formatter.printHelp("./proxy", options);
            return null;
        }
    }

    public String getForwardAddress() {
        return forwardAddress;
    }

    public String getListenAddress() {
        return listenAddress;
    }

    public int getForwardPort() {
        return forwardPort;
    }

    public int getListenPort() {
        return listenPort;
    }
}
