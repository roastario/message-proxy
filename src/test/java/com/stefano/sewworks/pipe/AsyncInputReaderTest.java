package com.stefano.sewworks.pipe;

import static java.util.Collections.singletonList;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;

/**
 * Author stefanofranz
 */
public class AsyncInputReaderTest {


    @Test(timeout = 2000L)
    public void shouldReadFromInputAndTriggerEvents() throws Exception {

        final String delimiter = "\n";


        ServerSocket toWriteTo = new ServerSocket(0);
        List<NetworkMessage> sentMessages = Collections.synchronizedList(new ArrayList<>());
        CompletableFuture<Void> writingFuture = CompletableFuture.runAsync(() -> {
            try {
                Random rnd = new Random();
                Socket accepted = toWriteTo.accept();
                PrintStream writer = new PrintStream(accepted.getOutputStream());
                for (int i = 0; i < 1000; i++) {
                    NetworkMessage toSend = new NetworkMessage(i, new BigInteger(128, rnd).toString(26));
                    writer.println((toSend.toNetworkRepresentation()));
                    sentMessages.add(toSend);
                    writer.flush();
                }
                accepted.close();
            } catch (IOException e) {
            }
        });

        List<NetworkMessage> recievedMessages = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch readCompletionSync = new CountDownLatch(1);
        AsyncUtils.asyncConnect("localhost", toWriteTo.getLocalPort()).thenAccept(connectedSocket -> {
            AsyncInputReader<NetworkMessage> reader = new AsyncInputReader<>(
                    connectedSocket,
                    delimiter,
                    NetworkMessage::fromNetworkRepresentation,
                    singletonList(recievedMessages::add),
                    Throwable::printStackTrace,
                    readCompletionSync::countDown
            );
            reader.enterReadLoop();
        });

        writingFuture.get();
        readCompletionSync.await();
        Assert.assertThat(recievedMessages, is(equalTo(sentMessages)));
    }


    private static class NetworkMessage {
        private final int id;
        private final String data;

        private NetworkMessage(int id, String data) {
            this.id = id;
            this.data = data;
        }

        private String toNetworkRepresentation() {
            return id + " " + data;
        }

        private static NetworkMessage fromNetworkRepresentation(String networkRep) {
            String[] tokens = networkRep.split(" ");
            return new NetworkMessage(Integer.valueOf(tokens[0]), tokens[1]);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NetworkMessage that = (NetworkMessage) o;
            return id == that.id &&
                    Objects.equals(data, that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, data);
        }

        @Override
        public String toString() {
            return "NetworkMessage{" +
                    "id=" + id +
                    ", data='" + data + '\'' +
                    '}';
        }
    }
}