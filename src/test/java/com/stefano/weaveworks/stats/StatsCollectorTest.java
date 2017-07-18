package com.stefano.weaveworks.stats;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

import static com.stefano.weaveworks.stats.StatsJSONifierTest.containsPattern;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.stefano.weaveworks.Message;

/**
 * Author stefanofranz
 */
public class StatsCollectorTest {


    @Test
    public void shouldTrackNumberOfVariousMessages() throws Exception {
        LinkedList<Message> queue = new LinkedList<>();
        queue.add(new Message("REQ 1 Hey", Message.MessageType.REQ, 1, "Hey", 1000));
        queue.add(new Message("ACK 2 Hey", Message.MessageType.ACK, 2, "Hey", 1999));
        queue.add(new Message("REQ 3 Hey", Message.MessageType.REQ, 3, "Hey", 3000));
        queue.add(new Message("ACK 4 Hey", Message.MessageType.ACK, 4, "Hey", 4000));
        queue.add(new Message("REQ 5 Hey", Message.MessageType.REQ, 5, "Hey", 5000));
        queue.add(new Message("NAK 6", Message.MessageType.NAK, 6, null, 6000));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        StatsCollector statsCollector = new StatsCollector(System.out, executorService, () -> 6000L);
        queue.forEach(statsCollector);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        Assert.assertThat(statsCollector.getTotalMessages(), is(equalTo(6L)));
        Assert.assertThat(statsCollector.getAckTotal(), is(equalTo(2L)));
        Assert.assertThat(statsCollector.getNakTotal(), is(equalTo(1L)));
        Assert.assertThat(statsCollector.getRequestTotal(), is(equalTo(3L)));
    }

    @Test
    public void shouldTrackRateOfMessagesWithinGivenWindow() throws Exception {
        LinkedList<Message> queue = new LinkedList<>();
        queue.add(new Message("REQ 1 Hey", Message.MessageType.REQ, 1, "Hey", 1000));
        queue.add(new Message("ACK 2 Hey", Message.MessageType.ACK, 2, "Hey", 1999));
        queue.add(new Message("REQ 3 Hey", Message.MessageType.REQ, 3, "Hey", 3000));
        queue.add(new Message("ACK 4 Hey", Message.MessageType.ACK, 4, "Hey", 4000));
        queue.add(new Message("REQ 5 Hey", Message.MessageType.REQ, 5, "Hey", 5000));
        queue.add(new Message("REQ 6 Hey", Message.MessageType.REQ, 6, "Hey", 5000));
        queue.add(new Message("REQ 7 Hey", Message.MessageType.REQ, 7, "Hey", 5000));
        queue.add(new Message("NAK 8", Message.MessageType.NAK, 8, null, 6000));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        StatsCollector statsCollector = new StatsCollector(System.out, executorService, () -> 6000L);
        queue.forEach(statsCollector);
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);

        double requestRateWithinTwoSeconds = statsCollector.getRequestRateWithinWindow(2, TimeUnit.SECONDS, 6000);
        double responseRateWithinFiveSeconds = statsCollector.getResponseRateWithinWindow(4, TimeUnit.SECONDS, 6000);
        //3/2 -> 1.5
        Assert.assertThat(requestRateWithinTwoSeconds, is(3 / 2d));
        // (ACK 4 + NAK 8) -> 2/4 -> 0.5
        Assert.assertThat(responseRateWithinFiveSeconds, is(2 / 4d));
    }

    @Test
    public void shouldDumpToOutputWhenRequested() throws Exception {

        LinkedList<Message> queue = new LinkedList<>();
        queue.add(new Message("REQ 1 Hey", Message.MessageType.REQ, 1, "Hey", 1000));
        queue.add(new Message("ACK 2 Hey", Message.MessageType.ACK, 2, "Hey", 1999));
        queue.add(new Message("REQ 3 Hey", Message.MessageType.REQ, 3, "Hey", 3000));
        queue.add(new Message("ACK 4 Hey", Message.MessageType.ACK, 4, "Hey", 4000));
        queue.add(new Message("REQ 5 Hey", Message.MessageType.REQ, 5, "Hey", 5000));
        queue.add(new Message("NAK 6", Message.MessageType.NAK, 6, null, 6000));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        StatsCollector statsCollector = new StatsCollector(new PrintStream(outputStream), executorService, () -> 6000L);
        queue.forEach(statsCollector);
        statsCollector.scheduleStatsDump();
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
        String dumpedOutput = new String(outputStream.toByteArray());
        Assert.assertThat(dumpedOutput, containsPattern("\"msg_total\":\\s*6"));
        Assert.assertThat(dumpedOutput, containsPattern("\"msg_req\":\\s*3"));
        Assert.assertThat(dumpedOutput, containsPattern("\"msg_ack\":\\s*2"));
        Assert.assertThat(dumpedOutput, containsPattern("\"msg_nak\":\\s*1"));
        Assert.assertThat(dumpedOutput, containsPattern("\"msg_total\":\\s*6"));

        //requests = 3
        //responses = 3

        //requests within 1 second of 6000 = 1
        //responses within 1 second of 6000 = 1
        Assert.assertThat(dumpedOutput, containsPattern("\"request_rate_1s\":\\s*1.0"));
        Assert.assertThat(dumpedOutput, containsPattern("\"response_rate_1s\":\\s*1.0"));

        //requests within 10seconds of 6000 = 3
        //responses within 10seconds of 6000 = 3
        Assert.assertThat(dumpedOutput, containsPattern("\"request_rate_10s\":\\s*0.3"));
        Assert.assertThat(dumpedOutput, containsPattern("\"response_rate_10s\":\\s*0.3"));
    }
}