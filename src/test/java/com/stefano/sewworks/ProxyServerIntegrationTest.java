package com.stefano.sewworks;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Test;

import com.stefano.sewworks.stats.StatsCollector;

/**
 * Author stefanofranz
 */
public class ProxyServerIntegrationTest {


    public static final int LISTEN_PORT = 8023;
    public static final int NUMBER_OF_MESSAGES = 100;

    @Test
    public void shouldCollectStatsWhilstProxyingMessages() throws Exception {


        ServerSocket serverSocket = new ServerSocket(0);
        StatsCollector statsCollector = new StatsCollector();
        ProxyServer proxyServer = new ProxyServer(
                InetAddress.getLocalHost().getHostAddress(),
                LISTEN_PORT,
                InetAddress.getLocalHost().getHostAddress(),
                serverSocket.getLocalPort(),
                statsCollector);

        Thread serverThread = new Thread(() -> {
            try {
                proxyServer.startAndWait();
            } catch (ExecutionException | InterruptedException | IOException e) {
            }
        });
        serverThread.start();


        CompletableFuture<List<String>> serverEchoFuture = CompletableFuture.supplyAsync(() -> {
            List<String> serverMessagesRecieved = new ArrayList<>();
            try {
                Socket toReadFrom = serverSocket.accept();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(toReadFrom.getInputStream()));
                PrintStream writer = new PrintStream(toReadFrom.getOutputStream());
                for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
                    String lineRead = bufferedReader.readLine();
                    serverMessagesRecieved.add(lineRead);
                    writer.println(lineRead);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return serverMessagesRecieved;
        });

        Socket clientSocket = null;
        boolean connected = false;
        while (!connected) {
            try {
                clientSocket = new Socket(InetAddress.getLocalHost().getHostAddress(), LISTEN_PORT);
                connected = true;
            } catch (Exception e) {
            }
        }

        Socket connectedClientSocket = clientSocket;
        CompletableFuture<List<String>> clientWritingFuture = CompletableFuture.supplyAsync(() -> {
            List<String> clientMessagesSent = new ArrayList<>();
            try {
                PrintStream printStream = new PrintStream(connectedClientSocket.getOutputStream());
                for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
                    String toSend = "REQ " + i + " " + BigInteger.valueOf((long) (i * i * i * i)).toString(32);
                    printStream.println(toSend);
                    clientMessagesSent.add(toSend);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return clientMessagesSent;
        });

        CompletableFuture<List<String>> clientReadingFuture = CompletableFuture.supplyAsync(() -> {
            List<String> clientMessagesRecieved = new ArrayList<>();
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connectedClientSocket.getInputStream()));
                for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
                    String lineRead = reader.readLine();
                    clientMessagesRecieved.add(lineRead);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return clientMessagesRecieved;
        });


        List<String> clientRecieved = clientReadingFuture.get();
        List<String> clientSent = clientWritingFuture.get();
        List<String> serverRecieved = serverEchoFuture.get();

        Assert.assertThat(clientRecieved, is(equalTo(serverRecieved)));
        Assert.assertThat(clientSent, is(equalTo(serverRecieved)));
    }
}