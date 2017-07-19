package com.stefano.weaveworks.pipe;

import static org.hamcrest.core.Is.is;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;

/**
 * Author stefanofranz
 */
public class AsyncUtilsTest {


    @Test(timeout = 2000L)
    public void shouldReadFromSocketAndTriggerListener() throws Exception {
        ServerSocket socket = new ServerSocket(0);
        StringBuffer writtenData = new StringBuffer();
        CompletableFuture<Void> writingFuture = CompletableFuture.runAsync(() -> {
            try {
                Socket accepted = socket.accept();
                PrintStream writer = new PrintStream(accepted.getOutputStream());
                for (int i = 0; i < 1000; i++) {
                    String toSend = "REQ " + i + "Hey";
                    writer.print(toSend);
                    writtenData.append(toSend);
                    writer.flush();
                }
                writer.close();
                accepted.close();
            } catch (IOException e) {
            }
        });

        CountDownLatch waiter = new CountDownLatch(1);
        StringBuffer readData = new StringBuffer();
        AsyncUtils.asyncConnect("localhost", socket.getLocalPort()).thenAccept(connectedSocket -> {
            ByteBuffer bb = ByteBuffer.allocate(1024);
            AsyncUtils.asyncRead(connectedSocket, bb, (bytesRead, readBuffer)->{
                String newData = new String(readBuffer.array(), 0, bytesRead);
                readData.append(newData);
            }, (t)->{}, waiter::countDown);
        });
        waiter.await();
        writingFuture.get();
        Assert.assertThat(writtenData.toString(), is(readData.toString()));
    }

    @Test(timeout = 2000L)
    public void shouldWriteToSocketAndTriggerCompletionListener() throws Exception {

        ServerSocket server = new ServerSocket(0);
        StringBuffer readData = new StringBuffer();
        CompletableFuture<Void> readingFuture = CompletableFuture.runAsync(() -> {
            try {
                Socket toReadFrom = server.accept();
                byte[] buffer = new byte[1024];
                int bytesRead = 0;
                while ((bytesRead = toReadFrom.getInputStream().read(buffer)) != -1) {
                    readData.append(new java.lang.String(buffer, 0, bytesRead));
                }
                toReadFrom.close();
            } catch (IOException e) {
            }
        });

        StringBuffer writtenData = new StringBuffer();
        AsyncUtils.asyncConnect("localhost", server.getLocalPort()).thenAccept(connectedSocket -> {
            for (int i = 0; i < 1000; i++) {
                String toWrite = "NAK " + i;
                writtenData.append(toWrite);
            }
            AsyncUtils.asyncWrite(connectedSocket, ByteBuffer.wrap(writtenData.toString().getBytes()), Throwable::printStackTrace, () -> {
                try {
                    connectedSocket.close();
                } catch (IOException e) {
                }
            });
        });

        readingFuture.get();
        Assert.assertThat(writtenData.toString(), is(readData.toString()));
    }
}