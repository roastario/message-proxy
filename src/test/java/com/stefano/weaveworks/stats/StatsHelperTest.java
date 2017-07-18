package com.stefano.weaveworks.stats;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.Is.is;

import java.util.LinkedList;
import java.util.PriorityQueue;

import org.junit.Assert;
import org.junit.Test;

import com.stefano.weaveworks.Message;

/**
 * Author stefanofranz
 */
public class StatsHelperTest {

    @Test
    public void shouldTrimQueueToCorrectSizeIfSorted() throws Exception {
        PriorityQueue<Message> queue = new PriorityQueue<>(Message.oldestMessageFirst());
        queue.add(new Message("REQ 1 Hey", Message.MessageType.REQ, 1, "Hey", 1000));
        queue.add(new Message("REQ 2 Hey", Message.MessageType.REQ, 2, "Hey", 1999));
        queue.add(new Message("REQ 3 Hey", Message.MessageType.REQ, 3, "Hey", 3000));
        queue.add(new Message("REQ 4 Hey", Message.MessageType.REQ, 4, "Hey", 4000));
        queue.add(new Message("REQ 5 Hey", Message.MessageType.REQ, 5, "Hey", 5000));
        queue.add(new Message("REQ 6 Hey", Message.MessageType.REQ, 6, "Hey", 6000));
        StatsHelper.trimListToWindowSize(3000, 6000, queue);
        Assert.assertThat(queue.size(), is(4));
        Assert.assertThat(queue, containsInAnyOrder(
                new Message("REQ 3 Hey", Message.MessageType.REQ, 3, "Hey", 3000),
                new Message("REQ 4 Hey", Message.MessageType.REQ, 4, "Hey", 4000),
                new Message("REQ 5 Hey", Message.MessageType.REQ, 5, "Hey", 5000),
                new Message("REQ 6 Hey", Message.MessageType.REQ, 6, "Hey", 6000)
        ));
    }

    @Test
    public void shouldTrimQueueToCorrectSizeIfOutOfOrder() throws Exception {
        PriorityQueue<Message> queue = new PriorityQueue<>(Message.oldestMessageFirst());
        queue.add(new Message("REQ 2 Hey", Message.MessageType.REQ, 2, "Hey", 1999));
        queue.add(new Message("REQ 6 Hey", Message.MessageType.REQ, 6, "Hey", 6000));
        queue.add(new Message("REQ 3 Hey", Message.MessageType.REQ, 3, "Hey", 3000));
        queue.add(new Message("REQ 5 Hey", Message.MessageType.REQ, 5, "Hey", 5000));
        queue.add(new Message("REQ 1 Hey", Message.MessageType.REQ, 1, "Hey", 1000));
        queue.add(new Message("REQ 4 Hey", Message.MessageType.REQ, 4, "Hey", 4000));
        StatsHelper.trimListToWindowSize(3000, 6000, queue);
        Assert.assertThat(queue.size(), is(4));
        Assert.assertThat(queue, containsInAnyOrder(
                new Message("REQ 6 Hey", Message.MessageType.REQ, 6, "Hey", 6000),
                new Message("REQ 3 Hey", Message.MessageType.REQ, 3, "Hey", 3000),
                new Message("REQ 5 Hey", Message.MessageType.REQ, 5, "Hey", 5000),
                new Message("REQ 4 Hey", Message.MessageType.REQ, 4, "Hey", 4000)
        ));
    }

    @Test
    public void shouldCalculateMessagesPerSecondForSortedWindow() throws Exception {

        LinkedList<Message> queue = new LinkedList<>();
        queue.add(new Message("REQ 0 Hey", Message.MessageType.REQ, 0, "Hey", 0));
        queue.add(new Message("REQ 1 Hey", Message.MessageType.REQ, 1, "Hey", 1000));
        queue.add(new Message("REQ 2 Hey", Message.MessageType.REQ, 2, "Hey", 1999));
        queue.add(new Message("REQ 3 Hey", Message.MessageType.REQ, 3, "Hey", 3000));
        queue.add(new Message("REQ 4 Hey", Message.MessageType.REQ, 4, "Hey", 4000));
        queue.add(new Message("REQ 5 Hey", Message.MessageType.REQ, 5, "Hey", 5000));
        queue.add(new Message("REQ 6 Hey", Message.MessageType.REQ, 6, "Hey", 6000));

        double messagesASecond = StatsHelper.getMessagesPerSecondWithinWindow(6, 6000, queue);
        //7 messages within a 6 second window
        Assert.assertThat(messagesASecond, is(7 / (double) 6));

        messagesASecond = StatsHelper.getMessagesPerSecondWithinWindow(6, 6001, queue);
        //message 0 is now outside the window
        Assert.assertThat(messagesASecond, is(6 / (double) 6));

        messagesASecond = StatsHelper.getMessagesPerSecondWithinWindow(5, 10000, queue);
        //messages 0,1,2,3,4 are now outside the window
        Assert.assertThat(messagesASecond, is(2 / (double) 5));
    }
}