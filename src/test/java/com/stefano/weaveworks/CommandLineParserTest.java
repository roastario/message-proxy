package com.stefano.weaveworks;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;

import org.junit.Assert;
import org.junit.Test;

/**
 * Author stefanofranz
 */
public class CommandLineParserTest {

    @Test
    public void shouldParseValidCommandLine() throws Exception {
        String[] validArgs = new String[]{
                "-forwardAddress", "proxy-host.com",
                "-forwardPort", "8001",
                "-listenPort", "8002",
                "-listenAddress", "localhost"
        };
        CommandLineParser parser = CommandLineParser.fromAgs(validArgs);
        Assert.assertThat(parser.getForwardAddress(), is("proxy-host.com"));
        Assert.assertThat(parser.getForwardPort(), is(8001));
        Assert.assertThat(parser.getListenPort(), is(8002));
        Assert.assertThat(parser.getListenAddress(), is("localhost"));
    }

    @Test
    public void shouldDefaultToAllAddressIfNoListenAddressSpecified() throws Exception {
        String[] validArgs = new String[]{
                "-forwardAddress", "proxy-host.com",
                "-forwardPort", "8001",
                "-listenPort", "8002",
        };
        CommandLineParser parser = CommandLineParser.fromAgs(validArgs);
        Assert.assertThat(parser.getListenAddress(), is("0.0.0.0"));
    }

    @Test
    public void shouldReturnNullIfInvalidArgsPassed() throws Exception {

        String[] missingForwardAddress = new String[]{
                "-forwardPort", "8001",
                "-listenPort", "8002",
                "-listenAddress", "localhost"
        };
        String[] missingForwardPort = new String[]{
                "-forwardAddress", "proxy-host.com",
                "-listenPort", "8002",
        };
        String[] missingListenPort = new String[]{
                "-forwardAddress", "proxy-host.com",
                "-forwardPort", "8001",
        };

        String[] malFormedPort = new String[]{
                "-forwardAddress", "proxy-host.com",
                "-forwardPort", "this is not a number",
                "-listenPort", "8002",
        };


        Assert.assertThat(CommandLineParser.fromAgs(missingForwardAddress), is(nullValue()));
        Assert.assertThat(CommandLineParser.fromAgs(missingForwardPort), is(nullValue()));
        Assert.assertThat(CommandLineParser.fromAgs(missingListenPort), is(nullValue()));
        Assert.assertThat(CommandLineParser.fromAgs(malFormedPort), is(nullValue()));

    }
}