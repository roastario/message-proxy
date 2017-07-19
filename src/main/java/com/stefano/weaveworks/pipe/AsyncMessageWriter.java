package com.stefano.weaveworks.pipe;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.function.Consumer;

import com.stefano.weaveworks.Message;

/**
 * Author stefanofranz
 */
public class AsyncMessageWriter implements Consumer<Message> {
    public static final Charset CHARSET = Charset.forName("UTF-8");
    private final AsynchronousSocketChannel toWriteTo;
    private final String delimiter;
    private final Consumer<Throwable> onWriteError;

    public AsyncMessageWriter(AsynchronousSocketChannel toWriteTo, String delimiter, Consumer<Throwable> errorConsumer) {
        this.toWriteTo = toWriteTo;
        this.delimiter = delimiter;
        this.onWriteError = errorConsumer;
    }

    @Override
    public void accept(Message message) {
        String toSend = message.getBackingData() + delimiter;
        ByteBuffer toWrite = ByteBuffer.wrap(toSend.getBytes(CHARSET));
        AsyncUtils.asyncWrite(toWriteTo, toWrite, onWriteError);
    }

    public void closeConnection() {
        try {
            toWriteTo.close();
        } catch (IOException e) {
        }
    }
}
