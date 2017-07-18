package com.stefano.weaveworks.stats;

import java.io.PrintStream;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.stefano.weaveworks.pipe.listeners.ConnectionEndedListener;
import com.stefano.weaveworks.pipe.listeners.ConnectionErrorListener;
import com.stefano.weaveworks.Message;

/**
 * Author stefanofranz
 */
public class StatsCollector implements Consumer<Message>, ConnectionErrorListener, ConnectionEndedListener {

    private final static long MAX_MESSAGE_AGE = TimeUnit.SECONDS.toMillis(10);
    private final static Message PRINT_MARKER = new Message();

    private long reqTotal;
    private long ackTotal;
    private long nakTotal;
    private final PriorityQueue<Message> responseQueue = new PriorityQueue<>(Message.oldestMessageFirst());
    private final PriorityQueue<Message> reqQueue = new PriorityQueue<>(Message.oldestMessageFirst());

    private final AtomicLong numberOfCompletedConnections = new AtomicLong();
    private final AtomicLong numberOfErrorsEncountered = new AtomicLong();

    private final ExecutorService service;
    private final StatsJSONifier jsoNifier;
    private final Supplier<Long> timeSource;

    StatsCollector(PrintStream printWriter, ExecutorService service, Supplier<Long> timeSource) {
        this.service = service;
        this.jsoNifier = new StatsJSONifier(printWriter);
        this.timeSource = timeSource;
    }

    public StatsCollector() {
        this(System.out,
                new ThreadPoolExecutor(1, 1, 1, TimeUnit.MINUTES, new ArrayBlockingQueue<>(1024)),
                System::currentTimeMillis);
    }

    public void scheduleStatsDump() {
        accept(PRINT_MARKER);
    }

    @Override
    public void accept(final Message message) {
        service.execute(() -> {
            if (message == PRINT_MARKER) {
                jsoNifier.jsonify(this, timeSource.get());
            } else if (message != null){
                switch (message.getMessageType()) {
                    case ACK:
                        responseQueue.add(message);
                        ackTotal++;
                        break;
                    case NAK:
                        responseQueue.add(message);
                        nakTotal++;
                        break;
                    case REQ:
                        reqQueue.add(message);
                        reqTotal++;
                        break;
                }
                long startTime = timeSource.get();
                StatsHelper.trimListToWindowSize(MAX_MESSAGE_AGE, startTime, responseQueue);
                StatsHelper.trimListToWindowSize(MAX_MESSAGE_AGE, startTime, reqQueue);
            }
        });
    }

    public long getTotalMessages() {
        return nakTotal + ackTotal + reqTotal;
    }

    public long getRequestTotal() {
        return reqTotal;
    }

    public long getAckTotal() {
        return ackTotal;
    }

    public long getNakTotal() {
        return nakTotal;
    }

    public double getRequestRateWithinWindow(long windowSize, TimeUnit windowUnit, long startingMillis) {
        return StatsHelper.getMessagesPerSecondWithinWindow(windowUnit.convert(windowSize, TimeUnit.SECONDS), startingMillis, reqQueue);
    }

    public double getResponseRateWithinWindow(long windowSize, TimeUnit windowUnit, long startingMillis) {
        return StatsHelper.getMessagesPerSecondWithinWindow(windowUnit.convert(windowSize, TimeUnit.SECONDS), startingMillis, responseQueue);
    }


    @Override
    public void connectionComplete() {
        numberOfCompletedConnections.incrementAndGet();
    }

    @Override
    public void onError(Throwable t) {
        t.printStackTrace();
        numberOfErrorsEncountered.incrementAndGet();
    }
}
