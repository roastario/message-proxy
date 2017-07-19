package com.stefano.sewworks.pipe;

import static com.stefano.sewworks.pipe.AsyncUtils.asyncRead;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.stefano.sewworks.pipe.listeners.ConnectionEndedListener;
import com.stefano.sewworks.pipe.listeners.ConnectionErrorListener;

/**
 * Author stefanofranz
 */
public class AsyncInputReader<T> {

    private final AsynchronousSocketChannel toReadFrom;
    private final String messageDelimiter;
    private final Function<String, T> deserialiser;
    private final List<Consumer<T>> messageListeners;
    private final ConnectionErrorListener errorListener;
    private final ConnectionEndedListener endedListener;

    public AsyncInputReader(AsynchronousSocketChannel toReadFrom,
                            String messageDelimiter,
                            Function<String, T> deserialiser,
                            List<Consumer<T>> messageListeners,
                            ConnectionErrorListener errorListener,
                            ConnectionEndedListener endedListener) {
        this.toReadFrom = toReadFrom;
        this.messageDelimiter = messageDelimiter;
        this.deserialiser = deserialiser;
        this.messageListeners = messageListeners;
        this.errorListener = errorListener;
        this.endedListener = endedListener;
    }

    private final ByteBuffer readBuffer = ByteBuffer.allocate(1024);
    private final StringBuffer messageData = new StringBuffer();

    private void readLoop() {
        asyncRead(toReadFrom, readBuffer, (bytesRead, buffer) -> {
            if (bytesRead != -1) {
                try {
                    String newData = new String(readBuffer.array(), 0, bytesRead);
                    messageData.append(newData);
                    consumeMessageData();
                } catch (Throwable t) {
                    errorListener.onError(t);
                }

            }
        }, errorListener::onError, endedListener);
    }

    public void enterReadLoop() {
        readLoop();
    }

    private void consumeMessageData() {
        int indexOfDelimiter;
        while ((indexOfDelimiter = messageData.indexOf(messageDelimiter)) != -1) {
            String toConvert = messageData.substring(0, indexOfDelimiter);
            convertMessageAndInvokeListeners(deserialiser, messageListeners, errorListener, toConvert);
            resetMessageBuffer(messageData, indexOfDelimiter);
        }
    }

    private void resetMessageBuffer(StringBuffer messageData, int indexOfDelimiter) {
        String leftOvers = messageData.substring(indexOfDelimiter + 1);
        messageData.replace(0, leftOvers.length(), leftOvers);
        messageData.setLength(leftOvers.length());
    }

    private void convertMessageAndInvokeListeners(Function<String, T> converter,
                                                  List<Consumer<T>> listeners,
                                                  ConnectionErrorListener connectionErrorListener,
                                                  String toConvert) {

        listeners.forEach(listener -> {
            try {
                T converted = converter.apply(toConvert);
                listener.accept(converted);
            } catch (Throwable t) {
                connectionErrorListener.onError(t);
            }
        });
    }


}
