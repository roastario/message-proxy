package com.stefano.sewworks.pipe;

import static java.net.InetAddress.getLocalHost;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Assert;
import org.junit.Test;

import com.stefano.sewworks.Message;

/**
 * Author stefanofranz
 */
public class AsyncMessageWriterTest {


    @Test
    public void shouldWriteReceivedMessagesToSocket() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0);
        List<String> messagesRecieved = new ArrayList<>();
        CompletableFuture<Void> readingFuture = CompletableFuture.runAsync(() -> {
            try {
                Socket toReadFrom = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(toReadFrom.getInputStream()));
                String lineRead;
                while ((lineRead = bufferedReader.readLine()) != null) {
                    messagesRecieved.add(lineRead);
                }
            } catch (IOException e) {
            }
        });

        List<String> messagesSent = new ArrayList<>();
        AsyncUtils.asyncConnect(getLocalHost().getHostAddress(), serverSocket.getLocalPort()).thenAccept(socket -> {
            Random rnd = new Random();
            AsyncMessageWriter consumer = new AsyncMessageWriter(socket, "\n", Throwable::printStackTrace);
            for (int i = 0; i < 100; i++) {
                String toSend = "REQ " + i + " " + new BigInteger(128, rnd).toString();
                messagesSent.add(toSend);
                Message message = Message.fromString(toSend);
                consumer.accept(message);
            }
            consumer.closeConnection();
        });
        readingFuture.get();
        Assert.assertThat(messagesSent, is(equalTo(messagesRecieved)));
    }

    @Test(expected = IOException.class, timeout = 5000L)
    public void shouldInvokeErrorListenerOnUnexpectedDisconnect() throws Throwable {
        ServerSocket serverSocket = new ServerSocket(0);
        CompletableFuture<Void> readingFuture = CompletableFuture.runAsync(() -> {
            try {
                Socket toReadFrom = serverSocket.accept();
                System.out.println("CLOSING CONNECTION EARLY");
                toReadFrom.close();
            } catch (IOException e) {
            }
        }, Executors.newSingleThreadExecutor());

        CountDownLatch exceptionWaiter = new CountDownLatch(1);
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        AsyncUtils.asyncConnect(getLocalHost().getHostAddress(), serverSocket.getLocalPort()).thenAccept(socket -> {
            Random rnd = new Random();
            AtomicBoolean shouldSend = new AtomicBoolean(true);
            AsyncMessageWriter consumer = new AsyncMessageWriter(socket, "\n", t -> {
                if(thrown.compareAndSet(null, t)){
                    System.out.println("EXCEPTION RECEIVED");
                    shouldSend.set(false);
                    exceptionWaiter.countDown();
                }
            });
            for (int i = 0; i < 100; i++) {
                if (shouldSend.get()) {
                    String toSend = "REQ " + i + " " + new BigInteger(128, rnd).toString();
                    Message message = Message.fromString(toSend);
                    consumer.accept(message);
                }
            }
            consumer.closeConnection();
        });

        readingFuture.get();
        exceptionWaiter.await();
        System.out.println("continuing");
        throw thrown.get();

    }
}