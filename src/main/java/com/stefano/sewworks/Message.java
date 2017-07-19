package com.stefano.sewworks;

import java.util.Comparator;
import java.util.Objects;

/**
 * Author stefanofranz
 */
public class Message {
    private final String backingData;
    private final MessageType messageType;
    private final int sequenceNumber;
    private final String data;
    private final long timeStamp;

    public Message(String backingData, MessageType messageType, int sequenceNumber, String data, long timeStamp) {
        this.backingData = backingData;
        this.messageType = messageType;
        this.sequenceNumber = sequenceNumber;
        this.data = data;
        this.timeStamp = timeStamp;
    }

    public Message(String backingData, MessageType messageType, int sequenceNumber, String data) {
        this(backingData, messageType, sequenceNumber, data, System.currentTimeMillis());
    }

    public Message() {
        this.backingData = null;
        this.messageType = null;
        this.sequenceNumber = 0;
        this.data = null;
        this.timeStamp = 0;
    }


    public static Message fromString(String backingData) {
        String[] tokenised = backingData.split("\\s");
        MessageType type = MessageType.value(tokenised[0]);
        switch (tokenised.length) {
            case 2:
                return new Message(backingData, type, Integer.parseInt(tokenised[1]), null);
            case 3:
                return new Message(backingData, type, Integer.parseInt(tokenised[1]), backingData);
            default:
                throw new IllegalArgumentException("MalFormed message: " + backingData);
        }
    }

    public String getBackingData() {
        return backingData;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public String getData() {
        return data;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public enum MessageType {
        ACK, NAK, REQ, UNKNOWN;

        static MessageType value(String string) {
            try {
                return MessageType.valueOf(string);
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "backingData='" + backingData + '\'' +
                ", messageType=" + messageType +
                ", sequenceNumber=" + sequenceNumber +
                ", data='" + data + '\'' +
                ", timeStamp=" + timeStamp +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return getSequenceNumber() == message.getSequenceNumber() &&
                getTimeStamp() == message.getTimeStamp() &&
                getMessageType() == message.getMessageType() &&
                Objects.equals(getData(), message.getData());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMessageType(), getSequenceNumber(), getData(), getTimeStamp());
    }

    public static Comparator<Message> oldestMessageFirst() {
        return Comparator.comparingLong(Message::getTimeStamp);
    }
}
