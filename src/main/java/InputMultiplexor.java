import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.singletonList;

/**
 * Created by stefano
 */
public class InputMultiplexor {
    private final Socket serverConnection;


    private final Thread serverReader;
    private final Thread clientReader;


    public InputMultiplexor(Socket clientConnection, String forwardAddress, int forwardPort) throws IOException {


        serverConnection = new Socket(forwardAddress, forwardPort);

        Runnable clientRunnable = () -> {
            MessageConsumer<Message> messageConsumer = new MessageConsumer<Message>() {
                @Override
                public void consume(Message o) throws Exception {
                    String dataToWrite = o.backingData + "\n";
                    System.out.println("toServer: " + o.backingData);
                    serverConnection.getOutputStream().write(dataToWrite.getBytes());
                    serverConnection.getOutputStream().flush();
                }
            };
            readFromSocket(clientConnection, "\n", Message::fromString, singletonList(messageConsumer),
                    () -> {}, t -> t.printStackTrace());
        };

        Runnable serverRunnable = () -> {
            readFromSocket(serverConnection, "\n", (s) -> s, singletonList(new MessageConsumer<Object>() {
                @Override
                public void consume(Object o) throws Exception {
                    System.out.println("toClient: " + o.toString());
                    clientConnection.getOutputStream().write((o.toString() + "\n").getBytes());
                    clientConnection.getOutputStream().flush();
                }
            }), () -> {
            }, t-> t.printStackTrace());
        };

        clientReader = new Thread(clientRunnable);
        serverReader = new Thread(serverRunnable);

    }

    public void startPipe() {
        clientReader.start();
        serverReader.start();
    }


    static <T> void readFromSocket(Socket toReadFrom,
                                   String delimiter,
                                   Function<String, T> converter,
                                   List<MessageConsumer<T>> listeners,
                                   ConnectionEndedListener disconnectListener,
                                   ConnectionErrorListener errorListener) {
        byte[] buffer = new byte[1024];
        StringBuilder currentMessage = new StringBuilder();
        int bytesRead = 0;
        try {
            while ((bytesRead = toReadFrom.getInputStream().read(buffer)) != -1) {
                currentMessage.append(new String(buffer, 0, bytesRead));
                currentMessage = consumeData(delimiter, converter, listeners, errorListener, currentMessage);
            }
        } catch (Throwable t) {
            errorListener.onError(t);
        }
        disconnectListener.connectionComplete();
    }

    private static <T> StringBuilder consumeData(String delimiter, Function<String, T> converter, List<MessageConsumer<T>> listeners, ConnectionErrorListener connectionErrorListener, StringBuilder currentMessage) {
        int indexOfDelimiter = currentMessage.indexOf(delimiter);
        if (indexOfDelimiter != -1) {
            String toConvert = currentMessage.substring(0, indexOfDelimiter);
            convertMessageAndInvokeListeners(converter, listeners, connectionErrorListener, toConvert);
            currentMessage = resetBuffer(currentMessage, indexOfDelimiter);
        }
        return currentMessage;
    }

    private static <T> void convertMessageAndInvokeListeners(Function<String, T> converter, List<MessageConsumer<T>> listeners, ConnectionErrorListener connectionErrorListener, String toConvert) {
        T converted = converter.apply(toConvert);
        listeners.forEach(listener -> {
            try {
                listener.consume(converted);
            } catch (Throwable t) {
                connectionErrorListener.onError(t);
            }
        });
    }

    private static StringBuilder resetBuffer(StringBuilder currentMessage, int indexOfDelimiter) {
        currentMessage = new StringBuilder(currentMessage.subSequence(indexOfDelimiter+1, currentMessage.length()));
        return currentMessage;
    }

    private static interface MessageConsumer<T> {
        void consume(T t) throws Exception;
    }

    private static interface ConnectionEndedListener {
        void connectionComplete();
    }

    private enum MessageType{
        ACK, NAK, REQ;

        static MessageType value(String string){
            try{
                return MessageType.valueOf(string);
            }catch (Exception e){
                throw new IllegalArgumentException("Unknown messageType: " + string);
            }
        }
    }

    private static class Message {
        private final CharSequence backingData;
        private final MessageType messageType;
        private final int sequenceNumber;
        private final String data;

        private Message(CharSequence backingData, MessageType messageType, int sequenceNumber, String data) {
            this.backingData = backingData;
            this.messageType = messageType;
            this.sequenceNumber = sequenceNumber;
            this.data = data;
        }


        private static Message fromString(String backingData) {
            String[] tokenised = backingData.split("\\s");
            MessageType type = MessageType.value(tokenised[0]);
            switch (tokenised.length){
                case 2:
                    return new Message(backingData, type, Integer.valueOf(tokenised[1]), null);
                case 3:
                    return new Message(backingData, type, Integer.valueOf(tokenised[1]), backingData);
                default:
                    throw new IllegalArgumentException("MalFormed message: " + backingData);
            }
        }
    }

    private interface ConnectionErrorListener {

        void onError(Throwable t);

    }
}
