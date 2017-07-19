package com.stefano.weaveworks.stats;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

import org.hamcrest.Matcher;
import org.hamcrest.core.SubstringMatcher;
import org.junit.Assert;
import org.junit.Test;

/**
 * Author stefanofranz
 */
public class StatsJSONifierTest {


    @Test(timeout = 2000L)
    public void shouldJsonifyWithSpecifiedNames() throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream writer = new PrintStream(outputStream);
        StatsJSONifier jsoNifier = new StatsJSONifier(writer);
        jsoNifier.jsonify(200, 100, 50, 50, .5, .2, .6, .3);
        writer.flush();
        String output = outputStream.toString();
        Assert.assertThat(output, containsPattern("\"msg_total\":\\s*200"));
        Assert.assertThat(output, containsPattern("\"msg_req\":\\s*100"));
        Assert.assertThat(output, containsPattern("\"msg_ack\":\\s*50"));
        Assert.assertThat(output, containsPattern("\"msg_nak\":\\s*50"));
        Assert.assertThat(output, containsPattern("\"request_rate_1s\":\\s*0.5"));
        Assert.assertThat(output, containsPattern("\"request_rate_10s\":\\s*0.2"));
        Assert.assertThat(output, containsPattern("\"response_rate_1s\":\\s*0.6"));
        Assert.assertThat(output, containsPattern("\"response_rate_10s\":\\s*0.3"));

    }

    public static Matcher<String> containsPattern(String pattern) {
        return new SubstringMatcher(pattern) {
            @Override
            protected boolean evalSubstringOf(String s) {
                return Pattern.compile(pattern).matcher(s).find();
            }
            @Override
            protected String relationship() {
                return "containing pattern";
            }
        };

    }
}