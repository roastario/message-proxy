package com.stefano.weaveworks.pipe;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import com.stefano.weaveworks.pipe.listeners.ConnectionEndedListener;
import com.stefano.weaveworks.pipe.listeners.ConnectionErrorListener;

/**
 * Author stefanofranz
 */
public class InputReader<T> implements Runnable {

    private final InputStream toReadFrom;
    private final String messageDelimiter;
    private final Function<String, T> deserialiser;
    private final List<Consumer<T>> messageListeners;
    private final ConnectionErrorListener errorListener;
    private final ConnectionEndedListener  endedListener;

    public InputReader(InputStream toReadFrom,
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

    private void readFromSocket() {
        byte[] buffer = new byte[1024];
        StringBuilder currentMessage = new StringBuilder();
        int bytesRead = 0;
        try {
            while ((bytesRead = toReadFrom.read(buffer)) != -1) {
                currentMessage.append(new String(buffer, 0, bytesRead, Charset.forName("UTF-8")));
                currentMessage = consumeData(messageDelimiter, deserialiser, messageListeners, errorListener, currentMessage);
            }
        } catch (Throwable t) {
            errorListener.onError(t);
        }
        endedListener.connectionComplete();
    }

    private StringBuilder consumeData(String delimiter,
                                             Function<String, T> converter,
                                             List<Consumer<T>> listeners,
                                             ConnectionErrorListener connectionErrorListener,
                                             StringBuilder currentMessage) {
        int indexOfDelimiter = currentMessage.indexOf(delimiter);
        if (indexOfDelimiter != -1) {
            String toConvert = currentMessage.substring(0, indexOfDelimiter);
            convertMessageAndInvokeListeners(converter, listeners, connectionErrorListener, toConvert);
            currentMessage = resetBuffer(currentMessage, indexOfDelimiter);
        }
        return currentMessage;
    }

    private void convertMessageAndInvokeListeners(Function<String, T> converter,
                                                         List<Consumer<T>> listeners,
                                                         ConnectionErrorListener connectionErrorListener,
                                                         String toConvert) {
        T converted = converter.apply(toConvert);
        listeners.forEach(listener -> {
            try {
                listener.accept(converted);
            } catch (Throwable t) {
                connectionErrorListener.onError(t);
            }
        });
    }

    private static StringBuilder resetBuffer(StringBuilder currentMessage, int indexOfDelimiter) {
        currentMessage = new StringBuilder(currentMessage.subSequence(indexOfDelimiter + 1, currentMessage.length()));
        return currentMessage;
    }

    @Override
    public void run() {
        readFromSocket();
    }
}
