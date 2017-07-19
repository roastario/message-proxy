package com.stefano.weaveworks.pipe;

import static java.util.Arrays.asList;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.function.Consumer;

import com.stefano.weaveworks.Message;
import com.stefano.weaveworks.pipe.listeners.ConnectionEndedListener;
import com.stefano.weaveworks.stats.StatsCollector;

/**
 * Created by stefano
 */
public class ListenableMessagePipe {

    private final AsynchronousSocketChannel clientConnection;
    private final String forwardAddress;
    private final int forwardPort;
    private final StatsCollector statsCollector;
    private final String delimiter;

    public ListenableMessagePipe(AsynchronousSocketChannel clientConnection,
                                 String forwardAddress, int forwardPort,
                                 StatsCollector statsCollector,
                                 String delimiter) {

        this.clientConnection = clientConnection;
        this.forwardAddress = forwardAddress;
        this.forwardPort = forwardPort;
        this.statsCollector = statsCollector;
        this.delimiter = delimiter;
    }

    public void startPipe() {
        AsyncUtils.asyncConnect(forwardAddress, forwardPort).thenAccept(serverConnection -> {
            AsyncMessageWriter clientConsumer = new AsyncMessageWriter(serverConnection, delimiter, new WriteErrorHandler(statsCollector, serverConnection, clientConnection));
            AsyncMessageWriter serverConsumer = new AsyncMessageWriter(clientConnection, delimiter, new WriteErrorHandler(statsCollector, clientConnection, serverConnection));
            ConnectionEndedListener clientDisconnectListner = new DisconnectionHandler(clientConnection, serverConsumer);
            ConnectionEndedListener serverDisconnectListner = new DisconnectionHandler(serverConnection, clientConsumer);
            AsyncInputReader<Message> clientInputReader = new AsyncInputReader<>(clientConnection, delimiter, Message::fromString, asList(statsCollector, clientConsumer), statsCollector, clientDisconnectListner);
            AsyncInputReader<Message> serverInputReader = new AsyncInputReader<>(serverConnection, delimiter, Message::fromString, asList(statsCollector, serverConsumer), statsCollector, serverDisconnectListner);
            clientInputReader.enterReadLoop();
            serverInputReader.enterReadLoop();
        });
    }


    private static class WriteErrorHandler implements Consumer<Throwable> {

        private final StatsCollector statsCollector;
        private final AsynchronousSocketChannel channelCausingError;
        private final AsynchronousSocketChannel channelToCloseDueToError;

        private WriteErrorHandler(StatsCollector statsCollector,
                                  AsynchronousSocketChannel channelCausingError,
                                  AsynchronousSocketChannel channelToCloseDueToError) {
            this.statsCollector = statsCollector;
            this.channelCausingError = channelCausingError;
            this.channelToCloseDueToError = channelToCloseDueToError;
        }

        @Override
        public void accept(Throwable throwable) {
            statsCollector.onError(throwable);
            try {
                System.err.println(channelToCloseDueToError.getRemoteAddress() + " has been disconnected due to proxy error");
                channelToCloseDueToError.close();
            } catch (IOException e) {
            }
        }
    }

    private static class DisconnectionHandler implements ConnectionEndedListener {

        private final AsynchronousSocketChannel channelDisconnected;
        private final AsyncMessageWriter channelToDisconnect;

        private DisconnectionHandler(AsynchronousSocketChannel channelDisconnected, AsyncMessageWriter channelToDisconnect) {
            this.channelDisconnected = channelDisconnected;
            this.channelToDisconnect = channelToDisconnect;
        }

        @Override
        public void connectionComplete() {
            try {
                System.err.println(channelDisconnected.getRemoteAddress().toString() + " has disconnected ");
                channelToDisconnect.closeConnection();
            } catch (IOException e) {
            }
        }
    }
}
