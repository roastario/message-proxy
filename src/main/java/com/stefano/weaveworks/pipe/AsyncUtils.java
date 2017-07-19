package com.stefano.weaveworks.pipe;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.stefano.weaveworks.pipe.listeners.ConnectionEndedListener;

/**
 * Author stefanofranz
 */
public class AsyncUtils {

    public static void asyncRead(AsynchronousSocketChannel toReadFrom,
                                 ByteBuffer buffer,
                                 BiConsumer<Integer, ByteBuffer> onRead,
                                 Consumer<Throwable> onError,
                                 ConnectionEndedListener onEnd) {
        toReadFrom.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (result != -1) {
                    onRead.accept(result, attachment);
                    attachment.clear();
                    toReadFrom.read(attachment, attachment, this);
                } else {
                    onEnd.connectionComplete();
                    attachment.clear();
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                onError.accept(exc);
            }
        });
    }

    public static CompletableFuture<AsynchronousSocketChannel> asyncConnect(String address, int port) {
        CompletableFuture<AsynchronousSocketChannel> toReturn = new CompletableFuture<>();
        try {
            AsynchronousSocketChannel serverConnection = AsynchronousSocketChannel.open();
            serverConnection.connect(new InetSocketAddress(address, port), null, new CompletionHandler<Void, Object>() {
                @Override
                public void completed(Void result, Object attachment) {
                    toReturn.complete(serverConnection);
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    toReturn.completeExceptionally(exc);
                }
            });
        } catch (IOException e) {
            toReturn.completeExceptionally(e);
        }
        return toReturn;
    }


    public static void asyncWrite(AsynchronousSocketChannel toWriteTo, ByteBuffer buffer, Consumer<Throwable> onError) {
        asyncWrite(toWriteTo, buffer, onError, () -> {
        });
    }

    public static void asyncWrite(AsynchronousSocketChannel toWriteTo,
                                  ByteBuffer buffer,
                                  Consumer<Throwable> onError,
                                  Runnable onComplete) {
        toWriteTo.write(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
                if (attachment.hasRemaining()) {
                    toWriteTo.write(attachment, attachment, this);
                } else {
                    onComplete.run();
                }
            }
            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                onError.accept(exc);
            }
        });
    }


}
