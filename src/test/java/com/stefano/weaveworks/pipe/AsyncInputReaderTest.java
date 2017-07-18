package com.stefano.weaveworks.pipe;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.junit.Test;

import com.stefano.weaveworks.pipe.listeners.ConnectionEndedListener;

/**
 * Author stefanofranz
 */
public class AsyncInputReaderTest {


    @Test
    public void shouldReadFromSocketAndTriggerListener() throws Exception {

        AsynchronousServerSocketChannel socket = AsynchronousServerSocketChannel.open();
        AsynchronousServerSocketChannel bound = socket.bind(new InetSocketAddress(50023));

        Thread.sleep(1000);
        AsyncUtils.asyncWrite(bound., ByteBuffer.wrap("TEST".getBytes()), new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                System.out.println(throwable);
            }
        });

        Thread.sleep(1000);

        AsyncUtils.asyncRead(bound, ByteBuffer.allocate(1024), new BiConsumer<Integer, ByteBuffer>() {
            @Override
            public void accept(Integer integer, ByteBuffer byteBuffer) {
                System.out.println(new String(byteBuffer.array()));
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {

            }
        }, new ConnectionEndedListener() {
            @Override
            public void connectionComplete() {

            }
        });

        Thread.sleep(10000);
    }
}