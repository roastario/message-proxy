package com.stefano.weaveworks.stats;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import com.stefano.weaveworks.Message;

/**
 * Author stefanofranz
 */
public class StatsHelper {

    public static double getMessagesPerSecondWithinWindow(long secondsAllowed, long startTime, Collection<Message> queue) {
        long milliesAllowed = TimeUnit.SECONDS.toMillis(secondsAllowed);
        long messagesWithinWindow = queue.stream().filter(message -> (startTime - message.getTimeStamp()) <= milliesAllowed).count();
        return messagesWithinWindow / (double) secondsAllowed;
    }

    public static Queue<Message> trimListToWindowSize(long millisToAllow, long startTime, Queue<Message> queue) {
        while (!queue.isEmpty() && ((startTime - queue.peek().getTimeStamp()) > millisToAllow)) {
            queue.poll();
        }
        return queue;
    }

}
